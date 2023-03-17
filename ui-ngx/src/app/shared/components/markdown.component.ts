///
/// Copyright © 2016-2023 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import {
  ChangeDetectorRef,
  Component,
  ComponentFactory,
  ComponentRef,
  ElementRef,
  EventEmitter,
  Inject,
  Injector,
  Input,
  OnChanges,
  Output,
  Renderer2,
  SimpleChanges,
  Type,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { HelpService } from '@core/services/help.service';
import { MarkdownService, PrismPlugin } from 'ngx-markdown';
import { DynamicComponentFactoryService } from '@core/services/dynamic-component-factory.service';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { SHARED_MODULE_TOKEN } from '@shared/components/tokens';
import { deepClone, guid, isDefinedAndNotNull } from '@core/utils';
import { Observable, of, ReplaySubject } from 'rxjs';

let defaultMarkdownStyle;

@Component({
  selector: 'tb-markdown',
  templateUrl: './markdown.component.html',
  styleUrls: ['./markdown.component.scss']
})
export class TbMarkdownComponent implements OnChanges {

  @ViewChild('markdownContainer', {read: ViewContainerRef, static: true}) markdownContainer: ViewContainerRef;
  @ViewChild('fallbackElement', {static: true}) fallbackElement: ElementRef<HTMLElement>;

  @Input() data: string | undefined;

  @Input() context: any;

  @Input() additionalCompileModules: Type<any>[];

  @Input() markdownClass: string | undefined;

  @Input() containerClass: string | undefined;

  @Input() style: { [klass: string]: any } = {};

  @Input() applyDefaultMarkdownStyle = true;

  @Input() additionalStyles: string[];

  @Input()
  get lineNumbers(): boolean { return this.lineNumbersValue; }
  set lineNumbers(value: boolean) { this.lineNumbersValue = coerceBooleanProperty(value); }

  @Input()
  get fallbackToPlainMarkdown(): boolean { return this.fallbackToPlainMarkdownValue; }
  set fallbackToPlainMarkdown(value: boolean) { this.fallbackToPlainMarkdownValue = coerceBooleanProperty(value); }

  @Output() ready = new EventEmitter<void>();

  private lineNumbersValue = false;
  private fallbackToPlainMarkdownValue = false;

  isMarkdownReady = false;

  error = null;

  private tbMarkdownInstanceComponentRef: ComponentRef<any>;
  private tbMarkdownInstanceComponentFactory: ComponentFactory<any>;

  constructor(private help: HelpService,
              private cd: ChangeDetectorRef,
              public markdownService: MarkdownService,
              @Inject(SHARED_MODULE_TOKEN) private sharedModule: Type<any>,
              private dynamicComponentFactoryService: DynamicComponentFactoryService,
              private renderer: Renderer2) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (isDefinedAndNotNull(this.data)) {
      this.render(this.data);
    }
  }

  private render(markdown: string) {
    const compiled = this.markdownService.parse(markdown, { decodeHtml: false });
    let markdownClass = 'tb-markdown-view';
    if (this.markdownClass) {
      markdownClass += ` ${this.markdownClass}`;
    }
    let template = `<div [ngStyle]="style" class="${markdownClass}">${compiled}</div>`;
    if (this.containerClass) {
      template = `<div class="${this.containerClass}" style="width: 100%; height: 100%;">${template}</div>`;
    }
    const element: HTMLDivElement = this.renderer.createElement('div');
    element.innerHTML = template;
    this.handlePlugins(element);
    this.markdownService.highlight(element);
    const preElements = element.querySelectorAll('pre');
    const matches = Array.from(template.matchAll(/<pre[\S\s]+?(?=<\/pre>)<\/pre>/g));
    for (let i = 0; i < preElements.length; i++) {
      const preHtml = preElements.item(i).outerHTML.replace('ngnonbindable=""', 'ngNonBindable');
      template = template.replace(matches[i][0], preHtml);
    }
    template = this.sanitizeCurlyBraces(template);
    this.markdownContainer.clear();
    const parent = this;
    let readyObservable: Observable<void>;
    let compileModules = [this.sharedModule];
    if (this.additionalCompileModules) {
      compileModules = compileModules.concat(this.additionalCompileModules);
    }
    let styles: string[] = [];
    if (this.applyDefaultMarkdownStyle) {
      if (!defaultMarkdownStyle) {
        defaultMarkdownStyle = deepClone(TbMarkdownComponent['ɵcmp'].styles)[0].replace(/\[_nghost\-%COMP%\]/g, '')
          .replace(/\[_ngcontent\-%COMP%\]/g, '');
      }
      styles.push(defaultMarkdownStyle);
    }
    if (this.additionalStyles) {
      styles = styles.concat(this.additionalStyles);
    }
    this.dynamicComponentFactoryService.createDynamicComponentFactory(
      class TbMarkdownInstance {
        ngOnDestroy(): void {
          parent.destroyMarkdownInstanceResources();
        }
      },
      template,
      compileModules,
      true, 1, styles
    ).subscribe((factory) => {
      this.tbMarkdownInstanceComponentFactory = factory;
      const injector: Injector = Injector.create({providers: [], parent: this.markdownContainer.injector});
      try {
        this.tbMarkdownInstanceComponentRef =
          this.markdownContainer.createComponent(this.tbMarkdownInstanceComponentFactory, 0, injector);
        if (this.context) {
          for (const propName of Object.keys(this.context)) {
            this.tbMarkdownInstanceComponentRef.instance[propName] = this.context[propName];
          }
        }
        this.tbMarkdownInstanceComponentRef.instance.style = this.style;
        readyObservable = this.handleImages(this.tbMarkdownInstanceComponentRef.location.nativeElement);
        this.cd.detectChanges();
        this.error = null;
      } catch (error) {
        readyObservable = this.handleError(template, error, styles);
      }
      readyObservable.subscribe(() => {
        this.ready.emit();
      });
    },
    (error) => {
      readyObservable = this.handleError(template, error, styles);
      this.cd.detectChanges();
      readyObservable.subscribe(() => {
        this.ready.emit();
      });
    });
  }

  private handleError(template: string, error, styles?: string[]): Observable<void> {
    this.error = (error ? error + '' : 'Failed to render markdown!').replace(/\n/g, '<br>');
    this.markdownContainer.clear();
    if (this.fallbackToPlainMarkdownValue) {
      const element = this.fallbackElement.nativeElement;
      let styleElement;
      if (styles?.length) {
        const markdownClass = 'tb-markdown-view-' + guid();
        let innerStyle = styles.join('\n');
        innerStyle = innerStyle.replace(/\.tb-markdown-view/g, '.' + markdownClass);
        template = template.replace(/tb-markdown-view/g, markdownClass);
        styleElement = this.renderer.createElement('style');
        styleElement.innerHTML = innerStyle;
      }
      element.innerHTML = template;
      if (styleElement) {
        this.renderer.appendChild(element, styleElement);
      }
      return this.handleImages(element);
    } else {
      return of(null);
    }
  }

  private handlePlugins(element: HTMLElement): void {
    if (this.lineNumbers) {
      this.setPluginClass(element, PrismPlugin.LineNumbers);
    }
  }

  private setPluginClass(element: HTMLElement, plugin: string | string[]): void {
    const preElements = element.querySelectorAll('pre');
    for (let i = 0; i < preElements.length; i++) {
      const classes = plugin instanceof Array ? plugin : [plugin];
      preElements.item(i).classList.add(...classes);
    }
  }

  private handleImages(element: HTMLElement): Observable<void> {
    const imgs = $('img', element);
    if (imgs.length) {
      let totalImages = imgs.length;
      const imagesLoadedSubject = new ReplaySubject<void>();
      imgs.each((index, img) => {
        $(img).one('load error', () => {
          totalImages--;
          if (totalImages === 0) {
            imagesLoadedSubject.next();
            imagesLoadedSubject.complete();
          }
        });
      });
      return imagesLoadedSubject.asObservable();
    } else {
      return of(null);
    }
  }

  private sanitizeCurlyBraces(template: string): string {
    return template.replace(/{/g, '&#123;').replace(/}/g, '&#125;');
  }

  private destroyMarkdownInstanceResources() {
    if (this.tbMarkdownInstanceComponentFactory) {
      this.dynamicComponentFactoryService.destroyDynamicComponentFactory(this.tbMarkdownInstanceComponentFactory);
      this.tbMarkdownInstanceComponentFactory = null;
    }
    this.tbMarkdownInstanceComponentRef = null;
  }
}
