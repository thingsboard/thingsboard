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

import { Component } from '@angular/core';
import {
  legendPositions,
  legendPositionTranslationMap,
  WidgetSettings,
  WidgetSettingsComponent
} from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { formatValue, mergeDeep } from '@core/utils';
import {
  LatestChartTooltipValueType,
  latestChartTooltipValueTypes,
  latestChartTooltipValueTypeTranslations
} from '@home/components/widget/lib/chart/latest-chart.models';
import {
  pieChartLabelPositions,
  pieChartLabelPositionTranslations
} from '@home/components/widget/lib/chart/pie-chart.models';
import {
  pieChartWidgetDefaultSettings,
  PieChartWidgetSettings
} from '@home/components/widget/lib/chart/pie-chart-widget.models';

@Component({
  selector: 'tb-pie-chart-widget-settings',
  templateUrl: './pie-chart-widget-settings.component.html',
  styleUrls: []
})
export class PieChartWidgetSettingsComponent extends WidgetSettingsComponent {

  pieChartLabelPositions = pieChartLabelPositions;

  pieChartLabelPositionTranslationMap = pieChartLabelPositionTranslations;

  legendPositions = legendPositions;

  legendPositionTranslationMap = legendPositionTranslationMap;

  latestChartTooltipValueTypes = latestChartTooltipValueTypes;

  latestChartTooltipValueTypeTranslationMap = latestChartTooltipValueTypeTranslations;

  pieChartWidgetSettingsForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.pieChartWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return mergeDeep<PieChartWidgetSettings>({} as PieChartWidgetSettings, pieChartWidgetDefaultSettings);
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.pieChartWidgetSettingsForm = this.fb.group({
      showLabel: [settings.showLabel, []],
      labelPosition: [settings.labelPosition, []],
      labelFont: [settings.labelFont, []],
      labelColor: [settings.labelColor, []],

      borderWidth: [settings.borderWidth, [Validators.min(0)]],
      borderColor: [settings.borderColor, []],

      radius: [settings.radius, [Validators.min(0), Validators.max(100)]],

      clockwise: [settings.clockwise, []],
      sortSeries: [settings.sortSeries, []],

      animation: [settings.animation, []],

      showLegend: [settings.showLegend, []],
      legendPosition: [settings.legendPosition, []],
      legendLabelFont: [settings.legendLabelFont, []],
      legendLabelColor: [settings.legendLabelColor, []],
      legendValueFont: [settings.legendValueFont, []],
      legendValueColor: [settings.legendValueColor, []],

      showTooltip: [settings.showTooltip, []],
      tooltipValueType: [settings.tooltipValueType, []],
      tooltipValueDecimals: [settings.tooltipValueDecimals, []],
      tooltipValueFont: [settings.tooltipValueFont, []],
      tooltipValueColor: [settings.tooltipValueColor, []],
      tooltipBackgroundColor: [settings.tooltipBackgroundColor, []],
      tooltipBackgroundBlur: [settings.tooltipBackgroundBlur, []],

      background: [settings.background, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showLabel', 'showLegend', 'showTooltip'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showLabel: boolean = this.pieChartWidgetSettingsForm.get('showLabel').value;
    const showLegend: boolean = this.pieChartWidgetSettingsForm.get('showLegend').value;
    const showTooltip: boolean = this.pieChartWidgetSettingsForm.get('showTooltip').value;

    if (showLabel) {
      this.pieChartWidgetSettingsForm.get('labelPosition').enable();
      this.pieChartWidgetSettingsForm.get('labelFont').enable();
      this.pieChartWidgetSettingsForm.get('labelColor').enable();
    } else {
      this.pieChartWidgetSettingsForm.get('labelPosition').disable();
      this.pieChartWidgetSettingsForm.get('labelFont').disable();
      this.pieChartWidgetSettingsForm.get('labelColor').disable();
    }

    if (showLegend) {
      this.pieChartWidgetSettingsForm.get('legendPosition').enable();
      this.pieChartWidgetSettingsForm.get('legendLabelFont').enable();
      this.pieChartWidgetSettingsForm.get('legendLabelColor').enable();
      this.pieChartWidgetSettingsForm.get('legendValueFont').enable();
      this.pieChartWidgetSettingsForm.get('legendValueColor').enable();
    } else {
      this.pieChartWidgetSettingsForm.get('legendPosition').disable();
      this.pieChartWidgetSettingsForm.get('legendLabelFont').disable();
      this.pieChartWidgetSettingsForm.get('legendLabelColor').disable();
      this.pieChartWidgetSettingsForm.get('legendValueFont').disable();
      this.pieChartWidgetSettingsForm.get('legendValueColor').disable();
    }
    if (showTooltip) {
      this.pieChartWidgetSettingsForm.get('tooltipValueType').enable();
      this.pieChartWidgetSettingsForm.get('tooltipValueDecimals').enable();
      this.pieChartWidgetSettingsForm.get('tooltipValueFont').enable();
      this.pieChartWidgetSettingsForm.get('tooltipValueColor').enable();
      this.pieChartWidgetSettingsForm.get('tooltipBackgroundColor').enable();
      this.pieChartWidgetSettingsForm.get('tooltipBackgroundBlur').enable();
    } else {
      this.pieChartWidgetSettingsForm.get('tooltipValueType').disable();
      this.pieChartWidgetSettingsForm.get('tooltipValueDecimals').disable();
      this.pieChartWidgetSettingsForm.get('tooltipValueFont').disable();
      this.pieChartWidgetSettingsForm.get('tooltipValueColor').disable();
      this.pieChartWidgetSettingsForm.get('tooltipBackgroundColor').disable();
      this.pieChartWidgetSettingsForm.get('tooltipBackgroundBlur').disable();
    }
  }

  private _valuePreviewFn(): string {
    const units: string = this.widgetConfig.config.units;
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(110, decimals, units, false);
  }

  private _tooltipValuePreviewFn(): string {
    const tooltipValueType: LatestChartTooltipValueType = this.pieChartWidgetSettingsForm.get('tooltipValueType').value;
    const decimals: number = this.pieChartWidgetSettingsForm.get('tooltipValueDecimals').value;
    if (tooltipValueType === LatestChartTooltipValueType.percentage) {
      return formatValue(35, decimals, '%', false);
    } else {
      const units: string = this.widgetConfig.config.units;
      return formatValue(110, decimals, units, false);
    }
  }

}
