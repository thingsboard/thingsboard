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
  polarAreaChartWidgetDefaultSettings,
  PolarAreaChartWidgetSettings
} from '@home/components/widget/lib/chart/polar-area-widget.models';

@Component({
  selector: 'tb-polar-area-chart-widget-settings',
  templateUrl: './polar-area-chart-widget-settings.component.html',
  styleUrls: []
})
export class PolarAreaChartWidgetSettingsComponent extends WidgetSettingsComponent {

  legendPositions = legendPositions;

  legendPositionTranslationMap = legendPositionTranslationMap;

  latestChartTooltipValueTypes = latestChartTooltipValueTypes;

  latestChartTooltipValueTypeTranslationMap = latestChartTooltipValueTypeTranslations;

  polarAreaChartWidgetSettingsForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.polarAreaChartWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return mergeDeep<PolarAreaChartWidgetSettings>({} as PolarAreaChartWidgetSettings, polarAreaChartWidgetDefaultSettings);
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.polarAreaChartWidgetSettingsForm = this.fb.group({

      sortSeries: [settings.sortSeries, []],

      barSettings: [settings.barSettings, []],

      axisMin: [settings.axisMin, []],
      axisMax: [settings.axisMax, []],
      axisTickLabelFont: [settings.axisTickLabelFont, []],
      axisTickLabelColor: [settings.axisTickLabelColor, []],
      angleAxisStartAngle: [settings.angleAxisStartAngle, [Validators.min(0), Validators.max(360)]],

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
    return ['showLegend', 'showTooltip'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showLegend: boolean = this.polarAreaChartWidgetSettingsForm.get('showLegend').value;
    const showTooltip: boolean = this.polarAreaChartWidgetSettingsForm.get('showTooltip').value;

    if (showLegend) {
      this.polarAreaChartWidgetSettingsForm.get('legendPosition').enable();
      this.polarAreaChartWidgetSettingsForm.get('legendLabelFont').enable();
      this.polarAreaChartWidgetSettingsForm.get('legendLabelColor').enable();
      this.polarAreaChartWidgetSettingsForm.get('legendValueFont').enable();
      this.polarAreaChartWidgetSettingsForm.get('legendValueColor').enable();
    } else {
      this.polarAreaChartWidgetSettingsForm.get('legendPosition').disable();
      this.polarAreaChartWidgetSettingsForm.get('legendLabelFont').disable();
      this.polarAreaChartWidgetSettingsForm.get('legendLabelColor').disable();
      this.polarAreaChartWidgetSettingsForm.get('legendValueFont').disable();
      this.polarAreaChartWidgetSettingsForm.get('legendValueColor').disable();
    }
    if (showTooltip) {
      this.polarAreaChartWidgetSettingsForm.get('tooltipValueType').enable();
      this.polarAreaChartWidgetSettingsForm.get('tooltipValueDecimals').enable();
      this.polarAreaChartWidgetSettingsForm.get('tooltipValueFont').enable();
      this.polarAreaChartWidgetSettingsForm.get('tooltipValueColor').enable();
      this.polarAreaChartWidgetSettingsForm.get('tooltipBackgroundColor').enable();
      this.polarAreaChartWidgetSettingsForm.get('tooltipBackgroundBlur').enable();
    } else {
      this.polarAreaChartWidgetSettingsForm.get('tooltipValueType').disable();
      this.polarAreaChartWidgetSettingsForm.get('tooltipValueDecimals').disable();
      this.polarAreaChartWidgetSettingsForm.get('tooltipValueFont').disable();
      this.polarAreaChartWidgetSettingsForm.get('tooltipValueColor').disable();
      this.polarAreaChartWidgetSettingsForm.get('tooltipBackgroundColor').disable();
      this.polarAreaChartWidgetSettingsForm.get('tooltipBackgroundBlur').disable();
    }
  }

  private _valuePreviewFn(): string {
    const units: string = this.widgetConfig.config.units;
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(110, decimals, units, false);
  }

  private _tooltipValuePreviewFn(): string {
    const tooltipValueType: LatestChartTooltipValueType = this.polarAreaChartWidgetSettingsForm.get('tooltipValueType').value;
    const decimals: number = this.polarAreaChartWidgetSettingsForm.get('tooltipValueDecimals').value;
    if (tooltipValueType === LatestChartTooltipValueType.percentage) {
      return formatValue(35, decimals, '%', false);
    } else {
      const units: string = this.widgetConfig.config.units;
      return formatValue(110, decimals, units, false);
    }
  }

}
