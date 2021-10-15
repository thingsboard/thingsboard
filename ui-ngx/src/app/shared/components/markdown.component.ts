///
/// Copyright © 2016-2021 The Thingsboard Authors
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
  EventEmitter,
  Inject,
  Injector,
  Input, OnChanges,
  Output,
  SimpleChanges,
  Type, ViewChild,
  ViewContainerRef
} from '@angular/core';
import { HelpService } from '@core/services/help.service';
import { MarkdownService, PrismPlugin } from 'ngx-markdown';
import { DynamicComponentFactoryService } from '@core/services/dynamic-component-factory.service';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { SHARED_MODULE_TOKEN } from '@shared/components/tokens';

@Component({
  selector: 'tb-markdown',
  template: '<ng-container #markdownContainer>' +
            '</ng-container>' +
            '<div *ngIf="error" style="color: #f00; font-size: 14px;' +
                                      ' line-height: 28px;' +
                                      ' background: #efefef;">' +
                      '{{error}}' +
            '</div>'
})
export class TbMarkdownComponent implements OnChanges {

  @ViewChild('markdownContainer', {read: ViewContainerRef, static: true}) markdownContainer: ViewContainerRef;

  @Input() data: string | undefined;

  @Input() markdownClass: string | undefined;

  @Input() style: { [klass: string]: any } = {};

  @Input()
  get lineNumbers(): boolean { return this.lineNumbersValue; }
  set lineNumbers(value: boolean) { this.lineNumbersValue = coerceBooleanProperty(value); }

  @Output() ready = new EventEmitter<void>();

  private lineNumbersValue = false;

  isMarkdownReady = false;

  error = null;

  private tbMarkdownInstanceComponentRef: ComponentRef<any>;
  private tbMarkdownInstanceComponentFactory: ComponentFactory<any>;

  constructor(private help: HelpService,
              private cd: ChangeDetectorRef,
              public markdownService: MarkdownService,
              @Inject(SHARED_MODULE_TOKEN) private sharedModule: Type<any>,
              private dynamicComponentFactoryService: DynamicComponentFactoryService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (this.data) {
      this.render(this.data);
    }
  }

  private render(markdown: string) {
    let template = this.markdownService.compile(markdown, false);
    template = this.sanitizeCurlyBraces(template);
    let markdownClass = 'tb-markdown-view';
    if (this.markdownClass) {
      markdownClass += ` ${this.markdownClass}`;
    }
    template = `<div [ngStyle]="style" class="${markdownClass}">${template}</div>`;
    this.markdownContainer.clear();
    const parent = this;
    this.dynamicComponentFactoryService.createDynamicComponentFactory(
      class TbMarkdownInstance {
        ngOnDestroy(): void {
          parent.destroyMarkdownInstanceResources();
        }
      },
      template,
      [this.sharedModule],
      true
    ).subscribe((factory) => {
      this.tbMarkdownInstanceComponentFactory = factory;
      const injector: Injector = Injector.create({providers: [], parent: this.markdownContainer.injector});
      try {
        this.tbMarkdownInstanceComponentRef =
          this.markdownContainer.createComponent(this.tbMarkdownInstanceComponentFactory, 0, injector);
        this.tbMarkdownInstanceComponentRef.instance.style = this.style;
        this.handlePlugins(this.tbMarkdownInstanceComponentRef.location.nativeElement);
        this.markdownService.highlight(this.tbMarkdownInstanceComponentRef.location.nativeElement);
        this.error = null;
      } catch (error) {
        this.error = (error ? error + '' : 'Failed to render markdown!').replace(/\n/g, '<br>');
        this.destroyMarkdownInstanceResources();
      }
      this.cd.detectChanges();
      this.ready.emit();
    },
    (error) => {
      this.error = (error ? error + '' : 'Failed to render markdown!').replace(/\n/g, '<br>');
      this.destroyMarkdownInstanceResources();
      this.cd.detectChanges();
      this.ready.emit();
    });
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
