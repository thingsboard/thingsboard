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
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import {
  DataKey,
  Datasource,
  datasourcesHasAggregation,
  datasourcesHasOnlyComparisonAggregation,
  legendPositions,
  legendPositionTranslationMap,
  WidgetConfig
} from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { formatValue, isUndefined, mergeDeep } from '@core/utils';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';
import {
  LatestChartTooltipValueType,
  latestChartTooltipValueTypes,
  latestChartTooltipValueTypeTranslations
} from '@home/components/widget/lib/chart/latest-chart.models';
import {
  barChartWidgetDefaultSettings,
  BarChartWidgetSettings
} from '@home/components/widget/lib/chart/bar-chart-widget.models';

@Component({
  selector: 'tb-bar-chart-basic-config',
  templateUrl: './bar-chart-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class BarChartBasicConfigComponent extends BasicWidgetConfigComponent {

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.barChartWidgetConfigForm.get('datasources').value;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  public get displayTimewindowConfig(): boolean {
    const datasources = this.barChartWidgetConfigForm.get('datasources').value;
    return datasourcesHasAggregation(datasources);
  }

  public onlyHistoryTimewindow(): boolean {
    const datasources = this.barChartWidgetConfigForm.get('datasources').value;
    return datasourcesHasOnlyComparisonAggregation(datasources);
  }

  legendPositions = legendPositions;

  legendPositionTranslationMap = legendPositionTranslationMap;

  latestChartTooltipValueTypes = latestChartTooltipValueTypes;

  latestChartTooltipValueTypeTranslationMap = latestChartTooltipValueTypeTranslations;

  barChartWidgetConfigForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.barChartWidgetConfigForm;
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{ name: 'windPower', label: 'Wind', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#08872B' },
      { name: 'solarPower', label: 'Solar', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#FF4D5A' },
      { name: 'hydroelectricPower', label: 'Hydroelectric', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#FFDE30' }];
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: BarChartWidgetSettings = mergeDeep<BarChartWidgetSettings>({} as BarChartWidgetSettings,
      barChartWidgetDefaultSettings, configData.config.settings as BarChartWidgetSettings);
    this.barChartWidgetConfigForm = this.fb.group({
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

      barSettings: [settings.barSettings, []],

      axisMin: [settings.axisMin, []],
      axisMax: [settings.axisMax, []],
      axisTickLabelFont: [settings.axisTickLabelFont, []],
      axisTickLabelColor: [settings.axisTickLabelColor, []],

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

      background: [settings.background, []],

      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],

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

    this.widgetConfig.config.showTitleIcon = config.showTitleIcon;
    this.widgetConfig.config.titleIcon = config.titleIcon;
    this.widgetConfig.config.iconColor = config.iconColor;

    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};

    this.widgetConfig.config.settings.sortSeries = config.sortSeries;

    this.widgetConfig.config.units = config.units;
    this.widgetConfig.config.decimals = config.decimals;

    this.widgetConfig.config.settings.barSettings = config.barSettings;

    this.widgetConfig.config.settings.axisMin = config.axisMin;
    this.widgetConfig.config.settings.axisMax = config.axisMax;
    this.widgetConfig.config.settings.axisTickLabelFont = config.axisTickLabelFont;
    this.widgetConfig.config.settings.axisTickLabelColor = config.axisTickLabelColor;

    this.widgetConfig.config.settings.animation = config.animation;

    this.widgetConfig.config.settings.showLegend = config.showLegend;
    this.widgetConfig.config.settings.legendPosition = config.legendPosition;
    this.widgetConfig.config.settings.legendLabelFont = config.legendLabelFont;
    this.widgetConfig.config.settings.legendLabelColor = config.legendLabelColor;
    this.widgetConfig.config.settings.legendValueFont = config.legendValueFont;
    this.widgetConfig.config.settings.legendValueColor = config.legendValueColor;

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

    this.widgetConfig.config.actions = config.actions;

    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showTitle', 'showTitleIcon', 'showLegend', 'showTooltip'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showTitle: boolean = this.barChartWidgetConfigForm.get('showTitle').value;
    const showTitleIcon: boolean = this.barChartWidgetConfigForm.get('showTitleIcon').value;
    const showLegend: boolean = this.barChartWidgetConfigForm.get('showLegend').value;
    const showTooltip: boolean = this.barChartWidgetConfigForm.get('showTooltip').value;

    if (showTitle) {
      this.barChartWidgetConfigForm.get('title').enable();
      this.barChartWidgetConfigForm.get('titleFont').enable();
      this.barChartWidgetConfigForm.get('titleColor').enable();
      this.barChartWidgetConfigForm.get('showTitleIcon').enable({emitEvent: false});
      if (showTitleIcon) {
        this.barChartWidgetConfigForm.get('titleIcon').enable();
        this.barChartWidgetConfigForm.get('iconColor').enable();
      } else {
        this.barChartWidgetConfigForm.get('titleIcon').disable();
        this.barChartWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.barChartWidgetConfigForm.get('title').disable();
      this.barChartWidgetConfigForm.get('titleFont').disable();
      this.barChartWidgetConfigForm.get('titleColor').disable();
      this.barChartWidgetConfigForm.get('showTitleIcon').disable({emitEvent: false});
      this.barChartWidgetConfigForm.get('titleIcon').disable();
      this.barChartWidgetConfigForm.get('iconColor').disable();
    }
    if (showLegend) {
      this.barChartWidgetConfigForm.get('legendPosition').enable();
      this.barChartWidgetConfigForm.get('legendLabelFont').enable();
      this.barChartWidgetConfigForm.get('legendLabelColor').enable();
      this.barChartWidgetConfigForm.get('legendValueFont').enable();
      this.barChartWidgetConfigForm.get('legendValueColor').enable();
    } else {
      this.barChartWidgetConfigForm.get('legendPosition').disable();
      this.barChartWidgetConfigForm.get('legendLabelFont').disable();
      this.barChartWidgetConfigForm.get('legendLabelColor').disable();
      this.barChartWidgetConfigForm.get('legendValueFont').disable();
      this.barChartWidgetConfigForm.get('legendValueColor').disable();
    }
    if (showTooltip) {
      this.barChartWidgetConfigForm.get('tooltipValueType').enable();
      this.barChartWidgetConfigForm.get('tooltipValueDecimals').enable();
      this.barChartWidgetConfigForm.get('tooltipValueFont').enable();
      this.barChartWidgetConfigForm.get('tooltipValueColor').enable();
      this.barChartWidgetConfigForm.get('tooltipBackgroundColor').enable();
      this.barChartWidgetConfigForm.get('tooltipBackgroundBlur').enable();
    } else {
      this.barChartWidgetConfigForm.get('tooltipValueType').disable();
      this.barChartWidgetConfigForm.get('tooltipValueDecimals').disable();
      this.barChartWidgetConfigForm.get('tooltipValueFont').disable();
      this.barChartWidgetConfigForm.get('tooltipValueColor').disable();
      this.barChartWidgetConfigForm.get('tooltipBackgroundColor').disable();
      this.barChartWidgetConfigForm.get('tooltipBackgroundBlur').disable();
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
    const units: string = this.barChartWidgetConfigForm.get('units').value;
    const decimals: number = this.barChartWidgetConfigForm.get('decimals').value;
    return formatValue(110, decimals, units, false);
  }

  private _tooltipValuePreviewFn(): string {
    const tooltipValueType: LatestChartTooltipValueType = this.barChartWidgetConfigForm.get('tooltipValueType').value;
    const decimals: number = this.barChartWidgetConfigForm.get('tooltipValueDecimals').value;
    if (tooltipValueType === LatestChartTooltipValueType.percentage) {
      return formatValue(35, decimals, '%', false);
    } else {
      const units: string = this.barChartWidgetConfigForm.get('units').value;
      return formatValue(110, decimals, units, false);
    }
  }
}
