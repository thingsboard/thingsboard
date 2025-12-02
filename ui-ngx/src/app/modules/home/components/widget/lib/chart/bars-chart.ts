///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { TbLatestChart } from '@home/components/widget/lib/chart/latest-chart';
import { barsChartDefaultSettings, BarsChartSettings } from '@home/components/widget/lib/chart/bars-chart.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { DeepPartial } from '@shared/models/common';
import { PieChartSettings } from '@home/components/widget/lib/chart/pie-chart.models';
import { Renderer2 } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { ComponentStyle } from '@shared/models/widget-settings.models';
import { LinearGradientObject } from 'zrender/lib/graphic/LinearGradient';
import tinycolor from 'tinycolor2';
import { BarDataItemOption, BarSeriesLabelOption } from 'echarts/types/src/chart/bar/BarSeries';
import { isDefinedAndNotNull, isNumber, isString } from '@core/utils';
import {
  ChartFillType,
  ChartLabelPosition,
  createChartTextStyle,
  createLinearOpacityGradient,
  toAnimationOption
} from '@home/components/widget/lib/chart/chart.models';
import { ValueAxisBaseOption } from 'echarts/types/src/coord/axisCommonTypes';
import { RadiusAxisOption, YAXisOption } from 'echarts/types/dist/shared';
import { DataKey, Datasource, DatasourceType, widgetType } from '@shared/models/widget.models';
import { WidgetSubscriptionOptions } from '@core/api/widget-api.models';
import { ValueSourceType } from '@shared/models/widget-settings.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { AxisLimitConfig } from '@home/components/widget/lib/chart/time-series-chart.models';

export class TbBarsChart extends TbLatestChart<BarsChartSettings> {

  private dynamicAxisMin: number | string = null;
  private dynamicAxisMax: number | string = null;
  private minLatestDataKey: DataKey = null;
  private maxLatestDataKey: DataKey = null;

  constructor(ctx: WidgetContext,
              inputSettings: DeepPartial<PieChartSettings>,
              chartElement: HTMLElement,
              renderer: Renderer2,
              translate: TranslateService,
              autoResize = true) {

    super(ctx, inputSettings, chartElement, renderer, translate, autoResize);
  }

  protected defaultSettings(): BarsChartSettings {
    return barsChartDefaultSettings;
  }

  protected initSettings() {
    super.initSettings();
    this.setupAxisLimits();
  }

  private setupAxisLimits(): void {
    const axisLimitDatasources: Datasource[] = [];

    if (isDefinedAndNotNull(this.settings.axisMin)) {
      this.processAxisLimit(this.settings.axisMin, 'min', axisLimitDatasources);
    }
    if (isDefinedAndNotNull(this.settings.axisMax)) {
      this.processAxisLimit(this.settings.axisMax, 'max', axisLimitDatasources);
    }

    this.subscribeForAxisLimits(axisLimitDatasources);
  }

  private processAxisLimit(
    limit: any,
    limitType: 'min' | 'max',
    axisLimitDatasources: Datasource[]
  ): void {
    if (limit && typeof limit === 'object' && 'type' in limit) {
      const axisLimit = limit as AxisLimitConfig;

      if (axisLimit.type === ValueSourceType.latestKey) {
        let latestDataKey: DataKey = null;
        if (this.ctx.datasources.length) {
          for (const datasource of this.ctx.datasources) {
            latestDataKey = datasource.latestDataKeys?.find(d =>
              (d.type === DataKeyType.function && d.label === (axisLimit.value as DataKey).label) ||
              (d.type !== DataKeyType.function && d.name === (axisLimit.value as DataKey).name &&
                d.type === (axisLimit.value as DataKey).type));
            if (latestDataKey) {
              break;
            }
          }
        }

        if (latestDataKey) {
          if (limitType === 'min') {
            this.minLatestDataKey = latestDataKey;
          } else {
            this.maxLatestDataKey = latestDataKey;
          }
        }
      } else if (axisLimit.type === ValueSourceType.entity) {
        const entityAliasId = this.ctx.aliasController.getEntityAliasId(axisLimit.entityAlias);
        if (entityAliasId) {
          let datasource = axisLimitDatasources.find(d => d.entityAliasId === entityAliasId);
          const entityDataKey: DataKey = {
            type: (axisLimit.value as DataKey).type,
            name: (axisLimit.value as DataKey).name,
            label: (axisLimit.value as DataKey).name,
            settings: {
              axisLimit: limitType
            }
          };

          if (datasource) {
            datasource.dataKeys.push(entityDataKey);
          } else {
            datasource = {
              type: DatasourceType.entity,
              name: axisLimit.entityAlias,
              aliasName: axisLimit.entityAlias,
              entityAliasId,
              dataKeys: [entityDataKey]
            };
            axisLimitDatasources.push(datasource);
          }
        }
      } else if (axisLimit.type === ValueSourceType.constant) {
        const value = axisLimit.value as number;
        if (limitType === 'min') {
          this.dynamicAxisMin = value;
        } else {
          this.dynamicAxisMax = value;
        }
      }
    } else if (typeof limit === 'number' || typeof limit === 'string') {
      if (limitType === 'min') {
        this.dynamicAxisMin = limit;
      } else {
        this.dynamicAxisMax = limit;
      }
    }
  }

  private subscribeForAxisLimits(datasources: Datasource[]) {
    if (datasources.length) {
      const axisLimitsSubscriptionOptions: WidgetSubscriptionOptions = {
        datasources,
        useDashboardTimewindow: false,
        type: widgetType.latest,
        callbacks: {
          onDataUpdated: (subscription) => {
            let update = false;
            if (subscription.data) {
              for (const data of subscription.data) {
                const limitType = data.dataKey.settings.axisLimit as ('min' | 'max');
                if (data.data[0]) {
                  const value = this.parseAxisLimitData(data.data[0][1]);
                  if (limitType === 'min') {
                    if (this.dynamicAxisMin !== value) {
                      this.dynamicAxisMin = value;
                      update = true;
                    }
                  } else {
                    if (this.dynamicAxisMax !== value) {
                      this.dynamicAxisMax = value;
                      update = true;
                    }
                  }
                }
              }
            }
            if (this.latestChart && update) {
              this.updateAxisLimits();
            }
          }
        }
      };
      this.ctx.subscriptionApi.createSubscription(axisLimitsSubscriptionOptions, true).subscribe();
    }
  }

  private parseAxisLimitData(data: any): number | null {
    let value: number;
    if (isDefinedAndNotNull(data)) {
      if (isNumber(data)) {
        value = data;
      } else if (isString(data)) {
        value = Number(data);
      }
    }
    if (isDefinedAndNotNull(value) && !isNaN(value)) {
      return value;
    }
    return null;
  }

  private updateAxisLimitsFromLatest(): boolean {
    let update = false;

    if (this.ctx.latestData) {
      if (this.minLatestDataKey) {
        const data = this.ctx.latestData.find(d => d.dataKey === this.minLatestDataKey);
        if (data?.data[0]) {
          const value = this.parseAxisLimitData(data.data[0][1]);
          if (this.dynamicAxisMin !== value) {
            this.dynamicAxisMin = value;
            update = true;
          }
        }
      }

      if (this.maxLatestDataKey) {
        const data = this.ctx.latestData.find(d => d.dataKey === this.maxLatestDataKey);
        if (data?.data[0]) {
          const value = this.parseAxisLimitData(data.data[0][1]);
          if (this.dynamicAxisMax !== value) {
            this.dynamicAxisMax = value;
            update = true;
          }
        }
      }
    }
    return update;
  }

  private updateAxisLimits(): void {
    if (this.latestChart && !this.latestChart.isDisposed()) {
      const axisTickLabelStyle = createChartTextStyle(this.settings.axisTickLabelFont,
        this.settings.axisTickLabelColor, false, 'axis.tickLabel');
      const valueAxis: ValueAxisBaseOption = {
        type: 'value',
        min: this.dynamicAxisMin,
        max: this.dynamicAxisMax,
        axisLabel: {
          color: axisTickLabelStyle.color,
          fontStyle: axisTickLabelStyle.fontStyle,
          fontWeight: axisTickLabelStyle.fontWeight,
          fontFamily: axisTickLabelStyle.fontFamily,
          fontSize: axisTickLabelStyle.fontSize,
          formatter: (value: any) => this.valueFormatter.format(value)
        }
      };

      if (this.settings.polar) {
        this.latestChartOption.radiusAxis = valueAxis as RadiusAxisOption;
      } else {
        this.latestChartOption.yAxis = valueAxis as YAXisOption;
      }

      this.latestChart.setOption(this.latestChartOption);
    }
  }

  public latestUpdated() {
    if (this.updateAxisLimitsFromLatest()) {
      this.updateAxisLimits();
    }
  }

  protected prepareLatestChartOption() {
    let labelStyle: ComponentStyle = {};
    if (this.settings.barSettings.showLabel) {
      labelStyle = createChartTextStyle(this.settings.barSettings.labelFont,
        this.settings.barSettings.labelColor, false, 'series.label', false);
    }
    const labelOption: BarSeriesLabelOption = {
      show: this.settings.barSettings.showLabel,
      position: this.settings.barSettings.labelPosition,
      formatter: (params) => `{label|${params.name}}`,
      rich: {
        label: labelStyle
      }
    };
    if (this.settings.barSettings.enableLabelBackground) {
      labelOption.backgroundColor = this.settings.barSettings.labelBackground;
      labelOption.padding = [4, 5];
      labelOption.borderRadius = 4;
    }
    this.latestChartOption.series = [
      {
        type: 'bar',
        barWidth: isDefinedAndNotNull(this.settings.barSettings.barWidth) ? this.settings.barSettings.barWidth + '%' : undefined,
        itemStyle: {
          borderWidth: this.settings.barSettings.showBorder ? this.settings.barSettings.borderWidth : 0
        },
        emphasis: {
          focus: 'self'
        },
        coordinateSystem: this.settings.polar ? 'polar' : 'cartesian2d',
        label: labelOption,
        ...toAnimationOption(this.ctx, this.settings.animation)
      }
    ];

    const axisTickLabelStyle = createChartTextStyle(this.settings.axisTickLabelFont,
      this.settings.axisTickLabelColor, false, 'axis.tickLabel');
    const minValue = isDefinedAndNotNull(this.dynamicAxisMin) ? this.dynamicAxisMin : undefined;
    const maxValue = isDefinedAndNotNull(this.dynamicAxisMax) ? this.dynamicAxisMax : undefined;
    const valueAxis: ValueAxisBaseOption = {
      type: 'value',
      min: minValue,
      max: maxValue,
      axisLabel: {
        color: axisTickLabelStyle.color,
        fontStyle: axisTickLabelStyle.fontStyle,
        fontWeight: axisTickLabelStyle.fontWeight,
        fontFamily: axisTickLabelStyle.fontFamily,
        fontSize: axisTickLabelStyle.fontSize,
        formatter: (value: any) => this.valueFormatter.format(value)
      }
    };

    if (this.settings.polar) {
      this.latestChartOption.polar = {
        radius: '100%'
      };
      this.latestChartOption.radiusAxis = valueAxis as RadiusAxisOption;
      this.latestChartOption.angleAxis = {
        type: 'category',
        data: [],
        startAngle: this.settings.angleAxisStartAngle
      };
    } else {
      let minTop = 0;
      let minBottom = 0;
      if (this.settings.barSettings.showLabel) {
        if (this.settings.barSettings.labelPosition === ChartLabelPosition.top) {
          minTop = this.settings.barSettings.labelFont.size;
        } else if (this.settings.barSettings.labelPosition === ChartLabelPosition.bottom) {
          minBottom = this.settings.barSettings.labelFont.size;
        }
      }
      this.latestChartOption.grid = [{
        containLabel: true,
        top: minTop,
        bottom: minBottom,
        left: 0,
        right: 0
      }];
      this.latestChartOption.xAxis = {
        type: 'category',
        data: []
      };
      this.latestChartOption.yAxis = valueAxis as YAXisOption;
    }
  }

  protected doUpdateSeriesData() {
    const seriesData: BarDataItemOption[] = [];
    for (const dataItem of this.dataItems) {
      if (dataItem.enabled && dataItem.hasValue) {
        const barSettings = this.settings.barSettings;
        let borderRadius: number[];
        if (dataItem.value < 0) {
          borderRadius = [0, 0, barSettings.borderRadius, barSettings.borderRadius];
        } else {
          borderRadius = [barSettings.borderRadius, barSettings.borderRadius, 0, 0];
        }
        let barColor: string | LinearGradientObject;
        if (barSettings.backgroundSettings.type === ChartFillType.none) {
          barColor = dataItem.dataKey.color;
        } else if (barSettings.backgroundSettings.type === ChartFillType.opacity) {
          barColor = tinycolor(dataItem.dataKey.color).setAlpha(barSettings.backgroundSettings.opacity).toRgbString();
        } else {
          barColor = createLinearOpacityGradient(dataItem.dataKey.color, barSettings.backgroundSettings.gradient);
        }
        seriesData.push(
          {id: dataItem.id, value: dataItem.value, name: dataItem.dataKey.label,
            itemStyle: {color: barColor, borderColor: dataItem.dataKey.color, borderRadius}}
        );
      }
    }
    this.latestChartOption.series[0].data = seriesData;
  }
}
