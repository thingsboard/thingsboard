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

import { WidgetContext } from '@home/models/widget-component.models';
import {
  adjustTimeAxisExtentToData,
  calculateThresholdsOffset,
  createTimeSeriesVisualMapOption,
  createTimeSeriesXAxis,
  createTimeSeriesYAxis,
  defaultTimeSeriesChartYAxisSettings,
  generateChartData,
  LineSeriesStepType,
  parseThresholdData,
  TimeSeriesChartAxis,
  TimeSeriesChartDataItem,
  timeSeriesChartDefaultSettings,
  timeSeriesChartKeyDefaultSettings,
  TimeSeriesChartKeySettings,
  TimeSeriesChartNoAggregationBarWidthStrategy,
  TimeSeriesChartSeriesType,
  TimeSeriesChartSettings,
  TimeSeriesChartThreshold,
  timeSeriesChartThresholdDefaultSettings,
  TimeSeriesChartThresholdItem,
  TimeSeriesChartType,
  TimeSeriesChartXAxis,
  TimeSeriesChartYAxis,
  TimeSeriesChartYAxisId,
  TimeSeriesChartYAxisSettings,
  toTimeSeriesChartDataSet,
  updateDarkMode,
  updateXAxisTimeWindow
} from '@home/components/widget/lib/chart/time-series-chart.models';
import {
  calculateAxisSize,
  ECharts,
  echartsModule,
  EChartsOption,
  getAxisExtent,
  getFocusedSeriesIndex,
  measureAxisNameSize
} from '@home/components/widget/lib/chart/echarts-widget.models';
import { DateFormatProcessor, ValueSourceConfig, ValueSourceType } from '@shared/models/widget-settings.models';
import {
  formattedDataFormDatasourceData,
  formatValue,
  isDefined,
  isDefinedAndNotNull,
  isEqual,
  isNumber,
  isString,
  mergeDeep
} from '@core/utils';
import { DataKey, Datasource, DatasourceType, FormattedData, widgetType } from '@shared/models/widget.models';
import * as echarts from 'echarts/core';
import { CallbackDataParams, PiecewiseVisualMapOption } from 'echarts/types/dist/shared';
import { Renderer2 } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { AggregationType } from '@shared/models/time/time.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { WidgetSubscriptionOptions } from '@core/api/widget-api.models';
import { DataKeySettingsFunction } from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { DeepPartial } from '@shared/models/common';
import { BarRenderSharedContext } from '@home/components/widget/lib/chart/time-series-chart-bar.models';
import { TimeSeriesChartStateValueConverter } from '@home/components/widget/lib/chart/time-series-chart-state.models';
import { ChartLabelPosition, ChartShape, toAnimationOption } from '@home/components/widget/lib/chart/chart.models';
import {
  createTooltipValueFormatFunction,
  TimeSeriesChartTooltip,
  TimeSeriesChartTooltipTrigger,
  TimeSeriesChartTooltipValueFormatFunction
} from '@home/components/widget/lib/chart/time-series-chart-tooltip.models';
import { UnitService } from '@core/services/unit.service';
import { isNotEmptyTbUnits, TbUnit } from '@shared/models/unit.models';

export class TbTimeSeriesChart {

  public static dataKeySettings(type = TimeSeriesChartType.default): DataKeySettingsFunction {
    return (_key, isLatestDataKey) => {
      if (!isLatestDataKey) {
        const settings = mergeDeep<TimeSeriesChartKeySettings>({} as TimeSeriesChartKeySettings,
          timeSeriesChartKeyDefaultSettings);
        if (type === TimeSeriesChartType.line) {
          settings.type = TimeSeriesChartSeriesType.line;
        } else if (type === TimeSeriesChartType.bar) {
          settings.type = TimeSeriesChartSeriesType.bar;
        } else if (type === TimeSeriesChartType.point) {
          settings.type = TimeSeriesChartSeriesType.line;
          settings.lineSettings.showLine = false;
          settings.lineSettings.showPoints = true;
          settings.lineSettings.pointShape = ChartShape.circle;
          settings.lineSettings.pointSize = 8;
        } else if (type === TimeSeriesChartType.state) {
          settings.type = TimeSeriesChartSeriesType.line;
          settings.lineSettings.showLine = true;
          settings.lineSettings.step = true;
          settings.lineSettings.stepType = LineSeriesStepType.end;
          settings.lineSettings.pointShape = ChartShape.circle;
          settings.lineSettings.pointSize = 12;
        }
        return settings;
      }
      return null;
    };
  }

  private get noAggregation(): boolean {
    return this.ctx.defaultSubscription.subscriptionTimewindow?.aggregation?.type === AggregationType.NONE;
  }

  private get stateData(): boolean {
    return this.ctx.defaultSubscription.subscriptionTimewindow?.aggregation?.stateData === true;
  }

  private readonly shapeResize$: ResizeObserver;

  private readonly settings: TimeSeriesChartSettings;

  private readonly comparisonEnabled: boolean;
  private readonly stackMode: boolean;

  private xAxisList: TimeSeriesChartXAxis[] = [];
  private yAxisList: TimeSeriesChartYAxis[] = [];
  private dataItems: TimeSeriesChartDataItem[] = [];
  private thresholdItems: TimeSeriesChartThresholdItem[] = [];

  private hasVisualMap = false;
  private visualMapSelectedRanges: {[key: number]: boolean};

  private timeSeriesChart: ECharts;
  private timeSeriesChartOptions: EChartsOption;

  private readonly tooltipDateFormat: DateFormatProcessor;
  private readonly timeSeriesChartTooltip: TimeSeriesChartTooltip;
  private readonly stateValueConverter: TimeSeriesChartStateValueConverter;

  private yMinSubject = new BehaviorSubject(-1);
  private yMaxSubject = new BehaviorSubject(1);

  private darkMode = false;

  private darkModeObserver: MutationObserver;

  private topPointLabels = false;

  private componentIndexCounter = 0;

  private highlightedDataKey: DataKey;

  private barRenderSharedContext: BarRenderSharedContext;

  private latestData: FormattedData[] = [];

  private onParentScroll = this._onParentScroll.bind(this);

  private unitService: UnitService;

  yMin$ = this.yMinSubject.asObservable();
  yMax$ = this.yMaxSubject.asObservable();

  constructor(private ctx: WidgetContext,
              private readonly inputSettings: DeepPartial<TimeSeriesChartSettings>,
              private chartElement: HTMLElement,
              private renderer: Renderer2,
              private autoResize = true) {

    let tooltipValueFormatFunction: TimeSeriesChartTooltipValueFormatFunction;

    this.settings = mergeDeep({} as TimeSeriesChartSettings,
      timeSeriesChartDefaultSettings,
      this.inputSettings as TimeSeriesChartSettings);
    this.comparisonEnabled = !!this.ctx.defaultSubscription.comparisonEnabled;
    this.stackMode = !this.comparisonEnabled && this.settings.stack;
    if (this.settings.states && this.settings.states.length) {
      this.stateValueConverter = new TimeSeriesChartStateValueConverter(this.ctx.utilsService, this.settings.states);
      tooltipValueFormatFunction = this.stateValueConverter.tooltipFormatter;
    }
    const $dashboardPageElement = this.ctx.$containerParent.parents('.tb-dashboard-page');
    const dashboardPageElement = $dashboardPageElement.length ? $($dashboardPageElement[$dashboardPageElement.length-1]) : null;
    this.darkMode = this.settings.darkMode || dashboardPageElement?.hasClass('dark');
    this.unitService = this.ctx.$injector.get(UnitService);
    this.setupXAxes();
    this.setupYAxes();
    this.setupData();
    this.setupThresholds();
    this.setupVisualMap();
    if (this.settings.showTooltip) {
      if (this.settings.tooltipShowDate) {
        this.tooltipDateFormat = DateFormatProcessor.fromSettings(this.ctx.$injector, this.settings.tooltipDateFormat);
      }
      if (!tooltipValueFormatFunction) {
        tooltipValueFormatFunction = createTooltipValueFormatFunction(this.settings.tooltipValueFormatter);
        if (!tooltipValueFormatFunction) {
          tooltipValueFormatFunction = (value, _latestData, units, decimals) => formatValue(value, decimals, units, false);
        }
      }
    }
    this.timeSeriesChartTooltip = new TimeSeriesChartTooltip(
      this.renderer,
      this.ctx.sanitizer,
      this.settings,
      this.tooltipDateFormat,
      tooltipValueFormatFunction,
      this.ctx.translate
    );
    this.onResize();
    if (this.autoResize) {
      this.shapeResize$ = new ResizeObserver(() => {
        this.onResize();
      });
      this.shapeResize$.observe(this.chartElement);
    }
    if (dashboardPageElement) {
      this.darkModeObserver = new MutationObserver(mutations => {
        for (const mutation of mutations) {
          if (mutation.type === 'attributes' && mutation.attributeName === 'class') {
            const darkMode = dashboardPageElement.hasClass('dark');
            this.setDarkMode(darkMode);
          }
        }
      });
      this.darkModeObserver.observe(dashboardPageElement[0], {attributes: true});
    }
  }

  public update(): void {
    for (const item of this.dataItems) {
      const datasourceData = this.ctx.data ? this.ctx.data.find(d => d.dataKey === item.dataKey) : null;
      if (!isEqual(item.dataSet, datasourceData?.data)) {
        item.dataSet = datasourceData?.data;
        item.data = datasourceData?.data ? toTimeSeriesChartDataSet(datasourceData.data, this.stateValueConverter?.valueConverter ?? item.unitConvertor) : [];
      }
    }
    this.onResize();
    if (this.timeSeriesChart) {
      updateXAxisTimeWindow(this.xAxisList[0].option, this.ctx.defaultSubscription.timeWindow);
      if (this.noAggregation) {
        this.timeSeriesChartOptions.tooltip[0].axisPointer.type = 'line';
      } else {
        this.timeSeriesChartOptions.tooltip[0].axisPointer.type = 'shadow';
      }
      if (this.comparisonEnabled) {
        updateXAxisTimeWindow(this.xAxisList[1].option, this.ctx.defaultSubscription.comparisonTimeWindow);
      }
      this.timeSeriesChartOptions.xAxis = this.xAxisList.map(axis => axis.option);
      if (this.hasVisualMap) {
        (this.timeSeriesChartOptions.visualMap as PiecewiseVisualMapOption).selected = this.visualMapSelectedRanges;
      }
      this.barRenderSharedContext.timeInterval = this.ctx.timeWindow.interval;
      this.updateSeriesData(true);
      if (this.highlightedDataKey) {
        this.keyEnter(this.highlightedDataKey);
      }
    }
  }

  public latestUpdated() {
    let update = false;
    if (this.ctx.latestData) {
      this.latestData = formattedDataFormDatasourceData(this.ctx.latestData);
      for (const item of this.dataItems) {
        let latestData = this.latestData.find(data => data.$datasource === item.datasource);
        if (!latestData) {
          latestData = {} as FormattedData;
        }
        item.latestData = latestData;
      }
      for (const item of this.thresholdItems) {
        if (item.settings.type === ValueSourceType.latestKey && item.latestDataKey) {
          const data = this.ctx.latestData.find(d => d.dataKey === item.latestDataKey);
          if (data.data[0]) {
            item.value = parseThresholdData(data.data[0][1], item.unitConvertor);
            update = true;
          }
        }
      }

      for (const yAxis of this.yAxisList) {
        const minType = (yAxis.settings.min as ValueSourceConfig).type;
        const maxType = (yAxis.settings.max as ValueSourceConfig).type;
        if (minType === ValueSourceType.latestKey && yAxis.minLatestDataKey) {
          const data = this.ctx.latestData.find(d => d.dataKey === yAxis.minLatestDataKey);
          if (data?.data[0]) {
            const value = this.parseAxisLimitData(data.data[0][1], yAxis.unitConvertor);
            if (yAxis.option.min !== value) {
              yAxis.option.min = value;
              update = true;
            }
          }
        }

        if (maxType === ValueSourceType.latestKey && yAxis.maxLatestDataKey) {
          const data = this.ctx.latestData.find(d => d.dataKey === yAxis.maxLatestDataKey);
          if (data?.data[0]) {
            const value = this.parseAxisLimitData(data.data[0][1], yAxis.unitConvertor);
            if (yAxis.option.max !== value) {
              yAxis.option.max = value;
              update = true;
            }
          }
        }
      }
    }
    if (this.timeSeriesChart && update) {
      this.updateSeriesData();
      this.updateAxisLimits();
    }
  }

  public keyEnter(dataKey: DataKey): void {
    this.highlightedDataKey = dataKey;
    const item = this.dataItems.find(d => d.dataKey === dataKey);
    if (item) {
      this.timeSeriesChart.dispatchAction({
        type: 'highlight',
        seriesId: item.id
      });
    }
  }

  public keyLeave(dataKey: DataKey): void {
    this.highlightedDataKey = null;
    const item = this.dataItems.find(d => d.dataKey === dataKey);
    if (item) {
      this.timeSeriesChart.dispatchAction({
        type: 'downplay',
        seriesId: item.id
      });
    }
  }

  public toggleKey(dataKey: DataKey, dataIndex?: number): void {
    const enable = dataKey.hidden;
    const dataItem = this.dataItems.find(d => d.dataKey === dataKey);
    if (dataItem) {
      dataItem.enabled = enable;
      if (!enable) {
        this.timeSeriesChart.dispatchAction({
          type: 'downplay',
          seriesId: dataItem.id
        });
      }
      this.updateSeries();
      const mergeList = ['series'];
      if (this.updateYAxisScale(this.yAxisList)) {
        this.timeSeriesChartOptions.yAxis = this.yAxisList.map(axis => axis.option);
        mergeList.push('yAxis');
      }
      this.timeSeriesChart.setOption(this.timeSeriesChartOptions, this.stackMode ? {notMerge: true} : {replaceMerge: mergeList});
      this.updateAxes();
      dataKey.hidden = !enable;
      if (isDefined(dataIndex)) {
        this.ctx.defaultSubscription.updateDataVisibility(dataIndex);
      }
      if (enable) {
        this.timeSeriesChart.dispatchAction({
          type: 'highlight',
          seriesId: dataItem.id
        });
      }
    }
  }

  public toggleVisualMapRange(index: number): void {
    if (this.hasVisualMap) {
      this.visualMapSelectedRanges[index] = !this.visualMapSelectedRanges[index];
      this.timeSeriesChart.dispatchAction({
        type: 'selectDataRange',
        selected: this.visualMapSelectedRanges
      });
    }
  }

  public destroy(): void {
    if (this.shapeResize$) {
      this.shapeResize$.disconnect();
    }
    if (this.timeSeriesChart) {
      this.timeSeriesChart.dispose();
    }
    this.yMinSubject.complete();
    this.yMaxSubject.complete();
    this.darkModeObserver?.disconnect();
    this.ctx.dashboard.gridster.el.removeEventListener('scroll', this.onParentScroll);
  }

  public resize(): void {
    this.onResize();
  }

  public setDarkMode(darkMode: boolean): void {
    if (this.darkMode !== darkMode) {
      this.darkMode = darkMode;
      if (this.timeSeriesChart) {
        this.timeSeriesChartOptions = updateDarkMode(this.timeSeriesChartOptions,
          this.xAxisList, this.yAxisList, this.dataItems, darkMode);
        this.timeSeriesChart.setOption(this.timeSeriesChartOptions);
      }
    }
  }

  public isDarkMode(): boolean {
    return this.darkMode;
  }

  private setupData(): void {
    const noAggregationBarWidthSettings = this.settings.noAggregationBarWidthSettings;
    const targetBarWidth = noAggregationBarWidthSettings.strategy === TimeSeriesChartNoAggregationBarWidthStrategy.group ?
      noAggregationBarWidthSettings.groupWidth : noAggregationBarWidthSettings.barWidth;
    this.barRenderSharedContext = {
      barGap: this.settings.barWidthSettings.barGap,
      intervalGap: this.settings.barWidthSettings.intervalGap,
      timeInterval: this.ctx.timeWindow?.interval,
      noAggregationBarWidthStrategy: noAggregationBarWidthSettings.strategy,
      noAggregationWidthRelative: targetBarWidth.relative,
      noAggregationWidth: targetBarWidth.relative ? targetBarWidth.relativeWidth : targetBarWidth.absoluteWidth
    };
    if (this.ctx.datasources.length) {
      for (const datasource of this.ctx.datasources) {
        const dataKeys = datasource.dataKeys;
        for (const dataKey of dataKeys) {
          const keySettings = mergeDeep<TimeSeriesChartKeySettings>({} as TimeSeriesChartKeySettings,
            timeSeriesChartKeyDefaultSettings, dataKey.settings);
          if ((keySettings.type === TimeSeriesChartSeriesType.line && keySettings.lineSettings.showPointLabel &&
              keySettings.lineSettings.pointLabelPosition === ChartLabelPosition.top) ||
            (keySettings.type === TimeSeriesChartSeriesType.bar &&
              keySettings.barSettings.showLabel &&
              [ChartLabelPosition.top, ChartLabelPosition.bottom]
              .includes(keySettings.barSettings.labelPosition as ChartLabelPosition))) {
            this.topPointLabels = true;
          }
          if (this.stateValueConverter && keySettings.type === TimeSeriesChartSeriesType.line) {
            keySettings.lineSettings.pointLabelFormatter = this.stateValueConverter.labelFormatter;
          }
          dataKey.settings = keySettings;
          const datasourceData = this.ctx.data ? this.ctx.data.find(d => d.dataKey === dataKey) : null;
          const units: TbUnit = isNotEmptyTbUnits(dataKey.units) ? dataKey.units : this.ctx.units;
          const unitSymbol = this.unitService.getTargetUnitSymbol(units);
          const unitConvertor = this.unitService.geUnitConverter(units);
          const data = datasourceData?.data ?
            toTimeSeriesChartDataSet(datasourceData.data, this.stateValueConverter?.valueConverter ?? unitConvertor) : [];
          const decimals = isDefinedAndNotNull(dataKey.decimals) ? dataKey.decimals :
            (isDefinedAndNotNull(this.ctx.decimals) ? this.ctx.decimals : 2);
          let yAxisId = keySettings.yAxisId;
          if (!Object.keys(this.settings.yAxes).includes(yAxisId)) {
            yAxisId = 'default';
          }
          const comparisonItem = this.comparisonEnabled && dataKey.isAdditional;
          const xAxisIndex = comparisonItem ? 1 : 0;
          this.dataItems.push({
            id: this.nextComponentId(),
            units: unitSymbol,
            decimals,
            xAxisIndex,
            yAxisId,
            yAxisIndex: this.getYAxisIndex(yAxisId),
            comparisonItem,
            datasource,
            dataKey,
            data,
            enabled: !keySettings.dataHiddenByDefault,
            tooltipValueFormatFunction: createTooltipValueFormatFunction(keySettings.tooltipValueFormatter),
            unitConvertor
          });
        }
      }
    }
  }

  private setupThresholds(): void {
    const thresholdDatasources: Datasource[] = [];
    for (const thresholdSettings of this.settings.thresholds) {
      const threshold = mergeDeep<TimeSeriesChartThreshold>({} as TimeSeriesChartThreshold,
        timeSeriesChartThresholdDefaultSettings, thresholdSettings);
      if (!this.topPointLabels) {
        if (threshold.showLabel && !threshold.labelPosition.endsWith('Bottom')) {
          this.topPointLabels = true;
        }
      }
      let latestDataKey: DataKey = null;
      let entityDataKey: DataKey = null;
      let value = null;
      const units = isNotEmptyTbUnits(threshold.units) ? threshold.units : this.ctx.units;
      const unitSymbol = this.unitService.getTargetUnitSymbol(units);
      const unitConvertor = this.unitService.geUnitConverter(units);
      if (threshold.type === ValueSourceType.latestKey) {
        if (this.ctx.datasources.length) {
          for (const datasource of this.ctx.datasources) {
            latestDataKey = datasource.latestDataKeys?.find(d =>
              (d.type === DataKeyType.function && d.label === threshold.latestKey) ||
              (d.type !== DataKeyType.function && d.name === threshold.latestKey &&
               d.type === threshold.latestKeyType));
            if (latestDataKey) {
              break;
            }
          }
        }
        if (!latestDataKey) {
          continue;
        }
      } else if (threshold.type === ValueSourceType.entity) {
        const entityAliasId = this.ctx.aliasController.getEntityAliasId(threshold.entityAlias);
        if (!entityAliasId) {
          continue;
        }
        let datasource = thresholdDatasources.find(d => d.entityAliasId === entityAliasId);
        entityDataKey = {
          type: threshold.entityKeyType,
          name: threshold.entityKey,
          label: threshold.entityKey,
          settings: {}
        };
        if (datasource) {
          datasource.dataKeys.push(entityDataKey);
        } else {
          datasource = {
            type: DatasourceType.entity,
            name: threshold.entityAlias,
            aliasName: threshold.entityAlias,
            entityAliasId,
            dataKeys: [entityDataKey]
          };
          thresholdDatasources.push(datasource);
        }
      } else { // constant
        value = unitConvertor ? unitConvertor(threshold.value) : threshold.value;
      }
      const decimals = isDefinedAndNotNull(threshold.decimals) ? threshold.decimals :
        (isDefinedAndNotNull(this.ctx.decimals) ? this.ctx.decimals : 2);
      let yAxisId = threshold.yAxisId;
      if (!Object.keys(this.settings.yAxes).includes(yAxisId)) {
        yAxisId = 'default';
      }
      const thresholdItem: TimeSeriesChartThresholdItem = {
        id: this.nextComponentId(),
        units: unitSymbol,
        decimals,
        yAxisId,
        yAxisIndex: this.getYAxisIndex(yAxisId),
        value,
        latestDataKey,
        settings: threshold,
        unitConvertor
      };
      if (entityDataKey) {
        entityDataKey.settings.thresholdItemId = thresholdItem.id;
      }
      this.thresholdItems.push(thresholdItem);
    }
    this.subscribeForEntityThresholds(thresholdDatasources);
  }

  private setupXAxes(): void {
    const mainXAxis = createTimeSeriesXAxis('main', this.settings.xAxis, this.ctx.defaultSubscription.timeWindow.minTime,
      this.ctx.defaultSubscription.timeWindow.maxTime, this.ctx.date, this.ctx.utilsService, this.darkMode);
    this.xAxisList.push(mainXAxis);
    if (this.comparisonEnabled) {
      const comparisonXAxis = createTimeSeriesXAxis('comparison', this.settings.comparisonXAxis,
        this.ctx.defaultSubscription.comparisonTimeWindow.minTime, this.ctx.defaultSubscription.comparisonTimeWindow.maxTime,
        this.ctx.date, this.ctx.utilsService, this.darkMode);
      this.xAxisList.push(comparisonXAxis);
    }
  }

  private setupYAxes(): void {
    const yAxisSettingsList = Object.values(this.settings.yAxes);
    yAxisSettingsList.sort((a1, a2) => a1.order - a2.order);
    const axisLimitDatasources: Datasource[] = [];
    for (const yAxisSettings of yAxisSettingsList) {
      const axisSettings = mergeDeep<TimeSeriesChartYAxisSettings>({} as TimeSeriesChartYAxisSettings,
        defaultTimeSeriesChartYAxisSettings, yAxisSettings);
      const units = isNotEmptyTbUnits(axisSettings.units) ? axisSettings.units : this.ctx.units;
      const unitSymbol = this.unitService.getTargetUnitSymbol(units);
      const unitConvertor = this.unitService.geUnitConverter(units);
      const decimals = isDefinedAndNotNull(axisSettings.decimals) ? axisSettings.decimals :
        (isDefinedAndNotNull(this.ctx.decimals) ? this.ctx.decimals : 2);
      if (this.stateValueConverter) {
        axisSettings.ticksGenerator = this.stateValueConverter.ticksGenerator;
        axisSettings.ticksFormatter = this.stateValueConverter.ticksFormatter;
      }
      const yAxis = createTimeSeriesYAxis(unitSymbol, decimals, axisSettings, this.ctx.utilsService, this.darkMode, unitConvertor);
      if (isDefinedAndNotNull(axisSettings.min)) {
        this.processYAxisLimit(axisSettings.min as ValueSourceConfig, 'min', yAxis, axisLimitDatasources, unitConvertor);
      }
      if (isDefinedAndNotNull(axisSettings.max)) {
        this.processYAxisLimit(axisSettings.max as ValueSourceConfig, 'max', yAxis, axisLimitDatasources, unitConvertor);
      }
      this.yAxisList.push(yAxis);
    }
    this.subscribeForAxisLimits(axisLimitDatasources);
  }

  private processYAxisLimit(
    limit: ValueSourceConfig,
    limitType: 'min' | 'max',
    yAxis: TimeSeriesChartYAxis,
    axisLimitDatasources: Datasource[],
    unitConvertor?: (value: number) => number
  ): void {
    if (limit && typeof limit === 'object' && 'type' in limit) {
      if (limit.type === ValueSourceType.latestKey) {
        let latestDataKey: DataKey = null;
        if (this.ctx.datasources.length) {
          for (const datasource of this.ctx.datasources) {
            latestDataKey = datasource.latestDataKeys?.find(d =>
              (d.type === DataKeyType.function && d.label === limit.latestKey) ||
              (d.type !== DataKeyType.function && d.name === limit.latestKey &&
                d.type === limit.latestKeyType));
            if (latestDataKey) {
              break;
            }
          }
        }
        if (latestDataKey) {
          if (limitType === 'min') {
            yAxis.minLatestDataKey = latestDataKey;
          } else {
            yAxis.maxLatestDataKey = latestDataKey;
          }
        }
      } else if (limit.type === ValueSourceType.entity) {
        const entityAliasId = this.ctx.aliasController.getEntityAliasId(limit.entityAlias);
        if (entityAliasId) {
          let datasource = axisLimitDatasources.find(d => d.entityAliasId === entityAliasId);
          const entityDataKey: DataKey = {
            type: limit.entityKeyType,
            name: limit.entityKey,
            label: limit.entityKey,
            settings: {
              yAxisId: yAxis.id,
              axisLimit: limitType
            }
          };
          if (datasource) {
            datasource.dataKeys.push(entityDataKey);
          } else {
            datasource = {
              type: DatasourceType.entity,
              name: limit.entityAlias,
              aliasName: limit.entityAlias,
              entityAliasId,
              dataKeys: [entityDataKey]
            };
            axisLimitDatasources.push(datasource);
          }
        }
      } else if (limit.type === ValueSourceType.constant) {
        const value = unitConvertor ? unitConvertor(limit.value) : limit.value;
        if (limitType === 'min') {
          yAxis.option.min = value;
        } else {
          yAxis.option.max = value;
        }
      }
      return;
    }
  }

  private setupVisualMap(): void {
    if (this.settings.visualMapSettings?.pieces && this.settings.visualMapSettings?.pieces.length) {
      this.hasVisualMap = true;
      this.visualMapSelectedRanges = {};
      this.settings.visualMapSettings.pieces.forEach((_val, index) => {
        this.visualMapSelectedRanges[index] = true;
      });
    }
  }

  private nextComponentId(): string {
    return (this.componentIndexCounter++) + '';
  }

  private getYAxisIndex(id: TimeSeriesChartYAxisId): number {
    let yAxisIndex = this.yAxisList.findIndex(axis => axis.id === id);
    if (yAxisIndex === -1) {
      yAxisIndex = this.yAxisList.findIndex(axis => axis.id === 'default');
      if (yAxisIndex === -1 && this.yAxisList.length) {
        yAxisIndex = 0;
      }
    }
    return yAxisIndex;
  }

  private subscribeForEntityThresholds(datasources: Datasource[]) {
    if (datasources.length) {
      const thresholdsSourcesSubscriptionOptions: WidgetSubscriptionOptions = {
        datasources,
        useDashboardTimewindow: false,
        type: widgetType.latest,
        callbacks: {
          onDataUpdated: (subscription) => {
            let update = false;
            if (subscription.data) {
              for (const item of this.thresholdItems) {
                if (item.settings.type === ValueSourceType.entity) {
                  const data = subscription.data.find(d => d.dataKey.settings?.thresholdItemId === item.id);
                  if (data.data[0]) {
                    item.value = parseThresholdData(data.data[0][1], item.unitConvertor);
                    update = true;
                  }
                }
              }
            }
            if (this.timeSeriesChart && update) {
              this.updateSeriesData();
            }
          }
        }
      };
      this.ctx.subscriptionApi.createSubscription(thresholdsSourcesSubscriptionOptions, true).subscribe();
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
              for (const yAxis of this.yAxisList) {
                for (const data of subscription.data) {
                  if (data.dataKey.settings?.yAxisId === yAxis.id) {
                    const limitType = data.dataKey.settings.axisLimit as ('min' | 'max');
                    if (data.data[0]) {
                      const value = this.parseAxisLimitData(data.data[0][1], yAxis.unitConvertor);
                      if (isDefinedAndNotNull(value)) {
                        if (limitType === 'min') {
                          if (yAxis.option.min !== value) {
                            yAxis.option.min = value;
                            update = true;
                          }
                        } else {
                          if (yAxis.option.max !== value) {
                            yAxis.option.max = value;
                            update = true;
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            if (this.timeSeriesChart && update) {
              this.updateAxisLimits();
            }
          }
        }
      };
      this.ctx.subscriptionApi.createSubscription(axisLimitsSubscriptionOptions, true).subscribe();
    }
  }

  private drawChart() {
    echartsModule.init();
    this.renderer.setStyle(this.chartElement, 'letterSpacing', 'normal');
    this.timeSeriesChart = echarts.init(this.chartElement,  null, {
      renderer: 'svg'
    });
    this.ctx.dashboard.gridster.el.addEventListener('scroll', this.onParentScroll);
    this.timeSeriesChartOptions = {
      darkMode: this.darkMode,
      backgroundColor: 'transparent',
      tooltip: [{
        trigger: this.settings.tooltipTrigger === TimeSeriesChartTooltipTrigger.axis ? 'axis' : 'item',
        confine: true,
        appendTo: 'body',
        axisPointer: {
          type: this.noAggregation ? 'line' : 'shadow'
        },
        formatter: (params: CallbackDataParams[]) =>
          this.timeSeriesChartTooltip.formatted(
            params,
            this.settings.tooltipShowFocusedSeries ? getFocusedSeriesIndex(this.timeSeriesChart) : -1,
            this.dataItems,
            this.noAggregation ? null : this.ctx.timeWindow.interval,
          ),
        padding: [8, 12],
        backgroundColor: this.settings.tooltipBackgroundColor,
        borderWidth: 0,
        extraCssText: `line-height: 1; backdrop-filter: blur(${this.settings.tooltipBackgroundBlur}px);`
      }],
      grid: [{
        show: this.settings.grid.show,
        backgroundColor: this.settings.grid.backgroundColor,
        borderWidth: this.settings.grid.borderWidth,
        borderColor: this.settings.grid.borderColor,
        top: this.minTopOffset(),
        left: this.settings.dataZoom ? 5 : 0,
        right: this.settings.dataZoom ? 5 : 0,
        bottom: this.minBottomOffset()
      }],
      xAxis: this.xAxisList.map(axis => axis.option),
      yAxis: this.yAxisList.map(axis => axis.option),
      dataZoom: [
        {
          type: 'inside',
          disabled: !this.settings.dataZoom,
          realtime: true,
          filterMode: this.stateData ? 'none' : 'weakFilter'
        },
        {
          type: 'slider',
          show: this.settings.dataZoom,
          showDetail: false,
          realtime: true,
          filterMode: this.stateData ? 'none' : 'weakFilter',
          bottom: 10
        }
      ],
      ...toAnimationOption(this.ctx, this.settings.animation)
    };
    if (this.hasVisualMap) {
      this.timeSeriesChartOptions.visualMap =
        createTimeSeriesVisualMapOption(this.settings.visualMapSettings, this.visualMapSelectedRanges);
    }

    this.timeSeriesChartOptions.xAxis[0].tbTimeWindow = this.ctx.defaultSubscription.timeWindow;

    this.updateSeries();
    if (this.updateYAxisScale(this.yAxisList)) {
      this.timeSeriesChartOptions.yAxis = this.yAxisList.map(axis => axis.option);
    }

    this.timeSeriesChart.setOption(this.timeSeriesChartOptions);
    this.updateAxes(false);

    if (this.settings.dataZoom) {
      this.timeSeriesChart.on('datazoom', () => {
        this.updateAxes();
      });
    }
  }

  private updateSeriesData(updateScale = false): void {
    if (!this.timeSeriesChart.isDisposed()) {
      this.updateSeries();
      if (updateScale && this.updateYAxisScale(this.yAxisList)) {
        this.timeSeriesChartOptions.yAxis = this.yAxisList.map(axis => axis.option);
      }
      this.timeSeriesChart.setOption(this.timeSeriesChartOptions);
      this.updateAxes();
    }
  }

  private parseAxisLimitData = (data: any, unitConvertor?: (value: number) => number): number => {
    let value: number;
    if (isDefinedAndNotNull(data)) {
      if (isNumber(data)) {
        value = data;
      } else if (isString(data)) {
        value = Number(data);
      }
    }
    if (isDefinedAndNotNull(value) && !isNaN(value)) {
      if (unitConvertor) {
        return unitConvertor(value);
      }
      return value;
    }
    return null;
  };

  private updateSeries(): void {
    this.timeSeriesChartOptions.series = generateChartData(this.dataItems, this.thresholdItems,
      this.stackMode,
      this.noAggregation,
      this.barRenderSharedContext, this.darkMode);
    if (this.stateData && !this.comparisonEnabled) {
      adjustTimeAxisExtentToData(this.timeSeriesChartOptions.xAxis[0], this.dataItems,
        this.ctx.defaultSubscription.timeWindow.minTime,
        this.ctx.defaultSubscription.timeWindow.maxTime);
    }
  }

  private updateAxes(lazy = true) {
    const leftAxisList = this.yAxisList.filter(axis => axis.option.position === 'left');
    let res = this.updateAxisOffset(leftAxisList);
    let leftOffset = res.offset + (!res.offset && this.settings.dataZoom ? 5 : 0);
    let changed = res.changed;
    const rightAxisList = this.yAxisList.filter(axis => axis.option.position === 'right');
    res = this.updateAxisOffset(rightAxisList);
    let rightOffset = res.offset + (!res.offset && this.settings.dataZoom ? 5 : 0);
    changed = changed || res.changed;

    let bottomOffset = this.minBottomOffset();
    const minTopOffset = this.minTopOffset();

    const topAxisList = this.xAxisList.filter(axis => axis.option.position === 'top');
    res = this.updateAxisOffset(topAxisList);
    const topOffset = Math.max(res.offset, minTopOffset);
    changed = changed || res.changed;

    const bottomAxisList = this.xAxisList.filter(axis => axis.option.position === 'bottom');
    res = this.updateAxisOffset(bottomAxisList);
    bottomOffset += res.offset;
    changed = changed || res.changed;

    const thresholdsOffset = calculateThresholdsOffset(this.timeSeriesChart, this.thresholdItems, this.yAxisList);
    leftOffset = Math.max(leftOffset, thresholdsOffset[0]);
    rightOffset = Math.max(rightOffset, thresholdsOffset[1]);

    if (this.timeSeriesChartOptions.grid[0].left !== leftOffset ||
      this.timeSeriesChartOptions.grid[0].right !== rightOffset  ||
      this.timeSeriesChartOptions.grid[0].bottom !== bottomOffset ||
      this.timeSeriesChartOptions.grid[0].top !== topOffset) {
      this.timeSeriesChartOptions.grid[0].left = leftOffset;
      this.timeSeriesChartOptions.grid[0].right = rightOffset;
      this.timeSeriesChartOptions.grid[0].bottom = bottomOffset;
      this.timeSeriesChartOptions.grid[0].top = topOffset;
      changed = true;
    }
    if (changed) {
      this.timeSeriesChartOptions.yAxis = this.yAxisList.map(axis => axis.option);
      this.timeSeriesChartOptions.xAxis = this.xAxisList.map(axis => axis.option);
      this.timeSeriesChart.setOption(this.timeSeriesChartOptions, {replaceMerge: ['yAxis', 'xAxis', 'grid'], lazyUpdate: lazy});
    }
    if (this.yAxisList.length) {
      const extent = getAxisExtent(this.timeSeriesChart, this.yAxisList[0].id);
      const min = extent[0];
      const max = extent[1];
      if (this.yMinSubject.value !== min) {
        this.yMinSubject.next(min);
      }
      if (this.yMaxSubject.value !== max) {
        this.yMaxSubject.next(max);
      }
    }
  }

  private updateAxisOffset(axisList: TimeSeriesChartAxis[]): {offset: number; changed: boolean} {
    const result = {offset: 0, changed: false};
    let size = 0;
    for (const axis of axisList) {
      const newSize = calculateAxisSize(this.timeSeriesChart, axis.option.mainType, axis.id);
      if (size && newSize) {
        result.offset += 5;
      }
      size = newSize;
      const showLine = !!size && axis.settings.showLine;
      if (axis.option.axisLine.show !== showLine) {
        axis.option.axisLine.show = showLine;
        result.changed = true;
      }
      if (axis.option.offset !== result.offset) {
        axis.option.offset = result.offset;
        result.changed = true;
      }
      if (axis.settings.label) {
        if (!size) {
          if (axis.option.name) {
            axis.option.name = null;
            result.changed = true;
          }
        } else {
          if (!axis.option.name) {
            axis.option.name = this.ctx.utilsService.customTranslation(axis.settings.label, axis.settings.label);
            result.changed = true;
          }
          const nameGap = size;
          if (axis.option.nameGap !== nameGap) {
            axis.option.nameGap = nameGap;
            result.changed = true;
          }
          const nameSize = measureAxisNameSize(this.timeSeriesChart, axis.option.mainType, axis.id, axis.settings.label);
          result.offset += nameSize;
        }
      }
      result.offset += size;
    }
    return result;
  }

  private updateYAxisScale(axisList: TimeSeriesChartYAxis[]): boolean {
    let changed = false;
    for (const yAxis of axisList) {
      const scaleYAxis = this.scaleYAxis(yAxis);
      if (yAxis.option.scale !== scaleYAxis) {
        yAxis.option.scale = scaleYAxis;
        changed = true;
      }
    }
    return changed;
  }

  private updateAxisLimits(): void {
    if (this.timeSeriesChart && !this.timeSeriesChart.isDisposed()) {
      this.timeSeriesChartOptions.yAxis = this.yAxisList.map(axis => axis.option);
      this.timeSeriesChart.setOption(this.timeSeriesChartOptions, {
        replaceMerge: ['yAxis']
      });
      this.updateAxes();
    }
  }

  private scaleYAxis(yAxis: TimeSeriesChartYAxis): boolean {
    if (!this.stateData) {
      const axisBarDataItems = this.dataItems.filter(d => d.yAxisId === yAxis.id && d.enabled &&
        d.data.length && d.dataKey.settings.type === TimeSeriesChartSeriesType.bar);
      return !axisBarDataItems.length;
    } else {
      return false;
    }
  }

  private minTopOffset(): number {
    const showTickLabels =
      !!this.yAxisList.find(yAxis => yAxis.settings.show && yAxis.settings.showTickLabels);
    return (this.topPointLabels) ? 25 :
      (showTickLabels ? 10 : 5);
  }

  private minBottomOffset(): number {
    return this.settings.dataZoom ? 45 : 5;
  }

  private _onParentScroll() {
    if (this.timeSeriesChart) {
      this.timeSeriesChart.dispatchAction({
        type: 'hideTip'
      });
    }
  }

  private onResize() {
    const shapeWidth = this.chartElement.offsetWidth;
    const shapeHeight = this.chartElement.offsetHeight;
    if (shapeWidth && shapeHeight) {
      if (!this.timeSeriesChart) {
        this.drawChart();
      } else {
        const width = this.timeSeriesChart.getWidth();
        const height = this.timeSeriesChart.getHeight();
        if (width !== shapeWidth || height !== shapeHeight) {
          let barItems: TimeSeriesChartDataItem[];
          if (this.animationEnabled()) {
            barItems =
              this.dataItems.filter(d => d.enabled && d.data.length &&
                d.dataKey.settings.type === TimeSeriesChartSeriesType.bar);
            this.updateBarsAnimation(barItems, false);
          }
          this.timeSeriesChart.resize();
          if (this.animationEnabled()) {
            this.updateBarsAnimation(barItems, true);
          }
        }
      }
    }
  }

  private animationEnabled(): boolean {
    return this.settings.animation.animation;
  }

  private updateBarsAnimation(barItems: TimeSeriesChartDataItem[], animation: boolean) {
    if (barItems.length) {
      barItems.forEach(item => {
        item.option.animation = animation;
      });
      this.timeSeriesChart.setOption(this.timeSeriesChartOptions);
    }
  }
}
