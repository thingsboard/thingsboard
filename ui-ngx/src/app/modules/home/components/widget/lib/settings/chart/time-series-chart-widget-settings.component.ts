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
import {
  Datasource,
  legendPositions,
  legendPositionTranslationMap,
  WidgetSettings,
  WidgetSettingsComponent
} from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { formatValue, isDefinedAndNotNull, mergeDeep } from '@core/utils';
import { DateFormatProcessor, DateFormatSettings } from '@shared/models/widget-settings.models';
import {
  barChartWithLabelsDefaultSettings
} from '@home/components/widget/lib/chart/bar-chart-with-labels-widget.models';
import { EChartsTooltipTrigger } from '../../chart/echarts-widget.models';
import {
  timeSeriesChartWidgetDefaultSettings, TimeSeriesChartWidgetSettings
} from '@home/components/widget/lib/chart/time-series-chart-widget.models';
import {
  timeSeriesChartNoAggregationBarWidthStrategies,
  TimeSeriesChartNoAggregationBarWidthStrategy,
  timeSeriesChartNoAggregationBarWidthStrategyTranslations, TimeSeriesChartType
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';

@Component({
  selector: 'tb-time-series-chart-widget-settings',
  templateUrl: './time-series-chart-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class TimeSeriesChartWidgetSettingsComponent extends WidgetSettingsComponent {

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.widgetConfig.config.datasources;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  TimeSeriesChartType = TimeSeriesChartType;

  EChartsTooltipTrigger = EChartsTooltipTrigger;

  legendPositions = legendPositions;

  legendPositionTranslationMap = legendPositionTranslationMap;

  TimeSeriesChartNoAggregationBarWidthStrategy = TimeSeriesChartNoAggregationBarWidthStrategy;

  timeSeriesChartNoAggregationBarWidthStrategies = timeSeriesChartNoAggregationBarWidthStrategies;

  timeSeriesChartNoAggregationBarWidthStrategyTranslations = timeSeriesChartNoAggregationBarWidthStrategyTranslations;

  timeSeriesChartWidgetSettingsForm: UntypedFormGroup;

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  tooltipDatePreviewFn = this._tooltipDatePreviewFn.bind(this);

  chartType: TimeSeriesChartType = TimeSeriesChartType.default;

  constructor(protected store: Store<AppState>,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.timeSeriesChartWidgetSettingsForm;
  }

  protected onWidgetConfigSet(widgetConfig: WidgetConfigComponentData) {
    const params = widgetConfig.typeParameters as any;
    if (isDefinedAndNotNull(params.chartType)) {
      this.chartType = params.chartType;
    }
  }

  protected defaultSettings(): WidgetSettings {
    return mergeDeep({} as TimeSeriesChartWidgetSettings, timeSeriesChartWidgetDefaultSettings);
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.timeSeriesChartWidgetSettingsForm = this.fb.group({

      thresholds: [settings.thresholds, []],

      dataZoom: [settings.dataZoom, []],
      stack: [settings.stack, []],

      yAxis: [settings.yAxis, []],
      xAxis: [settings.xAxis, []],

      noAggregationBarWidthSettings: this.fb.group({
        strategy: [settings.noAggregationBarWidthSettings.strategy, []],
        groupIntervalWidth: [settings.noAggregationBarWidthSettings.groupIntervalWidth, [Validators.min(100)]],
        separateBarWidth: [settings.noAggregationBarWidthSettings.separateBarWidth, [Validators.min(100)]],
      }),

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

      background: [settings.background, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showLegend', 'showTooltip', 'tooltipShowDate', 'noAggregationBarWidthSettings.strategy'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showLegend: boolean = this.timeSeriesChartWidgetSettingsForm.get('showLegend').value;
    const showTooltip: boolean = this.timeSeriesChartWidgetSettingsForm.get('showTooltip').value;
    const tooltipShowDate: boolean = this.timeSeriesChartWidgetSettingsForm.get('tooltipShowDate').value;
    const noAggregationBarWidthSettingsStrategy: TimeSeriesChartNoAggregationBarWidthStrategy =
      this.timeSeriesChartWidgetSettingsForm.get('noAggregationBarWidthSettings').get('strategy').value;

    if (noAggregationBarWidthSettingsStrategy === TimeSeriesChartNoAggregationBarWidthStrategy.group) {
      this.timeSeriesChartWidgetSettingsForm.get('noAggregationBarWidthSettings').get('groupIntervalWidth').enable();
      this.timeSeriesChartWidgetSettingsForm.get('noAggregationBarWidthSettings').get('separateBarWidth').disable();
    } else if (noAggregationBarWidthSettingsStrategy === TimeSeriesChartNoAggregationBarWidthStrategy.separate) {
      this.timeSeriesChartWidgetSettingsForm.get('noAggregationBarWidthSettings').get('groupIntervalWidth').disable();
      this.timeSeriesChartWidgetSettingsForm.get('noAggregationBarWidthSettings').get('separateBarWidth').enable();
    }

    if (showLegend) {
      this.timeSeriesChartWidgetSettingsForm.get('legendLabelFont').enable();
      this.timeSeriesChartWidgetSettingsForm.get('legendLabelColor').enable();
      this.timeSeriesChartWidgetSettingsForm.get('legendConfig').enable();
    } else {
      this.timeSeriesChartWidgetSettingsForm.get('legendLabelFont').disable();
      this.timeSeriesChartWidgetSettingsForm.get('legendLabelColor').disable();
      this.timeSeriesChartWidgetSettingsForm.get('legendConfig').disable();
    }

    if (showTooltip) {
      this.timeSeriesChartWidgetSettingsForm.get('tooltipTrigger').enable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipValueFont').enable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipValueColor').enable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipShowDate').enable({emitEvent: false});
      this.timeSeriesChartWidgetSettingsForm.get('tooltipBackgroundColor').enable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipBackgroundBlur').enable();
      if (tooltipShowDate) {
        this.timeSeriesChartWidgetSettingsForm.get('tooltipDateFormat').enable();
        this.timeSeriesChartWidgetSettingsForm.get('tooltipDateFont').enable();
        this.timeSeriesChartWidgetSettingsForm.get('tooltipDateColor').enable();
        this.timeSeriesChartWidgetSettingsForm.get('tooltipDateInterval').enable();
      } else {
        this.timeSeriesChartWidgetSettingsForm.get('tooltipDateFormat').disable();
        this.timeSeriesChartWidgetSettingsForm.get('tooltipDateFont').disable();
        this.timeSeriesChartWidgetSettingsForm.get('tooltipDateColor').disable();
        this.timeSeriesChartWidgetSettingsForm.get('tooltipDateInterval').disable();
      }
    } else {
      this.timeSeriesChartWidgetSettingsForm.get('tooltipValueFont').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipValueColor').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipShowDate').disable({emitEvent: false});
      this.timeSeriesChartWidgetSettingsForm.get('tooltipDateFormat').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipDateFont').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipDateColor').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipDateInterval').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipBackgroundColor').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipBackgroundBlur').disable();
    }
  }

  private _tooltipValuePreviewFn(): string {
    return formatValue(22, 0, '°C', false);
  }

  private _tooltipDatePreviewFn(): string {
    const dateFormat: DateFormatSettings = this.timeSeriesChartWidgetSettingsForm.get('tooltipDateFormat').value;
    const processor = DateFormatProcessor.fromSettings(this.$injector, dateFormat);
    processor.update(Date.now());
    return processor.formatted;
  }
}
