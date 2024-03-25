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

import { Component, Injector } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import {
  DataKey,
  Datasource,
  legendPositions,
  legendPositionTranslationMap,
  WidgetConfig,
} from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';
import { formatValue, isDefinedAndNotNull, isUndefined, mergeDeep } from '@core/utils';
import {
  cssSizeToStrSize,
  DateFormatProcessor,
  DateFormatSettings,
  resolveCssSize
} from '@shared/models/widget-settings.models';
import {
  timeSeriesChartWidgetDefaultSettings,
  TimeSeriesChartWidgetSettings
} from '@home/components/widget/lib/chart/time-series-chart-widget.models';
import { EChartsTooltipTrigger } from '@home/components/widget/lib/chart/echarts-widget.models';
import {
  TimeSeriesChartKeySettings, TimeSeriesChartThreshold,
  TimeSeriesChartType, TimeSeriesChartYAxes,
  TimeSeriesChartYAxisId
} from '@home/components/widget/lib/chart/time-series-chart.models';

@Component({
  selector: 'tb-time-series-chart-basic-config',
  templateUrl: './time-series-chart-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class TimeSeriesChartBasicConfigComponent extends BasicWidgetConfigComponent {

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.timeSeriesChartWidgetConfigForm.get('datasources').value;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  public get yAxisIds(): TimeSeriesChartYAxisId[] {
    const yAxes: TimeSeriesChartYAxes = this.timeSeriesChartWidgetConfigForm.get('yAxes').value;
    return Object.keys(yAxes);
  }

  TimeSeriesChartType = TimeSeriesChartType;

  EChartsTooltipTrigger = EChartsTooltipTrigger;

  legendPositions = legendPositions;

  legendPositionTranslationMap = legendPositionTranslationMap;

  timeSeriesChartWidgetConfigForm: UntypedFormGroup;

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  tooltipDatePreviewFn = this._tooltipDatePreviewFn.bind(this);

  chartType: TimeSeriesChartType = TimeSeriesChartType.default;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  public yAxisRemoved(yAxisId: TimeSeriesChartYAxisId): void {
    if (this.widgetConfig.config.datasources && this.widgetConfig.config.datasources.length > 1) {
      for (let i = 1; i < this.widgetConfig.config.datasources.length; i++) {
        const datasource = this.widgetConfig.config.datasources[i];
        this.removeYaxisId(datasource.dataKeys, yAxisId);
      }
    }
  }

  protected configForm(): UntypedFormGroup {
    return this.timeSeriesChartWidgetConfigForm;
  }

  protected setupConfig(widgetConfig: WidgetConfigComponentData) {
    const params = widgetConfig.typeParameters as any;
    if (isDefinedAndNotNull(params.chartType)) {
      this.chartType = params.chartType;
    }
    super.setupConfig(widgetConfig);
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{ name: 'temperature', label: 'Temperature', type: DataKeyType.timeseries, units: '°C', decimals: 0 }];
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: TimeSeriesChartWidgetSettings = mergeDeep<TimeSeriesChartWidgetSettings>({} as TimeSeriesChartWidgetSettings,
      timeSeriesChartWidgetDefaultSettings, configData.config.settings as TimeSeriesChartWidgetSettings);
    const iconSize = resolveCssSize(configData.config.iconSize);
    this.timeSeriesChartWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      datasources: [configData.config.datasources, []],

      yAxes: [settings.yAxes, []],
      series: [this.getSeries(configData.config.datasources), []],
      thresholds: [settings.thresholds, []],

      showTitle: [configData.config.showTitle, []],
      title: [configData.config.title, []],
      titleFont: [configData.config.titleFont, []],
      titleColor: [configData.config.titleColor, []],

      showIcon: [configData.config.showTitleIcon, []],
      iconSize: [iconSize[0], [Validators.min(0)]],
      iconSizeUnit: [iconSize[1], []],
      icon: [configData.config.titleIcon, []],
      iconColor: [configData.config.iconColor, []],

      dataZoom: [settings.dataZoom, []],
      stack: [settings.stack, []],

      xAxis: [settings.xAxis, []],

      noAggregationBarWidthSettings: [settings.noAggregationBarWidthSettings, []],

      showLegend: [settings.showLegend, []],
      legendLabelFont: [settings.legendLabelFont, []],
      legendLabelColor: [settings.legendLabelColor, []],
      legendConfig: [settings.legendConfig, []],

      showTooltip: [settings.showTooltip, []],
      tooltipTrigger: [settings.tooltipTrigger, []],
      tooltipValueFont: [settings.tooltipValueFont, []],
      tooltipValueColor: [settings.tooltipValueColor, []],
      tooltipShowDate: [settings.tooltipShowDate, []],
      tooltipDateFormat: [settings.tooltipDateFormat, []],
      tooltipDateFont: [settings.tooltipDateFont, []],
      tooltipDateColor: [settings.tooltipDateColor, []],
      tooltipDateInterval: [settings.tooltipDateInterval, []],

      tooltipBackgroundColor: [settings.tooltipBackgroundColor, []],
      tooltipBackgroundBlur: [settings.tooltipBackgroundBlur, []],

      animation: [settings.animation, []],

      background: [settings.background, []],

      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],
      padding: [settings.padding, []],

      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    setTimewindowConfig(this.widgetConfig.config, config.timewindowConfig);
    this.widgetConfig.config.datasources = config.datasources;
    this.setSeries(config.series, this.widgetConfig.config.datasources);

    this.widgetConfig.config.showTitle = config.showTitle;
    this.widgetConfig.config.title = config.title;
    this.widgetConfig.config.titleFont = config.titleFont;
    this.widgetConfig.config.titleColor = config.titleColor;

    this.widgetConfig.config.showTitleIcon = config.showIcon;
    this.widgetConfig.config.iconSize = cssSizeToStrSize(config.iconSize, config.iconSizeUnit);
    this.widgetConfig.config.titleIcon = config.icon;
    this.widgetConfig.config.iconColor = config.iconColor;

    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};

    this.widgetConfig.config.settings.thresholds = config.thresholds;

    this.widgetConfig.config.settings.dataZoom = config.dataZoom;
    this.widgetConfig.config.settings.stack = config.stack;

    this.widgetConfig.config.settings.yAxes = config.yAxes;
    this.widgetConfig.config.settings.xAxis = config.xAxis;

    this.widgetConfig.config.settings.noAggregationBarWidthSettings = config.noAggregationBarWidthSettings;

    this.widgetConfig.config.settings.showLegend = config.showLegend;
    this.widgetConfig.config.settings.legendLabelFont = config.legendLabelFont;
    this.widgetConfig.config.settings.legendLabelColor = config.legendLabelColor;
    this.widgetConfig.config.settings.legendConfig = config.legendConfig;

    this.widgetConfig.config.settings.showTooltip = config.showTooltip;
    this.widgetConfig.config.settings.tooltipTrigger = config.tooltipTrigger;
    this.widgetConfig.config.settings.tooltipValueFont = config.tooltipValueFont;
    this.widgetConfig.config.settings.tooltipValueColor = config.tooltipValueColor;
    this.widgetConfig.config.settings.tooltipShowDate = config.tooltipShowDate;
    this.widgetConfig.config.settings.tooltipDateFormat = config.tooltipDateFormat;
    this.widgetConfig.config.settings.tooltipDateFont = config.tooltipDateFont;
    this.widgetConfig.config.settings.tooltipDateColor = config.tooltipDateColor;
    this.widgetConfig.config.settings.tooltipDateInterval = config.tooltipDateInterval;
    this.widgetConfig.config.settings.tooltipBackgroundColor = config.tooltipBackgroundColor;
    this.widgetConfig.config.settings.tooltipBackgroundBlur = config.tooltipBackgroundBlur;

    this.widgetConfig.config.settings.animation = config.animation;

    this.widgetConfig.config.settings.background = config.background;

    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;
    this.widgetConfig.config.settings.padding = config.padding;

    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showTitle', 'showIcon', 'showLegend', 'showTooltip', 'tooltipShowDate'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showTitle: boolean = this.timeSeriesChartWidgetConfigForm.get('showTitle').value;
    const showIcon: boolean = this.timeSeriesChartWidgetConfigForm.get('showIcon').value;
    const showLegend: boolean = this.timeSeriesChartWidgetConfigForm.get('showLegend').value;
    const showTooltip: boolean = this.timeSeriesChartWidgetConfigForm.get('showTooltip').value;
    const tooltipShowDate: boolean = this.timeSeriesChartWidgetConfigForm.get('tooltipShowDate').value;

    if (showTitle) {
      this.timeSeriesChartWidgetConfigForm.get('title').enable();
      this.timeSeriesChartWidgetConfigForm.get('titleFont').enable();
      this.timeSeriesChartWidgetConfigForm.get('titleColor').enable();
      this.timeSeriesChartWidgetConfigForm.get('showIcon').enable({emitEvent: false});
      if (showIcon) {
        this.timeSeriesChartWidgetConfigForm.get('iconSize').enable();
        this.timeSeriesChartWidgetConfigForm.get('iconSizeUnit').enable();
        this.timeSeriesChartWidgetConfigForm.get('icon').enable();
        this.timeSeriesChartWidgetConfigForm.get('iconColor').enable();
      } else {
        this.timeSeriesChartWidgetConfigForm.get('iconSize').disable();
        this.timeSeriesChartWidgetConfigForm.get('iconSizeUnit').disable();
        this.timeSeriesChartWidgetConfigForm.get('icon').disable();
        this.timeSeriesChartWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.timeSeriesChartWidgetConfigForm.get('title').disable();
      this.timeSeriesChartWidgetConfigForm.get('titleFont').disable();
      this.timeSeriesChartWidgetConfigForm.get('titleColor').disable();
      this.timeSeriesChartWidgetConfigForm.get('showIcon').disable({emitEvent: false});
      this.timeSeriesChartWidgetConfigForm.get('iconSize').disable();
      this.timeSeriesChartWidgetConfigForm.get('iconSizeUnit').disable();
      this.timeSeriesChartWidgetConfigForm.get('icon').disable();
      this.timeSeriesChartWidgetConfigForm.get('iconColor').disable();
    }

    if (showLegend) {
      this.timeSeriesChartWidgetConfigForm.get('legendLabelFont').enable();
      this.timeSeriesChartWidgetConfigForm.get('legendLabelColor').enable();
      this.timeSeriesChartWidgetConfigForm.get('legendConfig').enable();
    } else {
      this.timeSeriesChartWidgetConfigForm.get('legendLabelFont').disable();
      this.timeSeriesChartWidgetConfigForm.get('legendLabelColor').disable();
      this.timeSeriesChartWidgetConfigForm.get('legendConfig').disable();
    }

    if (showTooltip) {
      this.timeSeriesChartWidgetConfigForm.get('tooltipTrigger').enable();
      this.timeSeriesChartWidgetConfigForm.get('tooltipValueFont').enable();
      this.timeSeriesChartWidgetConfigForm.get('tooltipValueColor').enable();
      this.timeSeriesChartWidgetConfigForm.get('tooltipShowDate').enable({emitEvent: false});
      this.timeSeriesChartWidgetConfigForm.get('tooltipBackgroundColor').enable();
      this.timeSeriesChartWidgetConfigForm.get('tooltipBackgroundBlur').enable();
      if (tooltipShowDate) {
        this.timeSeriesChartWidgetConfigForm.get('tooltipDateFormat').enable();
        this.timeSeriesChartWidgetConfigForm.get('tooltipDateFont').enable();
        this.timeSeriesChartWidgetConfigForm.get('tooltipDateColor').enable();
        this.timeSeriesChartWidgetConfigForm.get('tooltipDateInterval').enable();
      } else {
        this.timeSeriesChartWidgetConfigForm.get('tooltipDateFormat').disable();
        this.timeSeriesChartWidgetConfigForm.get('tooltipDateFont').disable();
        this.timeSeriesChartWidgetConfigForm.get('tooltipDateColor').disable();
        this.timeSeriesChartWidgetConfigForm.get('tooltipDateInterval').disable();
      }
    } else {
      this.timeSeriesChartWidgetConfigForm.get('tooltipValueFont').disable();
      this.timeSeriesChartWidgetConfigForm.get('tooltipValueColor').disable();
      this.timeSeriesChartWidgetConfigForm.get('tooltipShowDate').disable({emitEvent: false});
      this.timeSeriesChartWidgetConfigForm.get('tooltipDateFormat').disable();
      this.timeSeriesChartWidgetConfigForm.get('tooltipDateFont').disable();
      this.timeSeriesChartWidgetConfigForm.get('tooltipDateColor').disable();
      this.timeSeriesChartWidgetConfigForm.get('tooltipDateInterval').disable();
      this.timeSeriesChartWidgetConfigForm.get('tooltipBackgroundColor').disable();
      this.timeSeriesChartWidgetConfigForm.get('tooltipBackgroundBlur').disable();
    }
  }

  private getSeries(datasources?: Datasource[]): DataKey[] {
    if (datasources && datasources.length) {
      return datasources[0].dataKeys || [];
    }
    return [];
  }

  private setSeries(series: DataKey[], datasources?: Datasource[]) {
    if (datasources && datasources.length) {
      datasources[0].dataKeys = series;
    }
  }

  private removeYaxisId(series: DataKey[], yAxisId: TimeSeriesChartYAxisId): boolean {
    let changed = false;
    if (series) {
      series.forEach(key => {
        const keySettings = ((key.settings || {}) as TimeSeriesChartKeySettings);
        if (keySettings.yAxisId === yAxisId) {
          keySettings.yAxisId = 'default';
          changed = true;
        }
      });
    }
    return changed;
  }

  private getCardButtons(config: WidgetConfig): string[] {
    const buttons: string[] = [];
    if (isUndefined(config.enableFullscreen) || config.enableFullscreen) {
      buttons.push('fullscreen');
    }
    return buttons;
  }

  private setCardButtons(buttons: string[], config: WidgetConfig) {
    config.enableFullscreen = buttons.includes('fullscreen');
  }

  private _tooltipValuePreviewFn(): string {
    return formatValue(22, 0, '°C', false);
  }

  private _tooltipDatePreviewFn(): string {
    const dateFormat: DateFormatSettings = this.timeSeriesChartWidgetConfigForm.get('tooltipDateFormat').value;
    const processor = DateFormatProcessor.fromSettings(this.$injector, dateFormat);
    processor.update(Date.now());
    return processor.formatted;
  }
}
