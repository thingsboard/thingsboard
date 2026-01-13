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
import {
  windSpeedDirectionDefaultSettings,
  WindSpeedDirectionLayout,
  WindSpeedDirectionWidgetSettings
} from '@home/components/widget/lib/weather/wind-speed-direction-widget.models';
import {
  backgroundStyle,
  ColorProcessor,
  ComponentStyle,
  Font,
  getDataKey,
  getSingleTsValueByDataKey,
  overlayStyle,
  ValueFormatProcessor
} from '@shared/models/widget-settings.models';
import { formatValue, isDefinedAndNotNull, isNumeric } from '@core/utils';
import { Path, Svg, SVG, Text } from '@svgdotjs/svg.js';
import { DataKey } from '@shared/models/widget.models';
import { Observable } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { UnitService } from '@core/services/unit.service';

const shapeSize = 180;
const cx = shapeSize / 2;
const cy = shapeSize / 2;
const ticksDiameter = 140;

const ticksTextMap: {[angle: number]: string} = {
  0: 'N',
  45: 'NE',
  90: 'E',
  135: 'SE',
  180: 'S',
  225: 'SW',
  270: 'W',
  315: 'NW'
};

@Component({
  selector: 'tb-wind-speed-direction-widget',
  templateUrl: './wind-speed-direction-widget.component.html',
  styleUrls: ['./wind-speed-direction-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class WindSpeedDirectionWidgetComponent implements OnInit, OnDestroy, AfterViewInit {

  @ViewChild('windSpeedDirectionShape', {static: false})
  windSpeedDirectionShape: ElementRef<HTMLElement>;

  settings: WindSpeedDirectionWidgetSettings;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  layout: WindSpeedDirectionLayout;

  centerValueColor: ColorProcessor;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  shapeResize$: ResizeObserver;

  hasCardClickAction = false;

  private drawSvgShapePending = false;
  private svgShape: Svg;
  private arrow: Path;
  private centerValueTextNode: Text;

  private units = ''
  private valueFormat: ValueFormatProcessor;

  private windDirectionDataKey: DataKey;
  private centerValueDataKey: DataKey;

  private windDirection = 0;
  private centerValueText = 'N/A';

  constructor(private imagePipe: ImagePipe,
              private unitService: UnitService,
              private sanitizer: DomSanitizer,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.windSpeedDirectionWidget = this;
    this.settings = {...windSpeedDirectionDefaultSettings, ...this.ctx.settings};

    this.windDirectionDataKey = getDataKey(this.ctx.datasources, 0);
    this.centerValueDataKey = getDataKey(this.ctx.datasources, 1);

    if (this.centerValueDataKey) {
      let decimals = this.ctx.decimals;
      let units = this.ctx.units;
      if (isDefinedAndNotNull(this.centerValueDataKey.decimals)) {
        decimals = this.centerValueDataKey.decimals;
      }
      if (this.centerValueDataKey.units) {
        units = this.centerValueDataKey.units;
      }
      this.units = this.unitService.getTargetUnitSymbol(units);
      this.valueFormat = ValueFormatProcessor.fromSettings(this.ctx.$injector, {units, decimals, ignoreUnitSymbol: true})
    }

    this.layout = this.settings.layout;

    this.centerValueColor = ColorProcessor.fromSettings(this.settings.centerValueColor);

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
    if (this.windSpeedDirectionShape) {
      this.drawSvg();
    } else {
      this.drawSvgShapePending = true;
    }
    this.cd.detectChanges();
  }

  public onDataUpdated() {
    let value = 0;
    this.windDirection = 0;
    this.centerValueText = 'N/A';
    if (this.windDirectionDataKey) {
      const windDirectionTsValue = getSingleTsValueByDataKey(this.ctx.data, this.windDirectionDataKey);
      if (windDirectionTsValue && isDefinedAndNotNull(windDirectionTsValue[1]) && isNumeric(windDirectionTsValue[1])) {
        this.windDirection = windDirectionTsValue[1];
        if (!this.centerValueDataKey) {
          value = this.windDirection;
          this.centerValueText = formatValue(value, 0, '', false) + '°';
        }
      }
    }
    if (this.centerValueDataKey) {
      const centerValueTsValue = getSingleTsValueByDataKey(this.ctx.data, this.centerValueDataKey);
      if (centerValueTsValue && isDefinedAndNotNull(centerValueTsValue[1]) && isNumeric(centerValueTsValue[1])) {
        value = centerValueTsValue[1];
        this.centerValueText = this.valueFormat.format(value);
      }
    }
    this.centerValueColor.update(value);
    this.renderValues();
  }

  public cardClick($event: Event) {
    this.ctx.actionsApi.cardClick($event);
  }

  private drawSvg() {
    this.svgShape = SVG().addTo(this.windSpeedDirectionShape.nativeElement).size(shapeSize, shapeSize);
    this.renderer.setStyle(this.svgShape.node, 'overflow', 'visible');
    this.renderer.setStyle(this.svgShape.node, 'user-select', 'none');

    // Draw ticks

    const ticksYStart = (shapeSize - ticksDiameter) / 2;
    for (let i = 0; i < 360; i += 3) {
      if (i !== 0) {
        let color: string;
        let width: number;
        let height: number;
        if (i % 90 === 0) {
          // Major ticks
          color = this.settings.majorTicksColor;
          width = 2;
          height = 8;
        } else if (i % 45 === 0) {
          // Minor ticks
          color = this.settings.minorTicksColor;
          width = 2;
          height = 8;
        } else {
          color = this.settings.ticksColor;
          width = 1.2;
          height = 3;
        }
        this.svgShape.line(cx, ticksYStart, cx, ticksYStart + height).attr({
          'stroke-width': width,
          stroke: color
        }).rotate(i, cx, cy);
      }
    }

    // Draw pointer
    this.svgShape.path('m 89.152,20.470002 c 0.3917,-0.626669 1.3043,-0.626669 1.696,0 l 3.1958,5.1132 ' +
      'c 0.4162,0.66605 -0.0626,1.53 -0.848,1.53 h -6.3916 c -0.7854,0 -1.2642,-0.86395 -0.848,-1.53 z')
    .fill(this.settings.majorTicksColor);

    let x: number;
    let y: number;
    let degree: number;

    const drawMajorTicksText = [ WindSpeedDirectionLayout.default, WindSpeedDirectionLayout.advanced ].includes(this.settings.layout);
    const drawMinorTicksText = this.settings.layout === WindSpeedDirectionLayout.advanced;

    if (drawMajorTicksText) {
      // Draw major ticks text
      for (let i = 0; i < 4; i += 1) {
        degree = i * 90;
        if (i % 2 === 0) {
          x = cx;
          y = i === 0 ? 10 : shapeSize - 10;
        } else {
          y = cy;
          x = i === 3 ? 10 : shapeSize - 10;
        }
        this.drawTickText(degree, this.settings.majorTicksFont, this.settings.majorTicksColor, x, y);
      }
    }

    if (drawMinorTicksText) {
      // Draw minor ticks text
      for (let i = 0; i < 4; i += 1) {
        degree = 45 + (i * 90);
        if (i < 2) {
          x = shapeSize - 30;
          y = i === 0 ? 30 : shapeSize - 30;
        } else {
          x = 30;
          y = i === 3 ? 30 : shapeSize - 30;
        }
        this.drawTickText(degree, this.settings.minorTicksFont, this.settings.minorTicksColor, x, y);
      }
    }

    // Draw arrow
    this.arrow = this.svgShape.path('m 89.263587,23.438382 c 0.388942,-0.392146 1.022181,-0.388549 1.414649,0 ' +
      'l 6.389758,6.389 c 0.392414,0.388462 0.394911,1.022828 0.0059,1.415 -0.388987,0.392109 -1.022226,0.383311 -1.41408,-0.006 ' +
      'l -4.6762,-4.676 v 28.417 h -2 v -28.417 l -4.637642,4.676 ' +
      'c -0.388878,0.392069 -1.022053,0.394895 -1.414202,0.006 -0.392082,-0.388967 -0.394683,-1.022852 -0.0057,-1.415 ' +
      'z M 88.983614,154.85438 h -2.217 v 2 h 6.434 v -2 h -2.217 v -29.939 h -2 z').fill(this.settings.arrowColor);

    // Draw value
    this.centerValueTextNode = this.svgShape.text('').font({
      family: this.settings.centerValueFont.family,
      weight: this.settings.centerValueFont.weight,
      style: this.settings.centerValueFont.style
    }).attr({x: '50%', y: '50%', 'text-anchor': 'middle'});
    if (!this.units) {
      this.centerValueTextNode.attr({'dominant-baseline': 'middle'});
    }

    this.shapeResize$ = new ResizeObserver(() => {
      this.onResize();
    });
    this.shapeResize$.observe(this.windSpeedDirectionShape.nativeElement);
    this.onResize();

    this.renderValues();
  }

  private drawTickText(degree: number, font: Font, color: string, x: number, y: number) {
    const tickText = this.settings.directionalNamesElseDegrees ? ticksTextMap[degree] : degree + '';
    this.svgShape.text(tickText).font({
      family: font.family,
      weight: font.weight,
      style: font.style,
      size: this.settings.directionalNamesElseDegrees ? '14px' : '10px'
    }).fill(color).center(x, y);
  }

  private renderValues() {
    if (this.svgShape) {
      this.arrow.timeline().finish();
      this.arrow.animate(800).transform({rotate: this.windDirection});
      this.renderCenterValueText();
    }
  }

  private renderCenterValueText() {
    this.centerValueTextNode.text(add => {
      add.tspan(this.centerValueText).font({size: '24px'});
      if (this.units) {
        add.tspan(this.units).newLine().font({size: '14px'});
      }
    }).fill(this.centerValueColor.color);
  }

  private onResize() {
    const shapeWidth = this.windSpeedDirectionShape.nativeElement.getBoundingClientRect().width;
    const shapeHeight = this.windSpeedDirectionShape.nativeElement.getBoundingClientRect().height;
    const size = Math.min(shapeWidth, shapeHeight);
    const scale = size / shapeSize;
    this.renderer.setStyle(this.svgShape.node, 'transform', `scale(${scale})`);
  }

}
