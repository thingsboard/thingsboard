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

import { Directive, TemplateRef } from '@angular/core';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import {
  DataKey,
  Datasource,
  datasourcesHasAggregation,
  datasourcesHasOnlyComparisonAggregation,
  legendPositions,
  legendPositionTranslationMap,
  WidgetConfig
} from '@shared/models/widget.models';
import {
  LatestChartTooltipValueType,
  latestChartTooltipValueTypes,
  latestChartTooltipValueTypeTranslations,
  LatestChartWidgetSettings
} from '@home/components/widget/lib/chart/latest-chart.models';
import { formatValue, isDefinedAndNotNull, isUndefined, mergeDeep } from '@core/utils';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';
import { radarChartShapes, radarChartShapeTranslations } from '@home/components/widget/lib/chart/radar-chart.models';
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
import {
  DoughnutLayout,
  doughnutLayoutImages,
  doughnutLayouts,
  doughnutLayoutTranslations,
  horizontalDoughnutLayoutImages
} from '@home/components/widget/lib/chart/doughnut-widget.models';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

@Directive()
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export abstract class LatestChartBasicConfigComponent<S extends LatestChartWidgetSettings> extends BasicWidgetConfigComponent {

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.latestChartWidgetConfigForm.get('datasources').value;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  public get displayTimewindowConfig(): boolean {
    const datasources = this.latestChartWidgetConfigForm.get('datasources').value;
    return datasourcesHasAggregation(datasources);
  }

  public onlyHistoryTimewindow(): boolean {
    const datasources = this.latestChartWidgetConfigForm.get('datasources').value;
    return datasourcesHasOnlyComparisonAggregation(datasources);
  }

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

  latestChartWidgetConfigForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  get doughnutTotalEnabled(): boolean {
    const layout: DoughnutLayout = this.latestChartWidgetConfigForm.get('layout').value;
    return layout === DoughnutLayout.with_total;
  }

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              protected fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.latestChartWidgetConfigForm;
  }

  protected setupConfig(widgetConfig: WidgetConfigComponentData) {
    const params = widgetConfig.typeParameters as any;
    this.doughnutHorizontal = isDefinedAndNotNull(params.horizontal) ? params.horizontal : false;
    this.doughnutLayoutImageMap = this.doughnutHorizontal ? horizontalDoughnutLayoutImages : doughnutLayoutImages;
    super.setupConfig(widgetConfig);
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: S = mergeDeep<S>({} as S,
      this.defaultSettings(), configData.config.settings as S);

    this.latestChartWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      datasources: [configData.config.datasources, []],

      series: [this.getSeries(configData.config.datasources), []],

      showTitle: [configData.config.showTitle, []],
      title: [configData.config.title, []],
      titleFont: [configData.config.titleFont, []],
      titleColor: [configData.config.titleColor, []],
      showTitleIcon: [configData.config.showTitleIcon, []],
      titleIcon: [configData.config.titleIcon, []],
      iconColor: [configData.config.iconColor, []],

      sortSeries: [settings.sortSeries, []],

      units: [configData.config.units, []],
      decimals: [configData.config.decimals, []],

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

      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],
      padding: [settings.padding, []],

      actions: [configData.config.actions || {}, []]
    });
    this.setupLatestChartControls(this.latestChartWidgetConfigForm, settings);
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    setTimewindowConfig(this.widgetConfig.config, config.timewindowConfig);
    this.widgetConfig.config.datasources = config.datasources;
    this.setSeries(config.series, this.widgetConfig.config.datasources);

    this.widgetConfig.config.showTitle = config.showTitle;
    this.widgetConfig.config.title = config.title;
    this.widgetConfig.config.titleFont = config.titleFont;
    this.widgetConfig.config.titleColor = config.titleColor;

    this.widgetConfig.config.showTitleIcon = config.showTitleIcon;
    this.widgetConfig.config.titleIcon = config.titleIcon;
    this.widgetConfig.config.iconColor = config.iconColor;

    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};

    this.widgetConfig.config.settings.sortSeries = config.sortSeries;

    this.widgetConfig.config.units = config.units;
    this.widgetConfig.config.decimals = config.decimals;

    this.widgetConfig.config.settings.animation = config.animation;

    this.widgetConfig.config.settings.showLegend = config.showLegend;
    this.widgetConfig.config.settings.legendPosition = config.legendPosition;
    this.widgetConfig.config.settings.legendLabelFont = config.legendLabelFont;
    this.widgetConfig.config.settings.legendLabelColor = config.legendLabelColor;
    this.widgetConfig.config.settings.legendValueFont = config.legendValueFont;
    this.widgetConfig.config.settings.legendValueColor = config.legendValueColor;
    this.widgetConfig.config.settings.legendShowTotal = config.legendShowTotal;

    this.widgetConfig.config.settings.showTooltip = config.showTooltip;
    this.widgetConfig.config.settings.tooltipValueType = config.tooltipValueType;
    this.widgetConfig.config.settings.tooltipValueDecimals = config.tooltipValueDecimals;
    this.widgetConfig.config.settings.tooltipValueFont = config.tooltipValueFont;
    this.widgetConfig.config.settings.tooltipValueColor = config.tooltipValueColor;
    this.widgetConfig.config.settings.tooltipBackgroundColor = config.tooltipBackgroundColor;
    this.widgetConfig.config.settings.tooltipBackgroundBlur = config.tooltipBackgroundBlur;

    this.widgetConfig.config.settings.background = config.background;

    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;
    this.widgetConfig.config.settings.padding = config.padding;

    this.widgetConfig.config.actions = config.actions;

    this.prepareOutputLatestChartConfig(config);

    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showTitle', 'showTitleIcon', 'showLegend', 'showTooltip'].concat(this.latestChartValidatorTriggers());
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showTitle: boolean = this.latestChartWidgetConfigForm.get('showTitle').value;
    const showTitleIcon: boolean = this.latestChartWidgetConfigForm.get('showTitleIcon').value;
    const showLegend: boolean = this.latestChartWidgetConfigForm.get('showLegend').value;
    const showTooltip: boolean = this.latestChartWidgetConfigForm.get('showTooltip').value;

    if (showTitle) {
      this.latestChartWidgetConfigForm.get('title').enable();
      this.latestChartWidgetConfigForm.get('titleFont').enable();
      this.latestChartWidgetConfigForm.get('titleColor').enable();
      this.latestChartWidgetConfigForm.get('showTitleIcon').enable({emitEvent: false});
      if (showTitleIcon) {
        this.latestChartWidgetConfigForm.get('titleIcon').enable();
        this.latestChartWidgetConfigForm.get('iconColor').enable();
      } else {
        this.latestChartWidgetConfigForm.get('titleIcon').disable();
        this.latestChartWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.latestChartWidgetConfigForm.get('title').disable();
      this.latestChartWidgetConfigForm.get('titleFont').disable();
      this.latestChartWidgetConfigForm.get('titleColor').disable();
      this.latestChartWidgetConfigForm.get('showTitleIcon').disable({emitEvent: false});
      this.latestChartWidgetConfigForm.get('titleIcon').disable();
      this.latestChartWidgetConfigForm.get('iconColor').disable();
    }

    if (showLegend) {
      this.latestChartWidgetConfigForm.get('legendPosition').enable();
      this.latestChartWidgetConfigForm.get('legendLabelFont').enable();
      this.latestChartWidgetConfigForm.get('legendLabelColor').enable();
      this.latestChartWidgetConfigForm.get('legendValueFont').enable();
      this.latestChartWidgetConfigForm.get('legendValueColor').enable();
      this.latestChartWidgetConfigForm.get('legendShowTotal').enable();
    } else {
      this.latestChartWidgetConfigForm.get('legendPosition').disable();
      this.latestChartWidgetConfigForm.get('legendLabelFont').disable();
      this.latestChartWidgetConfigForm.get('legendLabelColor').disable();
      this.latestChartWidgetConfigForm.get('legendValueFont').disable();
      this.latestChartWidgetConfigForm.get('legendValueColor').disable();
      this.latestChartWidgetConfigForm.get('legendShowTotal').disable();
    }
    if (showTooltip) {
      this.latestChartWidgetConfigForm.get('tooltipValueType').enable();
      this.latestChartWidgetConfigForm.get('tooltipValueDecimals').enable();
      this.latestChartWidgetConfigForm.get('tooltipValueFont').enable();
      this.latestChartWidgetConfigForm.get('tooltipValueColor').enable();
      this.latestChartWidgetConfigForm.get('tooltipBackgroundColor').enable();
      this.latestChartWidgetConfigForm.get('tooltipBackgroundBlur').enable();
    } else {
      this.latestChartWidgetConfigForm.get('tooltipValueType').disable();
      this.latestChartWidgetConfigForm.get('tooltipValueDecimals').disable();
      this.latestChartWidgetConfigForm.get('tooltipValueFont').disable();
      this.latestChartWidgetConfigForm.get('tooltipValueColor').disable();
      this.latestChartWidgetConfigForm.get('tooltipBackgroundColor').disable();
      this.latestChartWidgetConfigForm.get('tooltipBackgroundBlur').disable();
    }
    this.updateLatestChartValidators(this.latestChartWidgetConfigForm, emitEvent, trigger);
  }

  protected setupLatestChartControls(latestChartWidgetConfigForm: UntypedFormGroup, settings: S) {}

  protected prepareOutputLatestChartConfig(config: any) {}

  protected latestChartValidatorTriggers(): string[] {
    return [];
  }

  protected updateLatestChartValidators(latestChartWidgetConfigForm: UntypedFormGroup, emitEvent: boolean, trigger?: string) {
  }

  protected abstract defaultSettings(): S;

  public abstract latestChartConfigTemplate(): TemplateRef<any>;

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

  private _valuePreviewFn(): string {
    const units: string = getSourceTbUnitSymbol(this.latestChartWidgetConfigForm.get('units').value);
    const decimals: number = this.latestChartWidgetConfigForm.get('decimals').value;
    return formatValue(110, decimals, units, false);
  }

  private _tooltipValuePreviewFn(): string {
    const tooltipValueType: LatestChartTooltipValueType = this.configForm().get('tooltipValueType').value;
    const decimals: number = this.latestChartWidgetConfigForm.get('tooltipValueDecimals').value;
    if (tooltipValueType === LatestChartTooltipValueType.percentage) {
      return formatValue(35, decimals, '%', false);
    } else {
      const units: string = getSourceTbUnitSymbol(this.latestChartWidgetConfigForm.get('units').value);
      return formatValue(110, decimals, units, false);
    }
  }
}
