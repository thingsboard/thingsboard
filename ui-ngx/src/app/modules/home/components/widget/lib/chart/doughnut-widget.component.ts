///
/// Copyright © 2016-2024 The Thingsboard Authors
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
import {
  doughnutDefaultSettings,
  DoughnutLayout,
  DoughnutTooltipValueType,
  DoughnutWidgetSettings
} from '@home/components/widget/lib/chart/doughnut-widget.models';
import { WidgetContext } from '@home/models/widget-component.models';
import {
  backgroundStyle,
  ColorProcessor,
  ComponentStyle,
  overlayStyle,
  textStyle
} from '@shared/models/widget-settings.models';
import { ResizeObserver } from '@juggle/resize-observer';
import { WidgetComponent } from '@home/components/widget/widget.component';
import * as echarts from 'echarts/core';
import { TranslateService } from '@ngx-translate/core';
import { PieDataItemOption } from 'echarts/types/src/chart/pie/PieSeries';
import { formatValue, isDefinedAndNotNull, isNumeric } from '@core/utils';
import { SVG, Svg, Text } from '@svgdotjs/svg.js';
import { DataKey, LegendPosition } from '@shared/models/widget.models';
import { Observable } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { ECharts, echartsModule, EChartsOption } from '@home/components/widget/lib/chart/echarts-widget.models';

const shapeSize = 134;
const shapeSegmentWidth = 13.4;

interface DoughnutDataItem {
  id: number;
  dataKey: DataKey;
  value: number;
  hasValue: boolean;
  enabled: boolean;
}

interface DoughnutLegendItem {
  id: number;
  color: string;
  label: string;
  value: string;
  hasValue: boolean;
  enabled: boolean;
  total?: boolean;
}

@Component({
  selector: 'tb-doughnut-widget',
  templateUrl: './doughnut-widget.component.html',
  styleUrls: ['./doughnut-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class DoughnutWidgetComponent implements OnInit, OnDestroy, AfterViewInit {

  @ViewChild('doughnutContent', {static: false})
  doughnutContent: ElementRef<HTMLElement>;

  @ViewChild('doughnutShape', {static: false})
  doughnutShape: ElementRef<HTMLElement>;

  @ViewChild('doughnutLegend', {static: false})
  doughnutLegend: ElementRef<HTMLElement>;

  settings: DoughnutWidgetSettings;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  showLegend: boolean;
  legendClass: string;

  totalValueColor: ColorProcessor;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};

  legendItems: DoughnutLegendItem[];
  legendLabelStyle: ComponentStyle;
  legendValueStyle: ComponentStyle;
  disabledLegendLabelStyle: ComponentStyle;
  disabledLegendValueStyle: ComponentStyle;

  private shapeResize$: ResizeObserver;
  private legendHorizontal: boolean;

  private decimals = 0;
  private units = '';

  private total = 0;
  private totalText = 'N/A';
  private scale = 1;

  private dataItems: DoughnutDataItem[] = [];

  private drawDoughnutPending = false;
  private showTotal = false;
  private doughnutChart: ECharts;
  private doughnutOptions: EChartsOption;
  private svgShape: Svg;
  private totalTextNode: Text;

  constructor(private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              private widgetComponent: WidgetComponent,
              private renderer: Renderer2,
              private translate: TranslateService,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    const params = this.widgetComponent.typeParameters as any;
    const horizontal  = isDefinedAndNotNull(params.horizontal) ? params.horizontal : false;
    this.ctx.$scope.doughnutWidget = this;
    this.settings = {...doughnutDefaultSettings(horizontal), ...this.ctx.settings};

    this.decimals = this.ctx.decimals;
    this.units = this.ctx.units;

    this.showLegend = this.settings.showLegend;
    this.showTotal = this.settings.layout === DoughnutLayout.with_total;

    if (this.showTotal) {
      this.totalValueColor = ColorProcessor.fromSettings(this.settings.totalValueColor);
    }

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);

    if (this.showLegend) {
      this.legendItems = [];
      this.legendClass = `legend-${this.settings.legendPosition}`;
      this.legendHorizontal = [LegendPosition.left, LegendPosition.right].includes(this.settings.legendPosition);
      this.legendLabelStyle = textStyle(this.settings.legendLabelFont);
      this.disabledLegendLabelStyle = textStyle(this.settings.legendLabelFont);
      this.legendLabelStyle.color = this.settings.legendLabelColor;
      this.legendValueStyle = textStyle(this.settings.legendValueFont);
      this.disabledLegendValueStyle = textStyle(this.settings.legendValueFont);
      this.legendValueStyle.color = this.settings.legendValueColor;
    }
    let counter = 0;
    if (this.ctx.datasources.length) {
      for (const datasource of this.ctx.datasources) {
        const dataKeys = datasource.dataKeys;
        for (const dataKey of dataKeys) {
          const id = counter++;
          this.dataItems.push({
            id,
            dataKey,
            value: 0,
            hasValue: false,
            enabled: true
          });
          if (this.showLegend) {
            this.legendItems.push(
              {
                id,
                value: '--',
                label: dataKey.label,
                color: dataKey.color,
                enabled: true,
                hasValue: false
              }
            );
          }
        }
      }
    }
    if (this.settings.sortSeries) {
      this.dataItems.sort((a, b) => a.dataKey.label.localeCompare(b.dataKey.label));
      if (this.showLegend) {
        this.legendItems.sort((a, b) => a.label.localeCompare(b.label));
      }
    }
    if (this.showLegend && !this.showTotal) {
      this.legendItems.push(
        {
          id: null,
          value: '--',
          label: this.translate.instant('widgets.doughnut.total'),
          color: 'rgba(0, 0, 0, 0.06)',
          enabled: true,
          hasValue: false,
          total: true
        }
      );
    }
  }

  ngAfterViewInit() {
    if (this.drawDoughnutPending) {
      this.drawDoughnut();
    }
  }

  ngOnDestroy() {
    if (this.shapeResize$) {
      this.shapeResize$.disconnect();
    }
    if (this.doughnutChart) {
      this.doughnutChart.dispose();
    }
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    if (this.doughnutShape) {
      this.drawDoughnut();
    } else {
      this.drawDoughnutPending = true;
    }
    this.cd.detectChanges();
  }

  public onDataUpdated() {
    for (const dsData of this.ctx.data) {
      let value = 0;
      const tsValue = dsData.data[0];
      const dataItem = this.dataItems.find(item => item.dataKey === dsData.dataKey);
      if (tsValue && isDefinedAndNotNull(tsValue[1]) && isNumeric(tsValue[1])) {
        value = tsValue[1];
        dataItem.hasValue = true;
        dataItem.value = Number(value);
      } else {
        dataItem.hasValue = false;
        dataItem.value = 0;
      }
    }
    this.updateSeriesData();
    if (this.showLegend) {
      this.cd.detectChanges();
      if (this.legendHorizontal) {
        setTimeout(() => {
          this.onResize();
        });
      }
    }
  }

  private updateSeriesData(renderTotal = true) {
    this.total = 0;
    this.totalText = 'N/A';
    let hasValue = false;
    const seriesData: PieDataItemOption[] = [];
    const enabledDataItems = this.dataItems.filter(item => item.enabled && item.hasValue);
    for (const dataItem of this.dataItems) {
      if (dataItem.enabled && dataItem.hasValue) {
        hasValue = true;
        this.total += dataItem.value;
        seriesData.push(
          {id: dataItem.id, value: dataItem.value, name: dataItem.dataKey.label, itemStyle: {color: dataItem.dataKey.color}}
        );
        if (enabledDataItems.length > 1) {
          seriesData.push({
            value: 0, name: '', itemStyle: {color: 'transparent'}, emphasis: {disabled: true}
          });
        }
      }
      if (this.showLegend) {
        const legendItem = this.legendItems.find(item => item.id === dataItem.id);
        if (dataItem.hasValue) {
          legendItem.hasValue = true;
          legendItem.value = formatValue(dataItem.value, this.decimals, this.units, false);
        } else {
          legendItem.hasValue = false;
          legendItem.value = '--';
        }
      }
    }
    for (let i= 1; i < seriesData.length; i+=2) {
      seriesData[i].value = this.total / 100;
    }
    if (this.showTotal || this.showLegend) {
      if (hasValue) {
        this.totalText = formatValue(this.total, this.decimals, this.units, false);
        if (this.showLegend && !this.showTotal) {
          this.legendItems[this.legendItems.length - 1].hasValue = true;
          this.legendItems[this.legendItems.length - 1].value = this.totalText;
        }
      } else if (this.showLegend && !this.showTotal) {
        this.legendItems[this.legendItems.length - 1].hasValue = false;
        this.legendItems[this.legendItems.length - 1].value = '--';
      }
    }
    this.doughnutOptions.series[0].data = seriesData;
    this.doughnutChart.setOption(this.doughnutOptions);
    if (this.showTotal) {
      this.totalValueColor.update(this.total);
      if (renderTotal) {
        this.renderTotal();
      }
    }
  }

  public onLegendItemEnter(item: DoughnutLegendItem) {
    if (!item.total && item.enabled && item.hasValue) {
      const dataIndex = this.doughnutOptions.series[0].data.findIndex(d => d.id === item.id);
      if (dataIndex > -1) {
        this.doughnutChart.dispatchAction({
          type: 'highlight',
          dataIndex
        });
      }
    }
  }

  public onLegendItemLeave(item: DoughnutLegendItem) {
    if (!item.total && item.enabled && item.hasValue) {
      const dataIndex = this.doughnutOptions.series[0].data.findIndex(d => d.id === item.id);
      if (dataIndex > -1) {
        this.doughnutChart.dispatchAction({
          type: 'downplay',
          dataIndex
        });
      }
    }
  }

  public toggleLegendItem(item: DoughnutLegendItem) {
    if (!item.total && item.hasValue) {
      const enable = !item.enabled;
      const dataItem = this.dataItems.find(d => d.id === item.id);
      if (dataItem) {
        dataItem.enabled = enable;
        this.updateSeriesData();
        item.enabled = enable;
        if (enable) {
          const dataIndex = this.doughnutOptions.series[0].data.findIndex(d => d.id === item.id);
          if (dataIndex > -1) {
            this.doughnutChart.dispatchAction({
              type: 'highlight',
              dataIndex
            });
          }
        }
      }
    }
  }

  private drawDoughnut() {
    echartsModule.init();
    const shapeWidth = this.doughnutShape.nativeElement.getBoundingClientRect().width;
    const shapeHeight = this.doughnutShape.nativeElement.getBoundingClientRect().height;
    const size = this.settings.autoScale ? shapeSize : Math.min(shapeWidth, shapeHeight);
    const innerRadius = size / 2 - shapeSegmentWidth;
    const outerRadius = size / 2;
    this.doughnutChart = echarts.init(this.doughnutShape.nativeElement, null, {
      renderer: 'svg',
      width: this.settings.autoScale ? shapeSize : undefined,
      height: this.settings.autoScale ? shapeSize : undefined,
    });
    this.doughnutOptions = {
      tooltip: {
        trigger: this.settings.showTooltip ? 'item' : 'none',
        confine: false,
        appendTo: 'body',
      },
      series: [
        {
          type: 'pie',
          clockwise: this.settings.clockwise,
          radius: [innerRadius, outerRadius],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: '50%',
            borderWidth: 0,
            borderColor: '#fff'
          },
          label: {
            show: false
          },
          emphasis: {
            scale: false,
            itemStyle: {
              borderColor: '#fff',
              borderWidth: 2,
              shadowColor: 'rgba(0, 0, 0, 0.24)',
              shadowBlur: 8
            },
            label: {
              show: false
            }
          }
        }
      ]
    };
    if (this.settings.showTooltip) {
      this.doughnutOptions.series[0].tooltip = {
        formatter: (params) => {
          if (!params.name) {
            return null;
          }
          let value: string;
          if (this.settings.tooltipValueType === DoughnutTooltipValueType.percentage) {
            const percents = params.value / this.total * 100;
            value = formatValue(percents, this.settings.tooltipValueDecimals, '%', false);
          } else {
            value = formatValue(params.value, this.settings.tooltipValueDecimals, this.units, false);
          }
          const textElement: HTMLElement = this.renderer.createElement('div');
          this.renderer.setStyle(textElement, 'display', 'inline-flex');
          this.renderer.setStyle(textElement, 'align-items', 'center');
          this.renderer.setStyle(textElement, 'gap', '8px');
          const labelElement: HTMLElement = this.renderer.createElement('div');
          this.renderer.appendChild(labelElement, this.renderer.createText(params.name));
          this.renderer.setStyle(labelElement, 'font-family', 'Roboto');
          this.renderer.setStyle(labelElement, 'font-size', '11px');
          this.renderer.setStyle(labelElement, 'font-style', 'normal');
          this.renderer.setStyle(labelElement, 'font-weight', '400');
          this.renderer.setStyle(labelElement, 'line-height', '16px');
          this.renderer.setStyle(labelElement, 'letter-spacing', '0.25px');
          this.renderer.setStyle(labelElement, 'color', 'rgba(0, 0, 0, 0.38)');
          const valueElement: HTMLElement = this.renderer.createElement('div');
          this.renderer.appendChild(valueElement, this.renderer.createText(value));
          this.renderer.setStyle(valueElement, 'font-family', this.settings.tooltipValueFont.family);
          this.renderer.setStyle(valueElement, 'font-size', this.settings.tooltipValueFont.size + this.settings.tooltipValueFont.sizeUnit);
          this.renderer.setStyle(valueElement, 'font-style', this.settings.tooltipValueFont.style);
          this.renderer.setStyle(valueElement, 'font-weight', this.settings.tooltipValueFont.weight);
          this.renderer.setStyle(valueElement, 'line-height', this.settings.tooltipValueFont.lineHeight);
          this.renderer.setStyle(valueElement, 'color', this.settings.tooltipValueColor);
          this.renderer.appendChild(textElement, labelElement);
          this.renderer.appendChild(textElement, valueElement);
          return textElement;
        },
        padding: [4, 8],
        backgroundColor: this.settings.tooltipBackgroundColor,
        extraCssText: `line-height: 1; backdrop-filter: blur(${this.settings.tooltipBackgroundBlur}px);`
      };
      this.doughnutOptions.series[0].tooltip.position = (pos) => [pos[0] + 10, pos[1] + 10];
    }
    this.updateSeriesData(false);

    this.renderer.setStyle(this.doughnutChart.getDom().firstChild, 'overflow', 'visible');
    if (this.settings.autoScale) {
      this.renderer.setStyle(this.doughnutChart.getDom().firstChild, 'position', 'absolute');
    }
    this.renderer.setStyle(this.doughnutChart.getDom().firstChild.firstChild, 'overflow', 'visible');

    this.svgShape = SVG(this.doughnutChart.getDom().firstChild.firstChild).toRoot();

    if (this.showTotal) {
      this.totalTextNode = this.svgShape.text('').font({
        family: 'Roboto',
        leading: 1
      }).attr({'text-anchor': 'middle'});
      this.renderTotal();
    }

    this.shapeResize$ = new ResizeObserver(() => {
      this.onResize();
    });
    this.shapeResize$.observe(this.doughnutContent.nativeElement);
    this.onResize();
  }

  private renderTotal() {
    this.totalTextNode.text(add => {
      add.tspan(this.translate.instant('widgets.doughnut.total')).font({size: '12px', weight: 400}).fill('rgba(0, 0, 0, 0.38)');
      add.tspan('').newLine().font({size: '4px'});
      add.tspan(this.totalText).newLine().font(
        {family: this.settings.totalValueFont.family,
            size: this.settings.totalValueFont.size + this.settings.totalValueFont.sizeUnit,
            weight: this.settings.totalValueFont.weight,
            style: this.settings.totalValueFont.style}
      ).fill(this.totalValueColor.color);
    }).center(this.svgShape.bbox().width / 2, this.svgShape.bbox().height / 2);
  }

  private onResize() {
    if (this.legendHorizontal) {
      this.renderer.setStyle(this.doughnutShape.nativeElement, 'max-width', null);
      this.renderer.setStyle(this.doughnutShape.nativeElement, 'min-width', null);
      this.renderer.setStyle(this.doughnutLegend.nativeElement, 'flex', null);
    }
    const shapeWidth = this.doughnutShape.nativeElement.getBoundingClientRect().width;
    const shapeHeight = this.doughnutShape.nativeElement.getBoundingClientRect().height;
    const size = Math.min(shapeWidth, shapeHeight);
    if (this.legendHorizontal) {
      this.renderer.setStyle(this.doughnutShape.nativeElement, 'max-width', `${size}px`);
      this.renderer.setStyle(this.doughnutShape.nativeElement, 'min-width', `${size}px`);
      this.renderer.setStyle(this.doughnutLegend.nativeElement, 'flex', '1');
    }
    if (!this.settings.autoScale) {
      const innerRadius = size / 2 - shapeSegmentWidth;
      const outerRadius = size / 2;
      this.doughnutOptions.series[0].radius = [innerRadius, outerRadius];
      this.doughnutChart.setOption(this.doughnutOptions);
    } else {
      this.scale = size / shapeSize;
      this.renderer.setStyle(this.doughnutChart.getDom().firstChild, 'transform', `scale(${this.scale})`);
    }
    if (!this.settings.autoScale) {
      this.doughnutChart.resize();
    }
    if (this.showTotal) {
      this.totalTextNode.center((this.settings.autoScale ? shapeSize : shapeWidth) / 2,
        (this.settings.autoScale ? shapeSize : shapeHeight) / 2);
    }
  }

}
