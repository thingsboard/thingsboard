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

import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { isDefinedAndNotNull } from '@core/utils';
import {
  timeSeriesChartKeyDefaultSettings,
  TimeSeriesChartKeySettings,
  TimeSeriesChartSeriesType,
  timeSeriesChartSeriesTypes,
  timeSeriesChartSeriesTypeTranslations,
  TimeSeriesChartType,
  timeSeriesChartTypeTranslations,
  TimeSeriesChartYAxisId
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { TimeSeriesChartWidgetSettings } from '@home/components/widget/lib/chart/time-series-chart-widget.models';
import { WidgetService } from '@core/http/widget.service';

@Component({
    selector: 'tb-time-series-chart-key-settings',
    templateUrl: './time-series-chart-key-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class TimeSeriesChartKeySettingsComponent extends WidgetSettingsComponent {

  TimeSeriesChartType = TimeSeriesChartType;

  timeSeriesChartTypeTranslations = timeSeriesChartTypeTranslations;

  TimeSeriesChartSeriesType = TimeSeriesChartSeriesType;

  timeSeriesChartSeriesTypes = timeSeriesChartSeriesTypes;

  timeSeriesChartSeriesTypeTranslations = timeSeriesChartSeriesTypeTranslations;

  timeSeriesChartKeySettingsForm: UntypedFormGroup;

  chartType = TimeSeriesChartType.default;

  yAxisIds: TimeSeriesChartYAxisId[];

  comparisonEnabled: boolean;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  constructor(protected store: Store<AppState>,
              private widgetService: WidgetService,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.timeSeriesChartKeySettingsForm;
  }

  protected onWidgetConfigSet(widgetConfig: WidgetConfigComponentData) {
    const params = widgetConfig.typeParameters as any;
    if (isDefinedAndNotNull(params.chartType)) {
      this.chartType = params.chartType;
    }
    const widgetSettings = (widgetConfig.config?.settings || {}) as TimeSeriesChartWidgetSettings;
    this.yAxisIds = widgetSettings.yAxes ? Object.keys(widgetSettings.yAxes) : ['default'];
    this.comparisonEnabled = !!widgetSettings.comparisonEnabled;
  }

  protected defaultSettings(): WidgetSettings {
    return timeSeriesChartKeyDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    const seriesSettings = settings as TimeSeriesChartKeySettings;
    let yAxisId = seriesSettings.yAxisId;
    if (!this.yAxisIds.includes(yAxisId)) {
      yAxisId = 'default';
    }
    this.timeSeriesChartKeySettingsForm = this.fb.group({
      yAxisId: [yAxisId, []],
      showInLegend: [seriesSettings.showInLegend, []],
      dataHiddenByDefault: [seriesSettings.dataHiddenByDefault, []],
      type: [seriesSettings.type, []],
      lineSettings: [seriesSettings.lineSettings, []],
      barSettings: [seriesSettings.barSettings, []],
      tooltipValueFormatter: [seriesSettings.tooltipValueFormatter, []],
      comparisonSettings: this.fb.group({
        showValuesForComparison: [seriesSettings.comparisonSettings?.showValuesForComparison, []],
        comparisonValuesLabel: [seriesSettings.comparisonSettings?.comparisonValuesLabel, []],
        color: [seriesSettings.comparisonSettings?.color, []]
      })
    });
  }

  protected validatorTriggers(): string[] {
    return ['showInLegend', 'type', 'comparisonSettings.showValuesForComparison'];
  }

  protected updateValidators(_emitEvent: boolean) {
    const showInLegend: boolean = this.timeSeriesChartKeySettingsForm.get('showInLegend').value;
    const type: TimeSeriesChartSeriesType = this.timeSeriesChartKeySettingsForm.get('type').value;
    const showValuesForComparison: boolean =
      this.timeSeriesChartKeySettingsForm.get('comparisonSettings').get('showValuesForComparison').value;
    if (showInLegend) {
      this.timeSeriesChartKeySettingsForm.get('dataHiddenByDefault').enable();
    } else {
      this.timeSeriesChartKeySettingsForm.get('dataHiddenByDefault').patchValue(false, {emitEvent: false});
      this.timeSeriesChartKeySettingsForm.get('dataHiddenByDefault').disable();
    }
    if (type === TimeSeriesChartSeriesType.line) {
      this.timeSeriesChartKeySettingsForm.get('lineSettings').enable();
      this.timeSeriesChartKeySettingsForm.get('barSettings').disable();
    } else if (type === TimeSeriesChartSeriesType.bar) {
      this.timeSeriesChartKeySettingsForm.get('lineSettings').disable();
      this.timeSeriesChartKeySettingsForm.get('barSettings').enable();
    }
    if (this.comparisonEnabled) {
      this.timeSeriesChartKeySettingsForm.get('comparisonSettings').enable({emitEvent: false});
      if (showValuesForComparison) {
        this.timeSeriesChartKeySettingsForm.get('comparisonSettings').get('comparisonValuesLabel').enable();
        this.timeSeriesChartKeySettingsForm.get('comparisonSettings').get('color').enable();
      } else {
        this.timeSeriesChartKeySettingsForm.get('comparisonSettings').get('comparisonValuesLabel').disable();
        this.timeSeriesChartKeySettingsForm.get('comparisonSettings').get('color').disable();
      }
    } else {
      this.timeSeriesChartKeySettingsForm.get('comparisonSettings').disable({emitEvent: false});
    }
  }
}
