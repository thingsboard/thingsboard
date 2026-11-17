// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { from, Observable, of } from 'rxjs';
import { catchError, filter, map, switchMap, take, timeout } from 'rxjs/operators';
import html2canvas from 'html2canvas';
import { WidgetService } from '@core/http/widget.service';
import { AiResourceService } from '@core/http/ai-resource.service';
import { migrateWidgetTypeToDynamicForms } from '@shared/models/widget.models';
import { detailsToWidgetInfo, WidgetInfo } from '@home/models/widget-component.models';

const SETTLE_MS = 2500;
const RUN_TIMEOUT_MS = 30000;
const MAX_REASON_LENGTH = 1900;
const MAX_LINE_LENGTH = 300;
const PREVIEW_WIDTH = 1280;
const PREVIEW_HEIGHT = 800;

interface WidgetCheckReport {
  initialised: boolean;
  errors: string[];
  console: string[];
  noData?: boolean;
  width?: number;
  height?: number;
  screenshot?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AiWidgetCheckService {

  constructor(private widgetService: WidgetService,
              private aiResources: AiResourceService) {
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

  private run(widget: WidgetInfo): Promise<WidgetCheckReport & {canvas?: HTMLCanvasElement}> {
    return new Promise(resolve => {
      const report: WidgetCheckReport & {canvas?: HTMLCanvasElement} = {initialised: false, errors: [], console: []};
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
    const widget = doc.querySelector('tb-widget') as HTMLElement;
    if (widget) {
      const rect = widget.getBoundingClientRect();
      report.width = Math.round(rect.width);
      report.height = Math.round(rect.height);
    }
  }

  private capture(frame: HTMLIFrameElement): Promise<HTMLCanvasElement | undefined> {
    const doc = frame.contentDocument;
    const target = (doc?.querySelector('tb-widget-container') || doc?.querySelector('tb-widget')) as HTMLElement;
    if (!target) {
      return Promise.resolve(undefined);
    }
    return html2canvas(target, {logging: false, useCORS: true, backgroundColor: null, scale: 1})
      .catch(() => undefined);
  }

  private attachScreenshot(report: WidgetCheckReport & {canvas?: HTMLCanvasElement}): Observable<WidgetCheckReport> {
    const canvas = report.canvas;
    delete report.canvas;
    if (!canvas) {
      return of(report);
    }
    return from(new Promise<Blob>(resolve => canvas.toBlob(blob => resolve(blob), 'image/png'))).pipe(
      switchMap(blob => blob ? this.aiResources.upload(new File([blob], 'widget-check.png', {type: 'image/png'})) : of(null)),
      filter(event => !event || event.type === 'done'),
      take(1),
      map(event => {
        if (event?.type === 'done' && event.resource?.id) {
          report.screenshot = event.resource.id;
        }
        return report;
      }),
      catchError(() => of(report))
    );
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
