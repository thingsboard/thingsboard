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
import { isDefinedAndNotNull, isNumeric } from '@core/utils';
import {
  backgroundStyle,
  ColorProcessor,
  ComponentStyle,
  createValueFormatterFromSettings,
  getSingleTsValue,
  overlayStyle,
  resolveCssSize,
  textStyle,
  ValueFormatProcessor
} from '@shared/models/widget-settings.models';
import { WidgetComponent } from '@home/components/widget/widget.component';
import { progressBarDefaultSettings, ProgressBarLayout, ProgressBarWidgetSettings } from './progress-bar-widget.models';
import { Observable } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';

const defaultLayoutHeight = 80;
const simplifiedLayoutHeight = 75;
const defaultAspect = defaultLayoutHeight / 150;
const simplifiedAspect = simplifiedLayoutHeight / 150;

@Component({
    selector: 'tb-progress-bar-widget',
    templateUrl: './progress-bar-widget.component.html',
    styleUrls: ['./progress-bar-widget.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ProgressBarWidgetComponent implements OnInit, OnDestroy, AfterViewInit {

  @ViewChild('progressBarPanel', {static: true})
  progressBarPanel: ElementRef<HTMLElement>;

  @ViewChild('progressBarContainer', {static: true})
  progressBarContainer: ElementRef<HTMLElement>;

  progressBarLayout = ProgressBarLayout;

  settings: ProgressBarWidgetSettings;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  layout: ProgressBarLayout;

  layoutClass = '';

  showTitleValueRow = true;
  titleValueRowClass = '';

  showValue = true;
  valueText = 'N/A';
  valueStyle: ComponentStyle = {};
  valueColor: ColorProcessor;

  barWidth = '0%';
  barColor: ColorProcessor;

  showTicks = true;
  ticksStyle: ComponentStyle = {};

  value: number;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  private progressBarPanelResize$: ResizeObserver;
  private valueFormat: ValueFormatProcessor;

  constructor(private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              private widgetComponent: WidgetComponent,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.progressBarWidget = this;
    this.settings = {...progressBarDefaultSettings, ...this.ctx.settings};

    this.valueFormat = createValueFormatterFromSettings(this.ctx);

    this.layout = this.settings.layout;

    this.showValue = this.settings.showValue;
    this.valueStyle = textStyle(this.settings.valueFont);
    this.valueColor = ColorProcessor.fromColorProcessorSettings({
      settings: this.settings.valueColor,
      ctx: this.ctx,
      minGradientValue: this.settings.tickMin,
      maxGradientValue: this.settings.tickMax
    });

    this.showTitleValueRow = this.showValue ||
      (this.layout === ProgressBarLayout.simplified && this.widgetComponent.dashboardWidget.showWidgetTitlePanel);

    this.layoutClass = (this.layout === ProgressBarLayout.simplified || !this.widgetComponent.dashboardWidget.showWidgetTitlePanel)
      ? 'simplified' : '';

    this.titleValueRowClass = (this.layout === ProgressBarLayout.simplified &&
      !this.widgetComponent.dashboardWidget.showWidgetTitlePanel) ? 'flex-end' : '';

    this.barColor = ColorProcessor.fromColorProcessorSettings({
      settings: this.settings.barColor,
      ctx: this.ctx,
      minGradientValue: this.settings.tickMin,
      maxGradientValue: this.settings.tickMax
    });

    this.showTicks = this.settings.showTicks && this.layout === ProgressBarLayout.default;
    if (this.showTicks) {
      this.ticksStyle = textStyle(this.settings.ticksFont);
      this.ticksStyle.color = this.settings.ticksColor;
    }

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
    this.padding = this.settings.background.overlay.enabled ? undefined : this.settings.padding;
  }

  ngAfterViewInit() {
    if (this.settings.autoScale) {
      this.renderer.setStyle(this.progressBarContainer.nativeElement, 'overflow', 'visible');
      this.renderer.setStyle(this.progressBarContainer.nativeElement, 'position', 'absolute');
      this.progressBarPanelResize$ = new ResizeObserver(() => {
        this.onResize();
      });
      this.progressBarPanelResize$.observe(this.progressBarPanel.nativeElement);
      this.onResize();
    }
  }

  ngOnDestroy() {
    if (this.progressBarPanelResize$) {
      this.progressBarPanelResize$.disconnect();
    }
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

  public onDataUpdated() {
    const tsValue = getSingleTsValue(this.ctx.data);
    this.value = 0;
    if (tsValue && isDefinedAndNotNull(tsValue[1]) && isNumeric(tsValue[1])) {
      this.value = tsValue[1];
      this.valueText = this.valueFormat.format(this.value);
    } else {
      this.valueText = 'N/A';
    }
    this.valueColor.update(this.value);
    this.barColor.update(this.value);
    const range = this.settings.tickMax - this.settings.tickMin;
    let barWidthValue = ((this.value - this.settings.tickMin) / range) * 100;
    barWidthValue = Math.max(0, Math.min(100, barWidthValue));
    this.barWidth = `${barWidthValue}%`;
    this.cd.detectChanges();
  }

  private onResize() {
    const paddingLeft = getComputedStyle(this.progressBarPanel.nativeElement).paddingLeft;
    const paddingRight = getComputedStyle(this.progressBarPanel.nativeElement).paddingRight;
    const paddingTop = getComputedStyle(this.progressBarPanel.nativeElement).paddingTop;
    const paddingBottom = getComputedStyle(this.progressBarPanel.nativeElement).paddingBottom;
    const pLeft = resolveCssSize(paddingLeft)[0];
    const pRight = resolveCssSize(paddingRight)[0];
    const pTop = resolveCssSize(paddingTop)[0];
    const pBottom = resolveCssSize(paddingBottom)[0];
    const panelWidth = this.progressBarPanel.nativeElement.getBoundingClientRect().width - (pLeft + pRight);
    const panelHeight = this.progressBarPanel.nativeElement.getBoundingClientRect().height - (pTop + pBottom);
    const defaultLayout = this.layout === ProgressBarLayout.default && this.widgetComponent.dashboardWidget.showWidgetTitlePanel;
    let minAspect = defaultLayout ? defaultAspect : simplifiedAspect;
    let layoutHeight = defaultLayout ? defaultLayoutHeight : simplifiedLayoutHeight;
    if (!this.showTitleValueRow) {
      minAspect -= (40 / 150);
      layoutHeight -= 40;
    }
    if (!this.showTicks) {
      minAspect -= (10 / 150);
      layoutHeight -= 10;
    }
    const aspect = Math.min(panelHeight / panelWidth, minAspect);
    const targetWidth = panelWidth;
    const targetHeight = targetWidth * aspect;
    const scale = targetHeight / layoutHeight;
    const width = targetWidth / scale;
    const height = panelHeight / scale;
    this.renderer.setStyle(this.progressBarContainer.nativeElement, 'width', width + 'px');
    this.renderer.setStyle(this.progressBarContainer.nativeElement, 'height', height + 'px');
    this.renderer.setStyle(this.progressBarContainer.nativeElement, 'transform', `scale(${scale})`);
  }

}
