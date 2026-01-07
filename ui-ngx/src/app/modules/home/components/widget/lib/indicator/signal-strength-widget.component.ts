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
import {
  backgroundStyle,
  ColorProcessor,
  ComponentStyle,
  createValueFormatterFromSettings,
  DateFormatProcessor,
  getSingleTsValue,
  overlayStyle,
  textStyle,
  ValueFormatProcessor
} from '@shared/models/widget-settings.models';
import { WidgetComponent } from '@home/components/widget/widget.component';
import { isNumeric, isUndefinedOrNull } from '@core/utils';
import { Element, G, Svg, SVG } from '@svgdotjs/svg.js';
import {
  signalBarActive,
  signalStrengthDefaultSettings,
  SignalStrengthLayout,
  SignalStrengthWidgetSettings
} from '@home/components/widget/lib/indicator/signal-strength-widget.models';
import tinycolor from 'tinycolor2';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';

const shapeWidth = 149;
const shapeHeight = 113;
const shapeAspect = shapeWidth / shapeHeight;

@Component({
  selector: 'tb-signal-strength-widget',
  templateUrl: './signal-strength-widget.component.html',
  styleUrls: ['./signal-strength-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class SignalStrengthWidgetComponent implements OnInit, OnDestroy, AfterViewInit {

  @ViewChild('signalStrengthShape', {static: false})
  signalStrengthShape: ElementRef<HTMLElement>;

  @ViewChild('signalStrengthTooltip', {static: false})
  signalStrengthTooltip: ElementRef<HTMLElement>;

  settings: SignalStrengthWidgetSettings;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  layout: SignalStrengthLayout;

  activeBarsColor: ColorProcessor;

  showDate = false;
  dateFormat: DateFormatProcessor;
  dateStyle: ComponentStyle = {};

  showTooltip = false;

  tooltipStyle: ComponentStyle = {};

  showTooltipValue = false;
  tooltipValueText = 'N/A';
  tooltipValueLabelStyle: ComponentStyle = {
    color: 'rgba(0, 0, 0, 0.38)'
  };
  tooltipValueStyle: ComponentStyle = {};

  showTooltipDate = false;
  tooltipDateFormat: DateFormatProcessor;
  tooltipDateLabelStyle: ComponentStyle = {
    color: 'rgba(0, 0, 0, 0.38)'
  };
  tooltipDateStyle: ComponentStyle = {};

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  shapeResize$: ResizeObserver;

  hasCardClickAction = false;

  private valueFormat: ValueFormatProcessor;

  private drawSvgShapePending = false;
  private svgShape: Svg;
  private bars: Element[] = Array.from(Array(4), () => null);
  private centerTextElement: G;
  private lastCenterText: string;

  private inactiveBarsColorHex: string;
  private inactiveBarsOpacity: number;

  private rssi = -100;
  private noSignal = false;
  private noData = false;
  private noSignalRssiValue = -100;

  constructor(public widgetComponent: WidgetComponent,
              private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              private translate: TranslateService,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.signalStrengthWidget = this;
    this.settings = {...signalStrengthDefaultSettings, ...this.ctx.settings};
    this.layout = this.settings.layout;

    this.showDate = this.settings.showDate;
    if (this.showDate) {
      this.dateFormat = DateFormatProcessor.fromSettings(this.ctx.$injector, this.settings.dateFormat);
      this.dateStyle = textStyle(this.settings.dateFont);
      this.dateStyle.color = this.settings.dateColor;
    }

    this.noSignalRssiValue = this.settings.noSignalRssiValue ?? -100;
    this.rssi = this.noSignalRssiValue;

    this.activeBarsColor = ColorProcessor.fromSettings(this.settings.activeBarsColor);
    const inactiveBarsColor = tinycolor(this.settings.inactiveBarsColor);
    this.inactiveBarsColorHex = inactiveBarsColor.toHexString();
    this.inactiveBarsOpacity = inactiveBarsColor.getAlpha();

    this.showTooltip = this.settings.showTooltip && (this.settings.showTooltipValue || this.settings.showTooltipDate);

    if (this.showTooltip) {
      this.tooltipStyle = {
        background: this.settings.tooltipBackgroundColor,
        backdropFilter: `blur(${this.settings.tooltipBackgroundBlur}px)`
      };
    }

    this.showTooltipValue = this.showTooltip && this.settings.showTooltipValue;
    this.showTooltipDate = this.showTooltip && this.settings.showTooltipDate;

    if (this.showTooltipValue) {
      this.valueFormat = createValueFormatterFromSettings(this.ctx);
      this.tooltipValueStyle = textStyle(this.settings.tooltipValueFont);
      this.tooltipValueStyle.color = this.settings.tooltipValueColor;
      this.tooltipValueLabelStyle = {...this.tooltipValueStyle, ...this.tooltipValueLabelStyle};
    }

    if (this.showTooltipDate) {
      this.tooltipDateFormat = DateFormatProcessor.fromSettings(this.ctx.$injector,
        {...this.settings.tooltipDateFormat, ...{hideLastUpdatePrefix: true}});
      this.tooltipDateStyle = textStyle(this.settings.tooltipDateFont);
      this.tooltipDateStyle.color = this.settings.tooltipDateColor;
      this.tooltipDateLabelStyle = {...this.tooltipDateStyle, ...this.tooltipDateLabelStyle};
    }

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
    this.padding = this.settings.background.overlay.enabled ? undefined : this.settings.padding;

    this.hasCardClickAction = this.ctx.actionsApi.getActionDescriptors('cardClick').length > 0;
  }

  ngAfterViewInit() {
    if (this.drawSvgShapePending) {
      this.drawSvg();
    }
  }

  ngOnDestroy() {
    if (this.shapeResize$) {
      this.shapeResize$.disconnect();
    }
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    if (this.signalStrengthShape) {
      this.drawSvg();
    } else {
      this.drawSvgShapePending = true;
    }
    this.cd.detectChanges();
  }

  public onDataUpdated() {
    const tsValue = getSingleTsValue(this.ctx.data);
    let ts;
    let value;
    if (tsValue) {
      ts = tsValue[0];
      value = tsValue[1];
    } else {
      if (this.showTooltipValue) {
        this.tooltipValueText = 'N/A';
      }
    }

    this.noData = isUndefinedOrNull(value) || !isNumeric(value);
    if (!this.noData) {
      this.rssi = Number(value);
      if (this.showTooltipValue) {
        this.tooltipValueText = this.valueFormat.format(value);
      }
    } else {
      this.rssi = this.noSignalRssiValue;
      if (this.showTooltipValue) {
        this.tooltipValueText = 'N/A';
      }
    }

    this.noSignal = this.rssi <= this.noSignalRssiValue;

    this.activeBarsColor.update(this.rssi);

    if (this.showDate) {
      this.dateFormat.update(ts);
    }

    if (this.showTooltipDate) {
      this.tooltipDateFormat.update(ts);
    }

    this.cd.detectChanges();
    this.renderValues();
  }

  public cardClick($event: Event) {
    this.ctx.actionsApi.cardClick($event);
  }

  private drawSvg() {
    this.svgShape = SVG().addTo(this.signalStrengthShape.nativeElement).size(shapeWidth, shapeHeight);
    this.renderer.setStyle(this.svgShape.node, 'overflow', 'visible');
    this.renderer.setStyle(this.svgShape.node, 'user-select', 'none');

    // Draw bars

    if (this.layout === SignalStrengthLayout.wifi) {

      this.bars[0] = this.svgShape.path('m 74.5,101.5 c 4.9706,0 9,-4.0294 9,-9 0,-4.9706 -4.0294,-9 -9,-9 -4.9706,0 ' +
        '-9,4.0294 -9,9 0,4.9706 4.0294,9 9,9 z').fill(this.inactiveBarsColorHex).opacity(this.inactiveBarsOpacity);

      this.bars[1] = this.svgShape.path('M 100.475,69.1453 C 93.3268,62.9321 84.0946,59.5 74.5299,59.5 c -9.5647,0 -18.7969,3.4321 ' +
        '-25.9455,9.6453 -0.6041,0.5111 -1.0983,1.1344 -1.4539,1.8338 -0.3556,0.6994 -0.5656,1.4609 -0.6177,2.2402 -0.0521,0.7794 ' +
        '0.0547,1.5612 0.3142,2.2999 0.2595,0.7386 0.6665,1.4196 1.1974,2.0032 0.5308,0.5836 1.175,1.0582 1.8951,1.3964 0.72,0.3381 ' +
        '1.5016,0.533 2.2994,0.5733 0.7977,0.0404 1.5957,-0.0747 2.3476,-0.3384 0.7519,-0.2638 1.4427,-0.671 2.0323,-1.198 ' +
        '4.9407,-4.2934 11.321,-6.665 17.9311,-6.665 6.6101,0 12.9904,2.3716 17.9311,6.665 1.1957,1.0392 2.7646,1.5713 ' +
        '4.3616,1.4795 1.5969,-0.0919 3.0911,-0.8003 4.1544,-1.9694 1.062,-1.1691 1.607,-2.7031 1.513,-4.2645 ' +
        '-0.094,-1.5615 -0.819,-3.0225 -2.015,-4.0616 z').fill(this.inactiveBarsColorHex).opacity(this.inactiveBarsOpacity);

      this.bars[2] = this.svgShape.path('m 74.5,35.5001 c -16.3337,-0.0218 -32.061,5.9702 -43.9755,16.7543 -0.6032,0.5216 ' +
        '-1.0925,1.1541 -1.4391,1.8606 -0.3467,0.7065 -0.5436,1.4726 -0.5795,2.2536 -0.0358,0.7809 0.0903,1.5608 0.371,2.2941 ' +
        '0.2806,0.7332 0.71,1.4051 1.2632,1.976 0.5531,0.5709 1.2188,1.0295 1.958,1.3489 0.7392,0.3194 1.5371,0.493 2.3467,0.5109 ' +
        '0.8097,0.0178 1.6149,-0.1206 2.3684,-0.4071 0.7535,-0.2865 1.4402,-0.7153 2.0197,-1.2613 9.6742,-8.7299 22.4234,-13.5862 ' +
        '35.6671,-13.5862 13.2437,0 25.993,4.8563 35.667,13.5862 0.58,0.546 1.266,0.9748 2.02,1.2613 0.753,0.2865 1.559,0.4249 ' +
        '2.368,0.4071 0.81,-0.0179 1.608,-0.1915 2.347,-0.5109 0.739,-0.3194 1.405,-0.778 1.958,-1.3489 0.553,-0.5709 0.983,-1.2428 ' +
        '1.263,-1.976 0.281,-0.7333 0.407,-1.5132 0.371,-2.2941 -0.036,-0.781 -0.233,-1.5471 -0.579,-2.2536 -0.347,-0.7065 ' +
        '-0.836,-1.339 -1.44,-1.8606 ' +
        'C 106.561,41.4703 90.8337,35.4783 74.5,35.5001 Z').fill(this.inactiveBarsColorHex).opacity(this.inactiveBarsOpacity);

      this.bars[3] = this.svgShape.path('M 134.464,33.9289 C 117.976,19.4835 96.6509,11.5 74.5537,11.5 c -22.0972,0 -43.422,7.9835 ' +
        '-59.9102,22.4289 -0.6196,0.5198 -1.1273,1.1559 -1.493,1.871 -0.3658,0.7151 -0.5823,1.4948 -0.6368,2.2933 -0.0545,0.7984 ' +
        '0.054,1.5995 0.3193,2.3562 0.2652,0.7567 0.6818,1.4536 1.2253,2.05 0.5434,0.5963 1.2028,1.08 1.9393,1.4225 0.7366,0.3426 ' +
        '1.5354,0.5372 2.3496,0.5723 0.8142,0.0351 1.6273,-0.0899 2.3916,-0.3677 0.7643,-0.2778 1.4643,-0.7028 2.0589,-1.25 ' +
        '14.2455,-12.4752 32.6666,-19.3694 51.7545,-19.3694 19.0879,0 37.5088,6.8942 51.7548,19.3694 1.209,1.0617 2.798,1.6084 ' +
        '4.418,1.5196 1.619,-0.0887 3.137,-0.8056 4.218,-1.9929 1.081,-1.1873 1.638,-2.7477 1.547,-4.338 -0.09,-1.5903 ' +
        '-0.82,-3.0802 -2.029,-4.142 z').fill(this.inactiveBarsColorHex).opacity(this.inactiveBarsOpacity);
    } else {
      this.bars[0] = this.svgShape.rect(18, 37).radius(4).move(19, 75).fill(this.inactiveBarsColorHex).opacity(this.inactiveBarsOpacity);
      this.bars[1] = this.svgShape.rect(18, 61).radius(4).move(50, 50).fill(this.inactiveBarsColorHex).opacity(this.inactiveBarsOpacity);
      this.bars[2] = this.svgShape.rect(18, 86).radius(4).move(80, 26).fill(this.inactiveBarsColorHex).opacity(this.inactiveBarsOpacity);
      this.bars[3] = this.svgShape.rect(18, 111).radius(4).move(111, 1).fill(this.inactiveBarsColorHex).opacity(this.inactiveBarsOpacity);
    }

    this.shapeResize$ = new ResizeObserver(() => {
      this.onResize();
    });
    this.shapeResize$.observe(this.signalStrengthShape.nativeElement);
    this.onResize();

    this.renderValues();
  }

  private renderValues() {
    if (this.svgShape) {
      const activeBarsColor = tinycolor(this.activeBarsColor.color);
      const activeBarsColorHex = activeBarsColor.toHexString();
      const activeBarsOpacity = activeBarsColor.getAlpha();
      for (let index = 0; index < this.bars.length; index++) {
        const bar = this.bars[index];
        const active = signalBarActive(this.rssi, index, this.noSignalRssiValue);
        const newFill = active ? activeBarsColorHex : this.inactiveBarsColorHex;
        const newOpacity = active ? activeBarsOpacity : this.inactiveBarsOpacity;
        if (newFill !== bar.fill() || newOpacity !== bar.opacity()) {
          bar.timeline().finish();
          bar.animate(200).attr({fill: newFill, opacity: newOpacity});
        }
      }
      const centerText = this.noData ? 'N/A' : (this.noSignal ? this.translate.instant('widgets.signal-strength.no-signal') : '');
      this.renderCenterText(centerText);
    }
  }

  private renderCenterText(text: string) {
    if (this.lastCenterText !== text) {
      if (this.centerTextElement) {
        this.centerTextElement.remove();
        this.centerTextElement = null;
      }
      if (text) {
        this.centerTextElement = this.svgShape.group();
        const textElement = this.centerTextElement.text(text).font(
          {
            family: 'Roboto',
            weight: '500',
            style: 'normal',
            size: '12px',
            leading: 1.333
          }
        ).fill('#fff');
        const bounds = textElement.bbox();
        this.centerTextElement.rect(bounds.width + 16, bounds.height + 8)
        .move(bounds.x - 8, bounds.y - 4).radius(4).fill('#848484').insertBefore(textElement);
        this.centerTextElement.center(shapeWidth / 2, shapeHeight / 2);
      }
      this.lastCenterText = text;
    }
  }

  private onResize() {
    const shapeContainerWidth = this.signalStrengthShape.nativeElement.getBoundingClientRect().width;
    const shapeContainerHeight = this.signalStrengthShape.nativeElement.getBoundingClientRect().height;
    const shapeContainerAspect = shapeContainerWidth / shapeContainerHeight;
    let scale: number;
    if (shapeAspect >= shapeContainerAspect) {
      scale = shapeContainerWidth / shapeWidth;
    } else {
      scale = shapeContainerHeight / shapeHeight;
    }
    this.renderer.setStyle(this.svgShape.node, 'transform', `scale(${scale})`);
    if (this.showTooltip) {
      const targetWidth = shapeContainerWidth;
      const minAspect = 0.45;
      const aspect = Math.min(shapeContainerHeight / targetWidth, minAspect);
      const targetHeight = targetWidth * aspect;
      const height = 80;
      scale = targetHeight / height;
      const width = targetWidth / scale;
      this.renderer.setStyle(this.signalStrengthTooltip.nativeElement, 'width', width + 'px');
      this.renderer.setStyle(this.signalStrengthTooltip.nativeElement, 'transform', `scale(${scale})`);
    }
  }
}
