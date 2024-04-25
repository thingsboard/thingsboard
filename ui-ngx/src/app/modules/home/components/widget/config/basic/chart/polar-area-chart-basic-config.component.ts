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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
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
  polarAreaChartWidgetDefaultSettings,
  PolarAreaChartWidgetSettings
} from '@home/components/widget/lib/chart/polar-area-widget.models';

@Component({
  selector: 'tb-polar-area-chart-basic-config',
  templateUrl: './polar-area-chart-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class PolarAreaChartBasicConfigComponent extends BasicWidgetConfigComponent {

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.polarAreaChartWidgetConfigForm.get('datasources').value;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  public get displayTimewindowConfig(): boolean {
    const datasources = this.polarAreaChartWidgetConfigForm.get('datasources').value;
    return datasourcesHasAggregation(datasources);
  }

  public onlyHistoryTimewindow(): boolean {
    const datasources = this.polarAreaChartWidgetConfigForm.get('datasources').value;
    return datasourcesHasOnlyComparisonAggregation(datasources);
  }

  legendPositions = legendPositions;

  legendPositionTranslationMap = legendPositionTranslationMap;

  latestChartTooltipValueTypes = latestChartTooltipValueTypes;

  latestChartTooltipValueTypeTranslationMap = latestChartTooltipValueTypeTranslations;

  polarAreaChartWidgetConfigForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.polarAreaChartWidgetConfigForm;
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{ name: 'windPower', label: 'Wind', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#08872B' },
      { name: 'solarPower', label: 'Solar', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#FF4D5A' },
      { name: 'hydroelectricPower', label: 'Hydroelectric', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#FFDE30' }];
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: PolarAreaChartWidgetSettings = mergeDeep<PolarAreaChartWidgetSettings>({} as PolarAreaChartWidgetSettings,
      polarAreaChartWidgetDefaultSettings, configData.config.settings as PolarAreaChartWidgetSettings);
    this.polarAreaChartWidgetConfigForm = this.fb.group({
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
    this.widgetConfig.config.settings.angleAxisStartAngle = config.angleAxisStartAngle;

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
    const showTitle: boolean = this.polarAreaChartWidgetConfigForm.get('showTitle').value;
    const showTitleIcon: boolean = this.polarAreaChartWidgetConfigForm.get('showTitleIcon').value;
    const showLegend: boolean = this.polarAreaChartWidgetConfigForm.get('showLegend').value;
    const showTooltip: boolean = this.polarAreaChartWidgetConfigForm.get('showTooltip').value;

    if (showTitle) {
      this.polarAreaChartWidgetConfigForm.get('title').enable();
      this.polarAreaChartWidgetConfigForm.get('titleFont').enable();
      this.polarAreaChartWidgetConfigForm.get('titleColor').enable();
      this.polarAreaChartWidgetConfigForm.get('showTitleIcon').enable({emitEvent: false});
      if (showTitleIcon) {
        this.polarAreaChartWidgetConfigForm.get('titleIcon').enable();
        this.polarAreaChartWidgetConfigForm.get('iconColor').enable();
      } else {
        this.polarAreaChartWidgetConfigForm.get('titleIcon').disable();
        this.polarAreaChartWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.polarAreaChartWidgetConfigForm.get('title').disable();
      this.polarAreaChartWidgetConfigForm.get('titleFont').disable();
      this.polarAreaChartWidgetConfigForm.get('titleColor').disable();
      this.polarAreaChartWidgetConfigForm.get('showTitleIcon').disable({emitEvent: false});
      this.polarAreaChartWidgetConfigForm.get('titleIcon').disable();
      this.polarAreaChartWidgetConfigForm.get('iconColor').disable();
    }
    if (showLegend) {
      this.polarAreaChartWidgetConfigForm.get('legendPosition').enable();
      this.polarAreaChartWidgetConfigForm.get('legendLabelFont').enable();
      this.polarAreaChartWidgetConfigForm.get('legendLabelColor').enable();
      this.polarAreaChartWidgetConfigForm.get('legendValueFont').enable();
      this.polarAreaChartWidgetConfigForm.get('legendValueColor').enable();
    } else {
      this.polarAreaChartWidgetConfigForm.get('legendPosition').disable();
      this.polarAreaChartWidgetConfigForm.get('legendLabelFont').disable();
      this.polarAreaChartWidgetConfigForm.get('legendLabelColor').disable();
      this.polarAreaChartWidgetConfigForm.get('legendValueFont').disable();
      this.polarAreaChartWidgetConfigForm.get('legendValueColor').disable();
    }
    if (showTooltip) {
      this.polarAreaChartWidgetConfigForm.get('tooltipValueType').enable();
      this.polarAreaChartWidgetConfigForm.get('tooltipValueDecimals').enable();
      this.polarAreaChartWidgetConfigForm.get('tooltipValueFont').enable();
      this.polarAreaChartWidgetConfigForm.get('tooltipValueColor').enable();
      this.polarAreaChartWidgetConfigForm.get('tooltipBackgroundColor').enable();
      this.polarAreaChartWidgetConfigForm.get('tooltipBackgroundBlur').enable();
    } else {
      this.polarAreaChartWidgetConfigForm.get('tooltipValueType').disable();
      this.polarAreaChartWidgetConfigForm.get('tooltipValueDecimals').disable();
      this.polarAreaChartWidgetConfigForm.get('tooltipValueFont').disable();
      this.polarAreaChartWidgetConfigForm.get('tooltipValueColor').disable();
      this.polarAreaChartWidgetConfigForm.get('tooltipBackgroundColor').disable();
      this.polarAreaChartWidgetConfigForm.get('tooltipBackgroundBlur').disable();
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
    const units: string = this.polarAreaChartWidgetConfigForm.get('units').value;
    const decimals: number = this.polarAreaChartWidgetConfigForm.get('decimals').value;
    return formatValue(110, decimals, units, false);
  }

  private _tooltipValuePreviewFn(): string {
    const tooltipValueType: LatestChartTooltipValueType = this.polarAreaChartWidgetConfigForm.get('tooltipValueType').value;
    const decimals: number = this.polarAreaChartWidgetConfigForm.get('tooltipValueDecimals').value;
    if (tooltipValueType === LatestChartTooltipValueType.percentage) {
      return formatValue(35, decimals, '%', false);
    } else {
      const units: string = this.polarAreaChartWidgetConfigForm.get('units').value;
      return formatValue(110, decimals, units, false);
    }
  }
}
