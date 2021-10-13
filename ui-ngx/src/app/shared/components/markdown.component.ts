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
  Component,
  ComponentFactory,
  ComponentRef,
  EventEmitter,
  Inject,
  Injector,
  Input, OnChanges,
  OnDestroy,
  OnInit,
  Output,
  Renderer2, SimpleChanges,
  Type,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { HelpService } from '@core/services/help.service';
import { MarkdownComponent } from 'ngx-markdown';
import { DynamicComponentFactoryService } from '@core/services/dynamic-component-factory.service';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { SHARED_MODULE_TOKEN } from '@shared/components/tokens';

@Component({
  selector: 'tb-markdown',
  templateUrl: './markdown.component.html',
  styleUrls: ['./markdown.component.scss']
})
export class TbMarkdownComponent implements OnDestroy, OnInit, OnChanges {

  private markdownComponent: MarkdownComponent;

  @ViewChild('markdownContainer', {read: ViewContainerRef, static: true}) markdownContainer: ViewContainerRef;

  @ViewChild('markdownComponent', {static: false}) set content(content: MarkdownComponent) {
      this.markdownComponent = content;
      if (this.isMarkdownReady && this.markdownComponent) {
        this.processMarkdownComponent();
      }
  }

  @Input() data: string | undefined;

  @Input() markdownClass: string | undefined;

  @Input() style: { [klass: string]: any } = {};

  @Input()
  get lineNumbers(): boolean { return this.lineNumbersValue; }
  set lineNumbers(value: boolean) { this.lineNumbersValue = coerceBooleanProperty(value); }

  @Output() ready = new EventEmitter<void>();

  private lineNumbersValue = false;

  isMarkdownReady = false;

  private tbMarkdownInstanceComponentRef: ComponentRef<any>;
  private tbMarkdownInstanceComponentFactory: ComponentFactory<any>;

  constructor(private help: HelpService,
              private renderer: Renderer2,
              @Inject(SHARED_MODULE_TOKEN) private sharedModule: Type<any>,
              private dynamicComponentFactoryService: DynamicComponentFactoryService) {}

  ngOnInit(): void {
  }

  ngOnDestroy(): void {
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'data') {
          if (this.data) {
            this.isMarkdownReady = false;
          }
        }
      }
    }
  }

  private processMarkdownComponent() {
    let template = this.markdownComponent.element.nativeElement.innerHTML;
    template = this.sanitizeCurlyBraces(template);
    let markdownClass = 'tb-markdown-view';
    if (this.markdownClass) {
      markdownClass += ` ${this.markdownClass}`;
    }
    template = `<div [ngStyle]="style" class="${markdownClass}">${template}</div>`;
    this.markdownContainer.clear();
    this.markdownComponent = null;
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
      } catch (e) {
        this.destroyMarkdownInstanceResources();
      }
      this.ready.emit();
    });
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

  onMarkdownReady() {
    if (this.data) {
      this.isMarkdownReady = true;
      if (this.markdownComponent) {
        this.processMarkdownComponent();
      }
    }
  }
}
