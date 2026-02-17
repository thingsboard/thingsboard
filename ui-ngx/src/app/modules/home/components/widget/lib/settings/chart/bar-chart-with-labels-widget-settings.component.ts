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
  Datasource,
  legendPositions,
  legendPositionTranslationMap,
  WidgetSettings,
  WidgetSettingsComponent
} from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { formatValue } from '@core/utils';
import { DateFormatProcessor, DateFormatSettings } from '@shared/models/widget-settings.models';
import {
  barChartWithLabelsDefaultSettings
} from '@home/components/widget/lib/chart/bar-chart-with-labels-widget.models';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

@Component({
    selector: 'tb-bar-chart-with-labels-widget-settings',
    templateUrl: './bar-chart-with-labels-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class BarChartWithLabelsWidgetSettingsComponent extends WidgetSettingsComponent {

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.widgetConfig.config.datasources;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  legendPositions = legendPositions;

  legendPositionTranslationMap = legendPositionTranslationMap;

  barChartWidgetSettingsForm: UntypedFormGroup;

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  tooltipDatePreviewFn = this._tooltipDatePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.barChartWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return barChartWithLabelsDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.barChartWidgetSettingsForm = this.fb.group({

      dataZoom: [settings.dataZoom, []],

      showBarLabel: [settings.showBarLabel, []],
      barLabelFont: [settings.barLabelFont, []],
      barLabelColor: [settings.barLabelColor, []],
      showBarValue: [settings.showBarValue, []],
      barValueFont: [settings.barValueFont, []],
      barValueColor: [settings.barValueColor, []],
      showBarBorder: [settings.showBarBorder, []],
      barBorderWidth: [settings.barBorderWidth, []],
      barBorderRadius: [settings.barBorderRadius, []],
      barBackgroundSettings: [settings.barBackgroundSettings, []],
      noAggregationBarWidthSettings: [settings.noAggregationBarWidthSettings, []],

      grid: [settings.grid, []],

      yAxis: [settings.yAxis, []],
      xAxis: [settings.xAxis, []],

      thresholds: [settings.thresholds, []],

      animation: [settings.animation, []],

      showLegend: [settings.showLegend, []],
      legendPosition: [settings.legendPosition, []],
      legendLabelFont: [settings.legendLabelFont, []],
      legendLabelColor: [settings.legendLabelColor, []],

      showTooltip: [settings.showTooltip, []],
      tooltipLabelFont: [settings.tooltipLabelFont, []],
      tooltipLabelColor: [settings.tooltipLabelColor, []],
      tooltipValueFont: [settings.tooltipValueFont, []],
      tooltipValueColor: [settings.tooltipValueColor, []],
      tooltipShowDate: [settings.tooltipShowDate, []],
      tooltipDateFormat: [settings.tooltipDateFormat, []],
      tooltipDateFont: [settings.tooltipDateFont, []],
      tooltipDateColor: [settings.tooltipDateColor, []],
      tooltipDateInterval: [settings.tooltipDateInterval, []],

      tooltipBackgroundColor: [settings.tooltipBackgroundColor, []],
      tooltipBackgroundBlur: [settings.tooltipBackgroundBlur, []],

      background: [settings.background, []],
      padding: [settings.padding, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showBarLabel', 'showBarValue', 'showBarBorder', 'showLegend', 'showTooltip', 'tooltipShowDate'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showBarLabel: boolean = this.barChartWidgetSettingsForm.get('showBarLabel').value;
    const showBarValue: boolean = this.barChartWidgetSettingsForm.get('showBarValue').value;
    const showBarBorder: boolean = this.barChartWidgetSettingsForm.get('showBarBorder').value;
    const showLegend: boolean = this.barChartWidgetSettingsForm.get('showLegend').value;
    const showTooltip: boolean = this.barChartWidgetSettingsForm.get('showTooltip').value;
    const tooltipShowDate: boolean = this.barChartWidgetSettingsForm.get('tooltipShowDate').value;

    if (showBarLabel) {
      this.barChartWidgetSettingsForm.get('barLabelFont').enable();
      this.barChartWidgetSettingsForm.get('barLabelColor').enable();
    } else {
      this.barChartWidgetSettingsForm.get('barLabelFont').disable();
      this.barChartWidgetSettingsForm.get('barLabelColor').disable();
    }

    if (showBarValue) {
      this.barChartWidgetSettingsForm.get('barValueFont').enable();
      this.barChartWidgetSettingsForm.get('barValueColor').enable();
    } else {
      this.barChartWidgetSettingsForm.get('barValueFont').disable();
      this.barChartWidgetSettingsForm.get('barValueColor').disable();
    }

    if (showBarBorder) {
      this.barChartWidgetSettingsForm.get('barBorderWidth').enable();
    } else {
      this.barChartWidgetSettingsForm.get('barBorderWidth').disable();
    }

    if (showLegend) {
      this.barChartWidgetSettingsForm.get('legendPosition').enable();
      this.barChartWidgetSettingsForm.get('legendLabelFont').enable();
      this.barChartWidgetSettingsForm.get('legendLabelColor').enable();
    } else {
      this.barChartWidgetSettingsForm.get('legendPosition').disable();
      this.barChartWidgetSettingsForm.get('legendLabelFont').disable();
      this.barChartWidgetSettingsForm.get('legendLabelColor').disable();
    }

    if (showTooltip) {
      this.barChartWidgetSettingsForm.get('tooltipLabelFont').enable();
      this.barChartWidgetSettingsForm.get('tooltipLabelColor').enable();
      this.barChartWidgetSettingsForm.get('tooltipValueFont').enable();
      this.barChartWidgetSettingsForm.get('tooltipValueColor').enable();
      this.barChartWidgetSettingsForm.get('tooltipShowDate').enable({emitEvent: false});
      this.barChartWidgetSettingsForm.get('tooltipBackgroundColor').enable();
      this.barChartWidgetSettingsForm.get('tooltipBackgroundBlur').enable();
      if (tooltipShowDate) {
        this.barChartWidgetSettingsForm.get('tooltipDateFormat').enable();
        this.barChartWidgetSettingsForm.get('tooltipDateFont').enable();
        this.barChartWidgetSettingsForm.get('tooltipDateColor').enable();
        this.barChartWidgetSettingsForm.get('tooltipDateInterval').enable();
      } else {
        this.barChartWidgetSettingsForm.get('tooltipDateFormat').disable();
        this.barChartWidgetSettingsForm.get('tooltipDateFont').disable();
        this.barChartWidgetSettingsForm.get('tooltipDateColor').disable();
        this.barChartWidgetSettingsForm.get('tooltipDateInterval').disable();
      }
    } else {
      this.barChartWidgetSettingsForm.get('tooltipLabelFont').disable();
      this.barChartWidgetSettingsForm.get('tooltipLabelColor').disable();
      this.barChartWidgetSettingsForm.get('tooltipValueFont').disable();
      this.barChartWidgetSettingsForm.get('tooltipValueColor').disable();
      this.barChartWidgetSettingsForm.get('tooltipShowDate').disable({emitEvent: false});
      this.barChartWidgetSettingsForm.get('tooltipDateFormat').disable();
      this.barChartWidgetSettingsForm.get('tooltipDateFont').disable();
      this.barChartWidgetSettingsForm.get('tooltipDateColor').disable();
      this.barChartWidgetSettingsForm.get('tooltipDateInterval').disable();
      this.barChartWidgetSettingsForm.get('tooltipBackgroundColor').disable();
      this.barChartWidgetSettingsForm.get('tooltipBackgroundBlur').disable();
    }
  }

  private _tooltipValuePreviewFn(): string {
    const units = getSourceTbUnitSymbol(this.widgetConfig.config.units);
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(22, decimals, units, false);
  }

  private _tooltipDatePreviewFn(): string {
    const dateFormat: DateFormatSettings = this.barChartWidgetSettingsForm.get('tooltipDateFormat').value;
    const processor = DateFormatProcessor.fromSettings(this.$injector, dateFormat);
    processor.update(Date.now());
    return processor.formatted;
  }

}
