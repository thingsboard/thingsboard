// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { firstValueFrom, forkJoin, from, Observable, of } from 'rxjs';
import { catchError, filter, map, switchMap, take, timeout } from 'rxjs/operators';
import { WidgetService } from '@core/http/widget.service';
import { AiResourceService } from '@core/http/ai-resource.service';
import { migrateWidgetTypeToDynamicForms, WidgetTypeDetails } from '@shared/models/widget.models';
import { ImageService } from '@core/http/image.service';
import { parseScadaSymbolMetadataFromContent, ScadaSymbolMetadata } from '@home/components/widget/lib/scada/scada-symbol.models';
import { detailsToWidgetInfo, WidgetInfo } from '@home/models/widget-component.models';

const SETTLE_MS = 2500;
const RUN_TIMEOUT_MS = 30000;
const MAX_REASON_LENGTH = 1900;
const MAX_LINE_LENGTH = 300;
const PREVIEW_WIDTH = 1280;
const PREVIEW_HEIGHT = 800;
const STYLE_CHUNK = 8;
const ICON_FONT = /material (icons|symbols)/i;

interface WidgetCheckReport {
  initialised: boolean;
  errors: string[];
  console: string[];
  noData?: boolean;
  width?: number;
  height?: number;
  screenshot?: string;
  rendered?: boolean;
  activeValues?: Record<string, unknown>;
}

type CapturedReport = WidgetCheckReport & {canvas?: HTMLCanvasElement};

@Injectable({
  providedIn: 'root'
})
export class AiWidgetCheckService {

  constructor(private widgetService: WidgetService,
              private imageService: ImageService,
              private aiResources: AiResourceService) {
  }

  checkScadaSymbol(link: string): Observable<string> {
    const url = link.replace(/^tb-image;/, '');
    return forkJoin({
      widgetType: this.widgetService.getWidgetType('system.scada_symbol', {ignoreLoading: true, ignoreErrors: true}),
      content: this.imageService.getImageString(url)
    }).pipe(
      switchMap(({widgetType, content}) => {
        const widget = detailsToWidgetInfo(migrateWidgetTypeToDynamicForms(widgetType as WidgetTypeDetails));
        const active = this.activeValues(parseScadaSymbolMetadataFromContent(content));
        return from(this.runSymbol(widget, link, {}).then(normal =>
          this.runSymbol(widget, link, active.behavior).then(activeRun => ({normal, activeRun, values: active.values}))));
      }),
      switchMap(({normal, activeRun, values}) => {
        const report: CapturedReport = {
          initialised: normal.initialised && activeRun.initialised,
          rendered: !!normal.rendered && !!activeRun.rendered,
          errors: [...new Set([...normal.errors, ...activeRun.errors])],
          console: [...new Set([...normal.console, ...activeRun.console])],
          activeValues: values,
          canvas: this.sideBySide(normal.canvas, activeRun.canvas)
        };
        return this.attachScreenshot(report);
      }),
      map(report => this.toReason(report)),
      timeout(2 * RUN_TIMEOUT_MS + 15000),
      catchError(error => of(this.toReason({
        initialised: false,
        errors: ['The symbol could not be loaded: ' + (error?.message || error?.statusText || error)],
        console: []
      })))
    );
  }

  private runSymbol(widget: WidgetInfo, link: string, behavior: Record<string, unknown>): Promise<CapturedReport> {
    const config = JSON.parse(widget.defaultConfig || '{}');
    config.showTitle = false;
    config.settings = {...(config.settings || {}), scadaSymbolUrl: link, simulated: true,
      scadaSymbolObjectSettings: {behavior, properties: {}}};
    return this.run({...widget, defaultConfig: JSON.stringify(config)});
  }

  private activeValues(metadata: ScadaSymbolMetadata): {behavior: Record<string, unknown>; values: Record<string, unknown>} {
    const behavior: Record<string, unknown> = {};
    const values: Record<string, unknown> = {};
    for (const item of metadata?.behavior || []) {
      if (item.type !== 'value') {
        continue;
      }
      const type = String(item.valueType);
      const value = type === 'BOOLEAN' ? !/animation/i.test(item.id)
        : type === 'DOUBLE' || type === 'INTEGER' ? (/capacity|max/i.test(item.id) ? 100 : 60)
        : type === 'STRING' ? 'Test' : null;
      behavior[item.id] = {...(item.defaultGetValueSettings || {}), action: 'DO_NOTHING', defaultValue: value};
      values[item.id] = value;
    }
    return {behavior, values};
  }

  private sideBySide(left?: HTMLCanvasElement, right?: HTMLCanvasElement): HTMLCanvasElement | undefined {
    if (!left || !right) {
      return left || right;
    }
    const gap = 16;
    const canvas = document.createElement('canvas');
    canvas.width = left.width + gap + right.width;
    canvas.height = Math.max(left.height, right.height);
    const context = canvas.getContext('2d');
    context.fillStyle = '#ffffff';
    context.fillRect(0, 0, canvas.width, canvas.height);
    context.drawImage(left, 0, 0);
    context.drawImage(right, left.width + gap, 0);
    return canvas;
  }

  check(widgetTypeId: string): Observable<string> {
    return this.widgetService.getWidgetTypeById(widgetTypeId, {ignoreLoading: true, ignoreErrors: true}).pipe(
      map(details => detailsToWidgetInfo(migrateWidgetTypeToDynamicForms(details))),
      switchMap(widget => from(this.run(widget))),
      switchMap(report => this.attachScreenshot(report)),
      map(report => this.toReason(report)),
      timeout(RUN_TIMEOUT_MS + 15000),
      catchError(error => of(this.toReason({
        initialised: false,
        errors: ['The widget could not be loaded: ' + (error?.message || error?.statusText || error)],
        console: []
      })))
    );
  }

  private run(widget: WidgetInfo): Promise<CapturedReport> {
    return new Promise(resolve => {
      const report: CapturedReport = {initialised: false, errors: [], console: []};
      const config = JSON.parse(widget.defaultConfig || '{}');
      config.title = config.title || widget.widgetName;
      const frame = document.createElement('iframe');
      frame.setAttribute('aria-hidden', 'true');
      frame.style.cssText = `position:fixed;left:-${PREVIEW_WIDTH + 200}px;top:0;width:${PREVIEW_WIDTH}px;height:${PREVIEW_HEIGHT}px;border:0;visibility:visible;`;
      frame.setAttribute('data-widget', JSON.stringify({...widget, defaultConfig: JSON.stringify(config)}));
      let settled = false;
      let settleTimer: ReturnType<typeof setTimeout>;
      const finish = async () => {
        if (settled) {
          return;
        }
        settled = true;
        clearTimeout(settleTimer);
        clearTimeout(overall);
        window.removeEventListener('message', onMessage);
        try {
          this.inspect(frame, report);
          report.canvas = await this.capture(frame);
        } catch (e) {
          report.console.push('Inspecting the preview failed: ' + (e as Error)?.message);
        }
        frame.remove();
        resolve(report);
      };
      const onMessage = (event: MessageEvent) => {
        if (event.source !== frame.contentWindow || typeof event.data !== 'string') {
          return;
        }
        let message: {type: string; data?: any};
        try {
          message = JSON.parse(event.data);
        } catch (e) {
          return;
        }
        if (message.type === 'widgetException') {
          const data = message.data || {};
          const line = data.lineNumber ? ` (line ${data.lineNumber})` : '';
          report.errors.push(this.clip(this.plain(data.message || data.name || 'Widget error') + line));
        } else if (message.type === 'widgetEditModeInited') {
          report.initialised = true;
          settleTimer = setTimeout(finish, SETTLE_MS);
        }
      };
      const overall = setTimeout(finish, RUN_TIMEOUT_MS);
      window.addEventListener('message', onMessage);
      frame.addEventListener('load', () => this.watchConsole(frame, report));
      frame.setAttribute('src', '/widget-editor');
      document.body.appendChild(frame);
    });
  }

  private watchConsole(frame: HTMLIFrameElement, report: WidgetCheckReport) {
    try {
      const win = frame.contentWindow as any;
      const original = win.console.error.bind(win.console);
      win.console.error = (...args: any[]) => {
        report.console.push(this.clip(args.map(arg => arg?.message || (typeof arg === 'string' ? arg : this.safeJson(arg))).join(' ')));
        original(...args);
      };
      win.addEventListener('error', (event: ErrorEvent) => report.console.push(this.clip(event.message)));
      win.addEventListener('unhandledrejection', (event: PromiseRejectionEvent) =>
        report.console.push(this.clip('Unhandled promise rejection: ' + (event.reason?.message || event.reason))));
    } catch (e) {
      report.console.push('The preview console could not be watched');
    }
  }

  private inspect(frame: HTMLIFrameElement, report: WidgetCheckReport) {
    const doc = frame.contentDocument;
    if (!doc) {
      return;
    }
    const errorPanel = doc.querySelector('.tb-widget-error');
    if (errorPanel?.textContent?.trim()) {
      const text = this.clip(errorPanel.textContent.trim().replace(/^Widget Error:\s*/, ''));
      if (!report.errors.includes(text)) {
        report.errors.push(text);
      }
    }
    report.noData = !!doc.querySelector('.tb-widget-no-data');
    report.rendered = !!doc.querySelector('tb-scada-symbol-widget svg');
    const widget = doc.querySelector('tb-widget') as HTMLElement;
    if (widget) {
      const rect = widget.getBoundingClientRect();
      report.width = Math.round(rect.width);
      report.height = Math.round(rect.height);
    }
  }

  private async capture(frame: HTMLIFrameElement): Promise<HTMLCanvasElement | undefined> {
    const doc = frame.contentDocument;
    const win = frame.contentWindow;
    const target = (doc?.querySelector('tb-widget-container') || doc?.querySelector('tb-widget')) as HTMLElement;
    if (!target || !win) {
      return undefined;
    }
    const rect = target.getBoundingClientRect();
    const width = Math.ceil(rect.width);
    const height = Math.ceil(rect.height);
    if (!width || !height) {
      return undefined;
    }
    const clone = target.cloneNode(true) as HTMLElement;
    const sources = [target, ...Array.from(target.querySelectorAll<Element>('*'))];
    const copies = [clone, ...Array.from(clone.querySelectorAll<Element>('*'))];
    for (let i = 0; i < sources.length; i++) {
      if (i % STYLE_CHUNK === STYLE_CHUNK - 1) {
        await new Promise(resolve => setTimeout(resolve));
      }
      this.copyElement(win, sources[i], copies[i]);
    }
    const markup = new XMLSerializer().serializeToString(clone);
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}">` +
      `<foreignObject x="0" y="0" width="100%" height="100%">${markup}</foreignObject></svg>`;
    const image = new Image();
    image.src = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg);
    await image.decode();
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    canvas.getContext('2d').drawImage(image, 0, 0);
    return canvas;
  }

  private copyElement(win: Window, source: Element, copy: Element) {
    const style = win.getComputedStyle(source);
    let text = '';
    for (const property of Array.from(style)) {
      if (!property.startsWith('--')) {
        text += `${property}:${style.getPropertyValue(property)};`;
      }
    }
    copy.setAttribute('style', text);
    const glyph = source.tagName !== 'CANVAS' && ICON_FONT.test(style.fontFamily) && source.childElementCount === 0;
    const image = source.tagName === 'CANVAS' ? this.canvasImage(source as HTMLCanvasElement)
      : glyph ? this.glyphImage(win, source, style) : undefined;
    if (glyph && !image) {
      copy.setAttribute('style', text + 'visibility:hidden;');
      return;
    }
    if (image) {
      const img = copy.ownerDocument.createElement('img');
      img.setAttribute('src', image);
      img.setAttribute('style', text);
      copy.replaceWith(img);
    }
  }

  private canvasImage(canvas: HTMLCanvasElement): string | undefined {
    try {
      return canvas.toDataURL();
    } catch (e) {
      return undefined;
    }
  }

  private glyphImage(win: Window, source: Element, style: CSSStyleDeclaration): string | undefined {
    const rect = source.getBoundingClientRect();
    const glyph = source.textContent?.trim();
    if (!glyph || !rect.width || !rect.height) {
      return undefined;
    }
    const canvas = win.document.createElement('canvas');
    canvas.width = Math.ceil(rect.width);
    canvas.height = Math.ceil(rect.height);
    const context = canvas.getContext('2d');
    context.font = `${style.fontStyle} ${style.fontWeight} ${style.fontSize} ${style.fontFamily}`;
    context.fillStyle = style.color;
    context.textAlign = 'center';
    context.textBaseline = 'middle';
    context.fillText(glyph, canvas.width / 2, canvas.height / 2);
    return this.canvasImage(canvas);
  }

  private attachScreenshot(report: CapturedReport): Observable<WidgetCheckReport> {
    const canvas = report.canvas;
    delete report.canvas;
    return from(this.upload(canvas, 'widget-check.png')).pipe(
      map(screenshot => {
        if (screenshot) {
          report.screenshot = screenshot;
        }
        return report;
      }),
      catchError(() => of(report))
    );
  }

  private upload(canvas: HTMLCanvasElement | undefined, name: string): Promise<string | undefined> {
    if (!canvas) {
      return Promise.resolve(undefined);
    }
    return new Promise<Blob>(resolve => canvas.toBlob(blob => resolve(blob), 'image/png')).then(blob => !blob ? undefined :
      firstValueFrom(this.aiResources.upload(new File([blob], name, {type: 'image/png'})).pipe(
        filter(event => event.type === 'done'),
        take(1),
        map(event => event.type === 'done' ? event.resource?.id : undefined),
        catchError(() => of(undefined))
      ), {defaultValue: undefined}));
  }

  private toReason(report: WidgetCheckReport): string {
    const trimmed: WidgetCheckReport = {...report, errors: report.errors.slice(0, 6), console: [...new Set(report.console)].slice(0, 5)};
    let reason = JSON.stringify(trimmed);
    while (reason.length > MAX_REASON_LENGTH && (trimmed.console.length || trimmed.errors.length > 1)) {
      if (trimmed.console.length) {
        trimmed.console.pop();
      } else {
        trimmed.errors.pop();
      }
      reason = JSON.stringify(trimmed);
    }
    return reason.length > MAX_REASON_LENGTH ? JSON.stringify({...trimmed, errors: [trimmed.errors[0]?.slice(0, 600)], console: []}) : reason;
  }

  private plain(html: string): string {
    const div = document.createElement('div');
    div.innerHTML = html;
    return (div.textContent || '').replace(/\s+/g, ' ').trim();
  }

  private clip(text: string): string {
    const single = (text || '').replace(/\s+/g, ' ').trim();
    return single.length > MAX_LINE_LENGTH ? single.substring(0, MAX_LINE_LENGTH) + '…' : single;
  }

  private safeJson(value: any): string {
    try {
      return JSON.stringify(value);
    } catch (e) {
      return String(value);
    }
  }

}
