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
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { formatValue, isDefinedAndNotNull } from '@core/utils';
import {
  doughnutDefaultSettings,
  DoughnutLayout,
  doughnutLayoutImages,
  doughnutLayouts,
  doughnutLayoutTranslations,
  DoughnutTooltipValueType,
  doughnutTooltipValueTypes,
  doughnutTooltipValueTypeTranslations,
  horizontalDoughnutLayoutImages
} from '@home/components/widget/lib/chart/doughnut-widget.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';

@Component({
  selector: 'tb-doughnut-widget-settings',
  templateUrl: './doughnut-widget-settings.component.html',
  styleUrls: []
})
export class DoughnutWidgetSettingsComponent extends WidgetSettingsComponent {

  get totalEnabled(): boolean {
    const layout: DoughnutLayout = this.doughnutWidgetSettingsForm.get('layout').value;
    return layout === DoughnutLayout.with_total;
  }

  doughnutLayouts = doughnutLayouts;

  doughnutLayoutTranslationMap = doughnutLayoutTranslations;

  horizontal = false;

  doughnutLayoutImageMap: Map<DoughnutLayout, string>;

  legendPositions = legendPositions;

  legendPositionTranslationMap = legendPositionTranslationMap;

  doughnutTooltipValueTypes = doughnutTooltipValueTypes;

  doughnutTooltipValueTypeTranslationMap = doughnutTooltipValueTypeTranslations;

  doughnutWidgetSettingsForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.doughnutWidgetSettingsForm;
  }

  protected onWidgetConfigSet(widgetConfig: WidgetConfigComponentData) {
    const params = widgetConfig.typeParameters as any;
    this.horizontal  = isDefinedAndNotNull(params.horizontal) ? params.horizontal : false;
    this.doughnutLayoutImageMap = this.horizontal ? horizontalDoughnutLayoutImages : doughnutLayoutImages;
  }

  protected defaultSettings(): WidgetSettings {
    return doughnutDefaultSettings(this.horizontal);
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.doughnutWidgetSettingsForm = this.fb.group({
      layout: [settings.layout, []],
      autoScale: [settings.autoScale, []],
      clockwise: [settings.clockwise, []],
      sortSeries: [settings.sortSeries, []],

      totalValueFont: [settings.totalValueFont, []],
      totalValueColor: [settings.totalValueColor, []],

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
    return ['layout', 'showLegend', 'showTooltip'];
  }

  protected updateValidators(emitEvent: boolean) {
    const layout: DoughnutLayout = this.doughnutWidgetSettingsForm.get('layout').value;
    const showLegend: boolean = this.doughnutWidgetSettingsForm.get('showLegend').value;
    const showTooltip: boolean = this.doughnutWidgetSettingsForm.get('showTooltip').value;

    const totalEnabled = layout === DoughnutLayout.with_total;

    if (showLegend) {
      this.doughnutWidgetSettingsForm.get('legendPosition').enable();
      this.doughnutWidgetSettingsForm.get('legendLabelFont').enable();
      this.doughnutWidgetSettingsForm.get('legendLabelColor').enable();
      this.doughnutWidgetSettingsForm.get('legendValueFont').enable();
      this.doughnutWidgetSettingsForm.get('legendValueColor').enable();
    } else {
      this.doughnutWidgetSettingsForm.get('legendPosition').disable();
      this.doughnutWidgetSettingsForm.get('legendLabelFont').disable();
      this.doughnutWidgetSettingsForm.get('legendLabelColor').disable();
      this.doughnutWidgetSettingsForm.get('legendValueFont').disable();
      this.doughnutWidgetSettingsForm.get('legendValueColor').disable();
    }
    if (showTooltip) {
      this.doughnutWidgetSettingsForm.get('tooltipValueType').enable();
      this.doughnutWidgetSettingsForm.get('tooltipValueDecimals').enable();
      this.doughnutWidgetSettingsForm.get('tooltipValueFont').enable();
      this.doughnutWidgetSettingsForm.get('tooltipValueColor').enable();
      this.doughnutWidgetSettingsForm.get('tooltipBackgroundColor').enable();
      this.doughnutWidgetSettingsForm.get('tooltipBackgroundBlur').enable();
    } else {
      this.doughnutWidgetSettingsForm.get('tooltipValueType').disable();
      this.doughnutWidgetSettingsForm.get('tooltipValueDecimals').disable();
      this.doughnutWidgetSettingsForm.get('tooltipValueFont').disable();
      this.doughnutWidgetSettingsForm.get('tooltipValueColor').disable();
      this.doughnutWidgetSettingsForm.get('tooltipBackgroundColor').disable();
      this.doughnutWidgetSettingsForm.get('tooltipBackgroundBlur').disable();
    }
    if (totalEnabled) {
      this.doughnutWidgetSettingsForm.get('totalValueFont').enable();
      this.doughnutWidgetSettingsForm.get('totalValueColor').enable();
    } else {
      this.doughnutWidgetSettingsForm.get('totalValueFont').disable();
      this.doughnutWidgetSettingsForm.get('totalValueColor').disable();
    }
  }

  private _valuePreviewFn(): string {
    const units: string = this.widgetConfig.config.units;
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(110, decimals, units, false);
  }

  private _tooltipValuePreviewFn(): string {
    const tooltipValueType: DoughnutTooltipValueType = this.doughnutWidgetSettingsForm.get('tooltipValueType').value;
    const decimals: number = this.doughnutWidgetSettingsForm.get('tooltipValueDecimals').value;
    if (tooltipValueType === DoughnutTooltipValueType.percentage) {
      return formatValue(35, decimals, '%', false);
    } else {
      const units: string = this.widgetConfig.config.units;
      return formatValue(110, decimals, units, false);
    }
  }

}
