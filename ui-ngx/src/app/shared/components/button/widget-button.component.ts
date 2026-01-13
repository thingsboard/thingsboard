///
/// Copyright © 2016-2026 The Thingsboard Authors
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
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  Renderer2,
  SimpleChanges,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {
  generateWidgetButtonAppearanceCss,
  widgetButtonDefaultAppearance
} from '@shared/components/button/widget-button.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { ComponentStyle, iconStyle, validateCssSize } from '@shared/models/widget-settings.models';
import { UtilsService } from '@core/services/utils.service';
import { Observable, of } from 'rxjs';
import { WidgetContext } from '@home/models/widget-component.models';
import { isDefinedAndNotNull, isNotEmptyStr } from '@core/utils';

const initialButtonHeight = 60;
const horizontalLayoutPadding = 24;
const verticalLayoutPadding = 16;

@Component({
  selector: 'tb-widget-button',
  templateUrl: './widget-button.component.html',
  styleUrls: ['./widget-button.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class WidgetButtonComponent implements OnInit, AfterViewInit, OnDestroy, OnChanges {

  @ViewChild('widgetButton', {read: ElementRef})
  widgetButton: ElementRef<HTMLElement>;

  @ViewChild('widgetButtonContent', {static: false})
  widgetButtonContent: ElementRef<HTMLElement>;

  @Input()
  appearance = widgetButtonDefaultAppearance;

  @Input()
  borderRadius = '4px';

  @Input()
  autoScale: boolean;

  @Input()
  @coerceBoolean()
  disabled = false;

  @Input()
  @coerceBoolean()
  activated = false;

  @Input()
  @coerceBoolean()
  hovered = false;

  @Input()
  @coerceBoolean()
  pressed = false;

  @Input()
  @coerceBoolean()
  disableEvents = false;

  @Input()
  ctx: WidgetContext;

  @Output()
  clicked = new EventEmitter<MouseEvent>();

  label$: Observable<string>;

  iconStyle: ComponentStyle = {};

  computedBorderRadius: string;

  mousePressed = false;

  private buttonResize$: ResizeObserver;

  private appearanceCssClass: string;

  constructor(private renderer: Renderer2,
              private elementRef: ElementRef,
              private utils: UtilsService) {}

  ngOnInit(): void {
    this.updateAppearance();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange) {
        if (propName === 'appearance') {
          this.updateAppearance();
        } else if (propName === 'borderRadius') {
          this.updateBorderRadius();
        } else if (propName === 'autoScale') {
          this.updateAutoScale();
        }
      }
    }
  }

  ngAfterViewInit(): void {
    this.updateAutoScale();
  }

  ngOnDestroy(): void {
    if (this.buttonResize$) {
      this.buttonResize$.disconnect();
    }
    this.clearAppearanceCss();
  }

  public validateSize() {
    if (this.appearance.autoScale && this.widgetButton.nativeElement) {
      this.onResize();
    }
  }

  private updateAppearance(): void {
    this.clearAppearanceCss();
    if (this.appearance.showIcon) {
      this.iconStyle = iconStyle(this.appearance.iconSize, this.appearance.iconSizeUnit);
    }
    if (this.appearance.showLabel) {
      this.label$ = this.ctx ? this.ctx.registerLabelPattern(this.appearance.label, this.label$) : of(this.appearance.label);
    }
    this.updateBorderRadius();
    const appearanceCss = generateWidgetButtonAppearanceCss(this.appearance);
    this.appearanceCssClass = this.utils.applyCssToElement(this.renderer, this.elementRef.nativeElement,
      'tb-widget-button', appearanceCss);
    this.updateAutoScale();
  }

  private updateBorderRadius(): void {
    const validatedBorderRadius = validateCssSize(this.appearance.borderRadius);
    if (validatedBorderRadius) {
      this.computedBorderRadius = validatedBorderRadius;
    } else {
      this.computedBorderRadius = this.borderRadius;
    }
  }

  private clearAppearanceCss(): void {
    if (this.appearanceCssClass) {
      this.utils.clearCssElement(this.renderer, this.appearanceCssClass, this.elementRef?.nativeElement);
      this.appearanceCssClass = null;
    }
  }

  private updateAutoScale() {
    if (this.buttonResize$) {
      this.buttonResize$.disconnect();
    }
    if (this.widgetButton && this.widgetButtonContent) {
      const autoScale = isDefinedAndNotNull(this.autoScale) ? this.autoScale : this.appearance.autoScale;
      if (autoScale) {
        this.buttonResize$ = new ResizeObserver(() => {
          this.onResize();
        });
        this.buttonResize$.observe(this.widgetButton.nativeElement);
        this.onResize();
      } else {
        this.renderer.setStyle(this.widgetButtonContent.nativeElement, 'transform', 'none');
        this.renderer.setStyle(this.widgetButtonContent.nativeElement, 'width', '100%');
      }
    }
  }

  private onResize() {
    const height = this.widgetButton.nativeElement.getBoundingClientRect().height;
    const buttonScale = height / initialButtonHeight;
    const paddingScale = Math.min(buttonScale, 1);
    const buttonWidth = this.widgetButton.nativeElement.getBoundingClientRect().width - (horizontalLayoutPadding * paddingScale);
    const buttonHeight = this.widgetButton.nativeElement.getBoundingClientRect().height - (verticalLayoutPadding * paddingScale);
    this.renderer.setStyle(this.widgetButtonContent.nativeElement, 'transform', `scale(1)`);
    this.renderer.setStyle(this.widgetButtonContent.nativeElement, 'width', 'auto');
    const contentWidth = this.widgetButtonContent.nativeElement.getBoundingClientRect().width;
    const contentHeight = this.widgetButtonContent.nativeElement.getBoundingClientRect().height;
    const maxScale = Math.max(1, buttonScale);
    const scale = Math.min(Math.min(buttonWidth / contentWidth, buttonHeight / contentHeight), maxScale);
    const targetWidth = buttonWidth / scale;
    this.renderer.setStyle(this.widgetButtonContent.nativeElement, 'width', targetWidth + 'px');
    this.renderer.setStyle(this.widgetButtonContent.nativeElement, 'transform', `scale(${scale})`);
  }

}
