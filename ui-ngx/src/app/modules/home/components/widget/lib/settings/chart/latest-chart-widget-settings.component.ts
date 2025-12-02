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

import { Directive, TemplateRef } from '@angular/core';
import {
  LatestChartTooltipValueType,
  latestChartTooltipValueTypes,
  latestChartTooltipValueTypeTranslations,
  LatestChartWidgetSettings
} from '@home/components/widget/lib/chart/latest-chart.models';
import {
  Datasource,
  legendPositions,
  legendPositionTranslationMap,
  WidgetSettings,
  WidgetSettingsComponent
} from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import {
  DoughnutLayout,
  doughnutLayoutImages,
  doughnutLayouts,
  doughnutLayoutTranslations,
  horizontalDoughnutLayoutImages
} from '@home/components/widget/lib/chart/doughnut-widget.models';
import {
  chartLabelPositions,
  chartLabelPositionTranslations,
  chartLineTypes,
  chartLineTypeTranslations,
  chartShapes,
  chartShapeTranslations,
  pieChartLabelPositions,
  pieChartLabelPositionTranslations
} from '@home/components/widget/lib/chart/chart.models';
import { radarChartShapes, radarChartShapeTranslations } from '@home/components/widget/lib/chart/radar-chart.models';
import { formatValue, isDefinedAndNotNull } from '@core/utils';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

@Directive()
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export abstract class LatestChartWidgetSettingsComponent<S extends LatestChartWidgetSettings> extends WidgetSettingsComponent {

  doughnutLayouts = doughnutLayouts;

  doughnutLayoutTranslationMap = doughnutLayoutTranslations;

  doughnutHorizontal = false;

  doughnutLayoutImageMap: Map<DoughnutLayout, string>;

  pieChartLabelPositions = pieChartLabelPositions;

  pieChartLabelPositionTranslationMap = pieChartLabelPositionTranslations;

  radarChartShapes = radarChartShapes;

  radarChartShapeTranslations = radarChartShapeTranslations;

  chartLineTypes = chartLineTypes;

  chartLineTypeTranslations = chartLineTypeTranslations;

  chartShapes = chartShapes;

  chartShapeTranslations = chartShapeTranslations;

  chartLabelPositions = chartLabelPositions;

  chartLabelPositionTranslations = chartLabelPositionTranslations;

  legendPositions = legendPositions;

  legendPositionTranslationMap = legendPositionTranslationMap;

  latestChartTooltipValueTypes = latestChartTooltipValueTypes;

  latestChartTooltipValueTypeTranslationMap = latestChartTooltipValueTypeTranslations;

  latestChartWidgetSettingsForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  get doughnutTotalEnabled(): boolean {
    const layout: DoughnutLayout = this.latestChartWidgetSettingsForm.get('layout').value;
    return layout === DoughnutLayout.with_total;
  }

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.widgetConfig.config.datasources;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  constructor(protected store: Store<AppState>,
              protected fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.latestChartWidgetSettingsForm;
  }

  protected onWidgetConfigSet(widgetConfig: WidgetConfigComponentData) {
    const params = widgetConfig.typeParameters as any;
    this.doughnutHorizontal  = isDefinedAndNotNull(params.horizontal) ? params.horizontal : false;
    this.doughnutLayoutImageMap = this.doughnutHorizontal ? horizontalDoughnutLayoutImages : doughnutLayoutImages;
  }

  protected defaultSettings(): WidgetSettings {
    return this.defaultLatestChartSettings();
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.latestChartWidgetSettingsForm = this.fb.group({

      sortSeries: [settings.sortSeries, []],

      animation: [settings.animation, []],

      showLegend: [settings.showLegend, []],
      legendPosition: [settings.legendPosition, []],
      legendLabelFont: [settings.legendLabelFont, []],
      legendLabelColor: [settings.legendLabelColor, []],
      legendValueFont: [settings.legendValueFont, []],
      legendValueColor: [settings.legendValueColor, []],
      legendShowTotal: [settings.legendShowTotal, []],

      showTooltip: [settings.showTooltip, []],
      tooltipValueType: [settings.tooltipValueType, []],
      tooltipValueDecimals: [settings.tooltipValueDecimals, []],
      tooltipValueFont: [settings.tooltipValueFont, []],
      tooltipValueColor: [settings.tooltipValueColor, []],
      tooltipBackgroundColor: [settings.tooltipBackgroundColor, []],
      tooltipBackgroundBlur: [settings.tooltipBackgroundBlur, []],

      background: [settings.background, []],
      padding: [settings.padding, []]
    });
    this.setupLatestChartControls(this.latestChartWidgetSettingsForm, settings);
  }

  protected validatorTriggers(): string[] {
    return ['showLegend', 'showTooltip'].concat(this.latestChartValidatorTriggers());
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showLegend: boolean = this.latestChartWidgetSettingsForm.get('showLegend').value;
    const showTooltip: boolean = this.latestChartWidgetSettingsForm.get('showTooltip').value;

    if (showLegend) {
      this.latestChartWidgetSettingsForm.get('legendPosition').enable();
      this.latestChartWidgetSettingsForm.get('legendLabelFont').enable();
      this.latestChartWidgetSettingsForm.get('legendLabelColor').enable();
      this.latestChartWidgetSettingsForm.get('legendValueFont').enable();
      this.latestChartWidgetSettingsForm.get('legendValueColor').enable();
      this.latestChartWidgetSettingsForm.get('legendShowTotal').enable();
    } else {
      this.latestChartWidgetSettingsForm.get('legendPosition').disable();
      this.latestChartWidgetSettingsForm.get('legendLabelFont').disable();
      this.latestChartWidgetSettingsForm.get('legendLabelColor').disable();
      this.latestChartWidgetSettingsForm.get('legendValueFont').disable();
      this.latestChartWidgetSettingsForm.get('legendValueColor').disable();
      this.latestChartWidgetSettingsForm.get('legendShowTotal').disable();
    }
    if (showTooltip) {
      this.latestChartWidgetSettingsForm.get('tooltipValueType').enable();
      this.latestChartWidgetSettingsForm.get('tooltipValueDecimals').enable();
      this.latestChartWidgetSettingsForm.get('tooltipValueFont').enable();
      this.latestChartWidgetSettingsForm.get('tooltipValueColor').enable();
      this.latestChartWidgetSettingsForm.get('tooltipBackgroundColor').enable();
      this.latestChartWidgetSettingsForm.get('tooltipBackgroundBlur').enable();
    } else {
      this.latestChartWidgetSettingsForm.get('tooltipValueType').disable();
      this.latestChartWidgetSettingsForm.get('tooltipValueDecimals').disable();
      this.latestChartWidgetSettingsForm.get('tooltipValueFont').disable();
      this.latestChartWidgetSettingsForm.get('tooltipValueColor').disable();
      this.latestChartWidgetSettingsForm.get('tooltipBackgroundColor').disable();
      this.latestChartWidgetSettingsForm.get('tooltipBackgroundBlur').disable();
    }
    this.updateLatestChartValidators(this.latestChartWidgetSettingsForm, emitEvent, trigger);
  }

  protected setupLatestChartControls(latestChartWidgetSettingsForm: UntypedFormGroup, settings: WidgetSettings) {}

  protected latestChartValidatorTriggers(): string[] {
    return [];
  }

  protected updateLatestChartValidators(latestChartWidgetSettingsForm: UntypedFormGroup, emitEvent: boolean, trigger?: string) {
  }

  protected abstract defaultLatestChartSettings(): S;

  public abstract latestChartConfigTemplate(): TemplateRef<any>;

  private _valuePreviewFn(): string {
    const units = getSourceTbUnitSymbol(this.widgetConfig.config.units);
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(110, decimals, units, false);
  }

  private _tooltipValuePreviewFn(): string {
    const tooltipValueType: LatestChartTooltipValueType = this.latestChartWidgetSettingsForm.get('tooltipValueType').value;
    const decimals: number = this.latestChartWidgetSettingsForm.get('tooltipValueDecimals').value;
    if (tooltipValueType === LatestChartTooltipValueType.percentage) {
      return formatValue(35, decimals, '%', false);
    } else {
      const units = getSourceTbUnitSymbol(this.widgetConfig.config.units);
      return formatValue(110, decimals, units, false);
    }
  }
}
