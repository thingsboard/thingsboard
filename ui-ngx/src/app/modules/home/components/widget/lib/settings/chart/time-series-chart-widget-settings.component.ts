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

import { Component, Injector } from '@angular/core';
import {
  DataKey,
  Datasource,
  legendPositions,
  legendPositionTranslationMap,
  WidgetSettings,
  WidgetSettingsComponent
} from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { formatValue, isDefinedAndNotNull } from '@core/utils';
import { DateFormatProcessor, DateFormatSettings } from '@shared/models/widget-settings.models';
import {
  timeSeriesChartWidgetDefaultSettings
} from '@home/components/widget/lib/chart/time-series-chart-widget.models';
import {
  TimeSeriesChartKeySettings,
  TimeSeriesChartType,
  TimeSeriesChartYAxes,
  TimeSeriesChartYAxisId
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { WidgetService } from '@core/http/widget.service';
import { TimeSeriesChartTooltipTrigger } from '@home/components/widget/lib/chart/time-series-chart-tooltip.models';

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

  public get yAxisIds(): TimeSeriesChartYAxisId[] {
    const yAxes: TimeSeriesChartYAxes = this.timeSeriesChartWidgetSettingsForm.get('yAxes').value;
    return Object.keys(yAxes);
  }

  TimeSeriesChartType = TimeSeriesChartType;

  EChartsTooltipTrigger = TimeSeriesChartTooltipTrigger;

  legendPositions = legendPositions;

  legendPositionTranslationMap = legendPositionTranslationMap;

  timeSeriesChartWidgetSettingsForm: UntypedFormGroup;

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  tooltipDatePreviewFn = this._tooltipDatePreviewFn.bind(this);

  chartType: TimeSeriesChartType = TimeSeriesChartType.default;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  constructor(protected store: Store<AppState>,
              private $injector: Injector,
              private widgetService: WidgetService,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  public yAxisRemoved(yAxisId: TimeSeriesChartYAxisId): void {
    if (this.widgetConfig.config.datasources && this.widgetConfig.config.datasources.length > 1) {
      for (let i = 1; i < this.widgetConfig.config.datasources.length; i++) {
        const datasource = this.widgetConfig.config.datasources[i];
        this.removeYaxisId(datasource.dataKeys, yAxisId);
      }
    }
  }

  protected settingsForm(): UntypedFormGroup {
    return this.timeSeriesChartWidgetSettingsForm;
  }

  protected onWidgetConfigSet(widgetConfig: WidgetConfigComponentData) {
    const params = widgetConfig.typeParameters as any;
    if (isDefinedAndNotNull(params.chartType)) {
      this.chartType = params.chartType;
    } else {
      this.chartType = TimeSeriesChartType.default;
    }
    if (this.timeSeriesChartWidgetSettingsForm) {
      const isStateChartType = this.chartType === TimeSeriesChartType.state;
      const hasStatesControl = this.timeSeriesChartWidgetSettingsForm.contains('states');
      if (isStateChartType && !hasStatesControl) {
        this.timeSeriesChartWidgetSettingsForm.addControl('states', this.fb.control(widgetConfig.config.settings.states), { emitEvent: false });
      } else if (!isStateChartType && hasStatesControl) {
        this.timeSeriesChartWidgetSettingsForm.removeControl('states', { emitEvent: false });
      }
    }
  }

  protected defaultSettings(): WidgetSettings {
    return timeSeriesChartWidgetDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.timeSeriesChartWidgetSettingsForm = this.fb.group({

      comparisonEnabled: [settings.comparisonEnabled, []],
      timeForComparison: [settings.timeForComparison, []],
      comparisonCustomIntervalValue: [settings.comparisonCustomIntervalValue, [Validators.min(0)]],
      comparisonXAxis: [settings.comparisonXAxis, []],

      yAxes: [settings.yAxes, []],
      thresholds: [settings.thresholds, []],

      dataZoom: [settings.dataZoom, []],
      stack: [settings.stack, []],

      grid: [settings.grid, []],

      xAxis: [settings.xAxis, []],

      noAggregationBarWidthSettings: [settings.noAggregationBarWidthSettings, []],

      showLegend: [settings.showLegend, []],
      legendColumnTitleFont: [settings.legendColumnTitleFont, []],
      legendColumnTitleColor: [settings.legendColumnTitleColor, []],
      legendLabelFont: [settings.legendLabelFont, []],
      legendLabelColor: [settings.legendLabelColor, []],
      legendValueFont: [settings.legendValueFont, []],
      legendValueColor: [settings.legendValueColor, []],
      legendConfig: [settings.legendConfig, []],

      showTooltip: [settings.showTooltip, []],
      tooltipTrigger: [settings.tooltipTrigger, []],
      tooltipLabelFont: [settings.tooltipLabelFont, []],
      tooltipLabelColor: [settings.tooltipLabelColor, []],
      tooltipValueFont: [settings.tooltipValueFont, []],
      tooltipValueColor: [settings.tooltipValueColor, []],
      tooltipValueFormatter: [settings.tooltipValueFormatter, []],
      tooltipShowDate: [settings.tooltipShowDate, []],
      tooltipDateFormat: [settings.tooltipDateFormat, []],
      tooltipDateFont: [settings.tooltipDateFont, []],
      tooltipDateColor: [settings.tooltipDateColor, []],
      tooltipDateInterval: [settings.tooltipDateInterval, []],
      tooltipHideZeroValues: [settings.tooltipHideZeroValues ,[]],
      tooltipStackedShowTotal: [settings.tooltipStackedShowTotal, []],

      tooltipBackgroundColor: [settings.tooltipBackgroundColor, []],
      tooltipBackgroundBlur: [settings.tooltipBackgroundBlur, []],

      animation: [settings.animation, []],

      background: [settings.background, []],
      padding: [settings.padding, []]
    });
    if (this.chartType === TimeSeriesChartType.state) {
      this.timeSeriesChartWidgetSettingsForm.addControl('states', this.fb.control(settings.states, []));
    }
  }

  protected validatorTriggers(): string[] {
    return ['comparisonEnabled', 'showLegend', 'showTooltip', 'tooltipShowDate', 'stack'];
  }

  protected updateValidators(emitEvent: boolean) {
    const comparisonEnabled: boolean = this.timeSeriesChartWidgetSettingsForm.get('comparisonEnabled').value;
    const showLegend: boolean = this.timeSeriesChartWidgetSettingsForm.get('showLegend').value;
    const showTooltip: boolean = this.timeSeriesChartWidgetSettingsForm.get('showTooltip').value;
    const tooltipShowDate: boolean = this.timeSeriesChartWidgetSettingsForm.get('tooltipShowDate').value;
    const stack: boolean = this.timeSeriesChartWidgetSettingsForm.get('stack').value;

    if (comparisonEnabled) {
      this.timeSeriesChartWidgetSettingsForm.get('timeForComparison').enable();
      this.timeSeriesChartWidgetSettingsForm.get('comparisonCustomIntervalValue').enable();
      this.timeSeriesChartWidgetSettingsForm.get('comparisonXAxis').enable();
    } else {
      this.timeSeriesChartWidgetSettingsForm.get('timeForComparison').disable();
      this.timeSeriesChartWidgetSettingsForm.get('comparisonCustomIntervalValue').disable();
      this.timeSeriesChartWidgetSettingsForm.get('comparisonXAxis').disable();
    }

    if (showLegend) {
      this.timeSeriesChartWidgetSettingsForm.get('legendColumnTitleFont').enable();
      this.timeSeriesChartWidgetSettingsForm.get('legendColumnTitleColor').enable();
      this.timeSeriesChartWidgetSettingsForm.get('legendLabelFont').enable();
      this.timeSeriesChartWidgetSettingsForm.get('legendLabelColor').enable();
      this.timeSeriesChartWidgetSettingsForm.get('legendValueFont').enable();
      this.timeSeriesChartWidgetSettingsForm.get('legendValueColor').enable();
      this.timeSeriesChartWidgetSettingsForm.get('legendConfig').enable();
    } else {
      this.timeSeriesChartWidgetSettingsForm.get('legendColumnTitleFont').disable();
      this.timeSeriesChartWidgetSettingsForm.get('legendColumnTitleColor').disable();
      this.timeSeriesChartWidgetSettingsForm.get('legendLabelFont').disable();
      this.timeSeriesChartWidgetSettingsForm.get('legendLabelColor').disable();
      this.timeSeriesChartWidgetSettingsForm.get('legendValueFont').disable();
      this.timeSeriesChartWidgetSettingsForm.get('legendValueColor').disable();
      this.timeSeriesChartWidgetSettingsForm.get('legendConfig').disable();
    }

    if (showTooltip) {
      this.timeSeriesChartWidgetSettingsForm.get('tooltipTrigger').enable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipLabelFont').enable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipLabelColor').enable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipValueFont').enable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipValueColor').enable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipValueFormatter').enable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipShowDate').enable({emitEvent: false});
      this.timeSeriesChartWidgetSettingsForm.get('tooltipHideZeroValues').enable();
      if (stack) {
        this.timeSeriesChartWidgetSettingsForm.get('tooltipStackedShowTotal').enable();
      } else {
        this.timeSeriesChartWidgetSettingsForm.get('tooltipStackedShowTotal').disable();
      }
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
      this.timeSeriesChartWidgetSettingsForm.get('tooltipTrigger').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipLabelFont').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipLabelColor').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipValueFont').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipValueColor').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipValueFormatter').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipShowDate').disable({emitEvent: false});
      this.timeSeriesChartWidgetSettingsForm.get('tooltipDateFormat').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipDateFont').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipDateColor').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipDateInterval').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipHideZeroValues').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipStackedShowTotal').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipBackgroundColor').disable();
      this.timeSeriesChartWidgetSettingsForm.get('tooltipBackgroundBlur').disable();
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
