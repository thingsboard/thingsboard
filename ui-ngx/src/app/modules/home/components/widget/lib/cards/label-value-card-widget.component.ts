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
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input,
  OnDestroy,
  OnInit,
  Renderer2,
  TemplateRef,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { WidgetContext } from '@home/models/widget-component.models';
import { Observable } from 'rxjs';
import {
  backgroundStyle,
  ColorProcessor,
  ComponentStyle,
  createValueFormatterFromSettings,
  getSingleTsValue,
  iconStyle,
  overlayStyle,
  resolveCssSize,
  textStyle,
  ValueFormatProcessor
} from '@shared/models/widget-settings.models';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import {
  labelValueCardWidgetDefaultSettings,
  LabelValueCardWidgetSettings
} from '@home/components/widget/lib/cards/label-value-card-widget.models';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-label-value-card-widget',
  templateUrl: './label-value-card-widget.component.html',
  styleUrls: ['./label-value-card-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class LabelValueCardWidgetComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('labelCardPanel', {static: false})
  labelCardPanel: ElementRef<HTMLElement>;

  @ViewChild('labelCardContent', {static: false})
  labelCardContent: ElementRef<HTMLElement>;

  @ViewChild('labelCardRow', {static: false})
  labelCardRow: ElementRef<HTMLElement>;

  settings: LabelValueCardWidgetSettings;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  showLabel = true;
  label$: Observable<string>;
  labelStyle: ComponentStyle = {};
  labelColor: ColorProcessor;

  valueText = 'N/A';
  valueStyle: ComponentStyle = {};
  valueColor: ColorProcessor;

  showIcon = true;
  icon = '';
  iconStyle: ComponentStyle = {};
  iconColor: ColorProcessor;

  hasCardClickAction = false;

  private panelResize$: ResizeObserver;
  private valueFormat: ValueFormatProcessor;

  constructor(private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.labelValueCardWidget = this;
    this.settings = {...labelValueCardWidgetDefaultSettings, ...this.ctx.settings};

    this.valueFormat = createValueFormatterFromSettings(this.ctx);

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
    this.padding = this.settings.background.overlay.enabled ? undefined : this.settings.padding;

    this.showIcon = this.settings.showIcon;
    this.icon = this.settings.icon;
    this.iconStyle = iconStyle(this.settings.iconSize, this.settings.iconSizeUnit);
    this.iconColor = ColorProcessor.fromSettings(this.settings.iconColor);

    this.showLabel = this.settings.showLabel;
    const label = this.settings.label;
    this.label$ = this.ctx.registerLabelPattern(label, this.label$);
    this.labelStyle = textStyle(this.settings.labelFont);
    this.labelColor =  ColorProcessor.fromSettings(this.settings.labelColor);
    this.valueStyle = textStyle(this.settings.valueFont);
    this.valueColor = ColorProcessor.fromSettings(this.settings.valueColor);

    this.hasCardClickAction = this.ctx.actionsApi.getActionDescriptors('cardClick').length > 0;
  }

  public ngAfterViewInit() {
    if (this.settings.autoScale) {
      this.renderer.setStyle(this.labelCardContent.nativeElement, 'overflow', 'visible');
      this.renderer.setStyle(this.labelCardContent.nativeElement, 'position', 'absolute');
      this.panelResize$ = new ResizeObserver(() => {
        this.onResize();
      });
      this.panelResize$.observe(this.labelCardPanel.nativeElement);
      this.onResize();
    }
  }

  ngOnDestroy() {
    if (this.panelResize$) {
      this.panelResize$.disconnect();
    }
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

  public onDataUpdated() {
    const tsValue = getSingleTsValue(this.ctx.data);
    let value;
    if (tsValue && isDefinedAndNotNull(tsValue[1]) && tsValue[0] !== 0) {
      value = tsValue[1];
      this.valueText = this.valueFormat.format(value);
    } else {
      this.valueText = 'N/A';
    }
    this.iconColor.update(value);
    this.labelColor.update(value);
    this.valueColor.update(value);
    this.cd.detectChanges();
    if (this.settings.autoScale) {
      setTimeout(() => {
        this.onResize();
      }, 0);
    }
  }

  public cardClick($event: Event) {
    this.ctx.actionsApi.cardClick($event);
  }

  private onResize() {
    const paddingLeft = getComputedStyle(this.labelCardPanel.nativeElement).paddingLeft;
    const paddingRight = getComputedStyle(this.labelCardPanel.nativeElement).paddingRight;
    const paddingTop = getComputedStyle(this.labelCardPanel.nativeElement).paddingTop;
    const paddingBottom = getComputedStyle(this.labelCardPanel.nativeElement).paddingBottom;
    const pLeft = resolveCssSize(paddingLeft)[0];
    const pRight = resolveCssSize(paddingRight)[0];
    const pTop = resolveCssSize(paddingTop)[0];
    const pBottom = resolveCssSize(paddingBottom)[0];
    const panelWidth = this.labelCardPanel.nativeElement.getBoundingClientRect().width - (pLeft + pRight);
    const panelHeight = this.labelCardPanel.nativeElement.getBoundingClientRect().height - (pTop + pBottom);
    this.renderer.setStyle(this.labelCardContent.nativeElement, 'width', 'auto');
    this.renderer.setStyle(this.labelCardContent.nativeElement, 'transform', `none`);
    const contentWidth = this.labelCardRow.nativeElement.getBoundingClientRect().width;
    const contentHeight = this.labelCardRow.nativeElement.getBoundingClientRect().height;
    const panelAspect = panelWidth / panelHeight;
    const contentAspect = contentWidth / contentHeight;
    let scale: number;
    if (contentAspect > panelAspect) {
      scale = panelWidth / contentWidth;
    } else {
      scale = panelHeight / contentHeight;
    }
    const width = panelWidth / scale;
    this.renderer.setStyle(this.labelCardContent.nativeElement, 'width', width + 'px');
    this.renderer.setStyle(this.labelCardContent.nativeElement, 'transform', `scale(${scale})`);
  }

}
