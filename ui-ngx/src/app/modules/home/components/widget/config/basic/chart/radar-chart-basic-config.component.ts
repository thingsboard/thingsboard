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
  radarChartWidgetDefaultSettings,
  RadarChartWidgetSettings
} from '@home/components/widget/lib/chart/radar-chart-widget.models';
import { radarChartShapes, radarChartShapeTranslations } from '@home/components/widget/lib/chart/radar-chart.models';
import {
  chartLabelPositions,
  chartLabelPositionTranslations,
  chartLineTypes,
  chartLineTypeTranslations,
  chartShapes,
  chartShapeTranslations
} from '@home/components/widget/lib/chart/chart.models';

@Component({
  selector: 'tb-radar-chart-basic-config',
  templateUrl: './radar-chart-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class RadarChartBasicConfigComponent extends BasicWidgetConfigComponent {

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.radarChartWidgetConfigForm.get('datasources').value;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  public get displayTimewindowConfig(): boolean {
    const datasources = this.radarChartWidgetConfigForm.get('datasources').value;
    return datasourcesHasAggregation(datasources);
  }

  public onlyHistoryTimewindow(): boolean {
    const datasources = this.radarChartWidgetConfigForm.get('datasources').value;
    return datasourcesHasOnlyComparisonAggregation(datasources);
  }

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

  radarChartWidgetConfigForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.radarChartWidgetConfigForm;
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{name: 'windPower', label: 'Wind', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#08872B'},
      {name: 'solarPower', label: 'Solar', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#FF4D5A'},
      {name: 'hydroelectricPower', label: 'Hydroelectric', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#FFDE30'}];
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: RadarChartWidgetSettings = mergeDeep<RadarChartWidgetSettings>({} as RadarChartWidgetSettings,
      radarChartWidgetDefaultSettings, configData.config.settings as RadarChartWidgetSettings);
    this.radarChartWidgetConfigForm = this.fb.group({
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

      shape: [settings.shape, []],
      color: [settings.color, []],
      showLine: [settings.showLine, []],
      lineType: [settings.lineType, []],
      lineWidth: [settings.lineWidth, [Validators.min(0)]],
      showPoints: [settings.showPoints, []],
      pointShape: [settings.pointShape, []],
      pointSize: [settings.pointSize, [Validators.min(0)]],
      showLabel: [settings.showLabel, []],
      labelPosition: [settings.labelPosition, []],
      labelFont: [settings.labelFont, []],
      labelColor: [settings.labelColor, []],
      fillAreaSettings: [settings.fillAreaSettings, []],

      axisShowLabel: [settings.axisShowLabel, []],
      axisLabelFont: [settings.axisLabelFont, []],
      axisShowTickLabels: [settings.axisShowTickLabels, []],
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

    this.widgetConfig.config.settings.shape = config.shape;
    this.widgetConfig.config.settings.color = config.color;
    this.widgetConfig.config.settings.showLine = config.showLine;
    this.widgetConfig.config.settings.lineType = config.lineType;
    this.widgetConfig.config.settings.lineWidth = config.lineWidth;
    this.widgetConfig.config.settings.showPoints = config.showPoints;
    this.widgetConfig.config.settings.pointShape = config.pointShape;
    this.widgetConfig.config.settings.pointSize = config.pointSize;
    this.widgetConfig.config.settings.showLabel = config.showLabel;
    this.widgetConfig.config.settings.labelPosition = config.labelPosition;
    this.widgetConfig.config.settings.labelFont = config.labelFont;
    this.widgetConfig.config.settings.labelColor = config.labelColor;
    this.widgetConfig.config.settings.fillAreaSettings = config.fillAreaSettings;

    this.widgetConfig.config.settings.axisShowLabel = config.axisShowLabel;
    this.widgetConfig.config.settings.axisLabelFont = config.axisLabelFont;
    this.widgetConfig.config.settings.axisShowTickLabels = config.axisShowTickLabels;
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
    return ['showTitle', 'showTitleIcon', 'showLine', 'showPoints', 'showLabel', 'axisShowLabel',
      'axisShowTickLabels', 'showLegend', 'showTooltip'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showTitle: boolean = this.radarChartWidgetConfigForm.get('showTitle').value;
    const showTitleIcon: boolean = this.radarChartWidgetConfigForm.get('showTitleIcon').value;
    const showLine: boolean = this.radarChartWidgetConfigForm.get('showLine').value;
    const showPoints: boolean = this.radarChartWidgetConfigForm.get('showPoints').value;
    const showLabel: boolean = this.radarChartWidgetConfigForm.get('showLabel').value;
    const axisShowLabel: boolean = this.radarChartWidgetConfigForm.get('axisShowLabel').value;
    const axisShowTickLabels: boolean = this.radarChartWidgetConfigForm.get('axisShowTickLabels').value;
    const showLegend: boolean = this.radarChartWidgetConfigForm.get('showLegend').value;
    const showTooltip: boolean = this.radarChartWidgetConfigForm.get('showTooltip').value;

    if (showTitle) {
      this.radarChartWidgetConfigForm.get('title').enable();
      this.radarChartWidgetConfigForm.get('titleFont').enable();
      this.radarChartWidgetConfigForm.get('titleColor').enable();
      this.radarChartWidgetConfigForm.get('showTitleIcon').enable({emitEvent: false});
      if (showTitleIcon) {
        this.radarChartWidgetConfigForm.get('titleIcon').enable();
        this.radarChartWidgetConfigForm.get('iconColor').enable();
      } else {
        this.radarChartWidgetConfigForm.get('titleIcon').disable();
        this.radarChartWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.radarChartWidgetConfigForm.get('title').disable();
      this.radarChartWidgetConfigForm.get('titleFont').disable();
      this.radarChartWidgetConfigForm.get('titleColor').disable();
      this.radarChartWidgetConfigForm.get('showTitleIcon').disable({emitEvent: false});
      this.radarChartWidgetConfigForm.get('titleIcon').disable();
      this.radarChartWidgetConfigForm.get('iconColor').disable();
    }

    if (showLine) {
      this.radarChartWidgetConfigForm.get('lineType').enable();
      this.radarChartWidgetConfigForm.get('lineWidth').enable();
    } else {
      this.radarChartWidgetConfigForm.get('lineType').disable();
      this.radarChartWidgetConfigForm.get('lineWidth').disable();
    }

    if (showPoints) {
      this.radarChartWidgetConfigForm.get('pointShape').enable();
      this.radarChartWidgetConfigForm.get('pointSize').enable();
    } else {
      this.radarChartWidgetConfigForm.get('pointShape').disable();
      this.radarChartWidgetConfigForm.get('pointSize').disable();
    }

    if (showLabel) {
      this.radarChartWidgetConfigForm.get('labelPosition').enable();
      this.radarChartWidgetConfigForm.get('labelFont').enable();
      this.radarChartWidgetConfigForm.get('labelColor').enable();
    } else {
      this.radarChartWidgetConfigForm.get('labelPosition').disable();
      this.radarChartWidgetConfigForm.get('labelFont').disable();
      this.radarChartWidgetConfigForm.get('labelColor').disable();
    }

    if (axisShowLabel) {
      this.radarChartWidgetConfigForm.get('axisLabelFont').enable();
    } else {
      this.radarChartWidgetConfigForm.get('axisLabelFont').disable();
    }

    if (axisShowTickLabels) {
      this.radarChartWidgetConfigForm.get('axisTickLabelFont').enable();
      this.radarChartWidgetConfigForm.get('axisTickLabelColor').enable();
    } else {
      this.radarChartWidgetConfigForm.get('axisTickLabelFont').disable();
      this.radarChartWidgetConfigForm.get('axisTickLabelColor').disable();
    }

    if (showLegend) {
      this.radarChartWidgetConfigForm.get('legendPosition').enable();
      this.radarChartWidgetConfigForm.get('legendLabelFont').enable();
      this.radarChartWidgetConfigForm.get('legendLabelColor').enable();
      this.radarChartWidgetConfigForm.get('legendValueFont').enable();
      this.radarChartWidgetConfigForm.get('legendValueColor').enable();
    } else {
      this.radarChartWidgetConfigForm.get('legendPosition').disable();
      this.radarChartWidgetConfigForm.get('legendLabelFont').disable();
      this.radarChartWidgetConfigForm.get('legendLabelColor').disable();
      this.radarChartWidgetConfigForm.get('legendValueFont').disable();
      this.radarChartWidgetConfigForm.get('legendValueColor').disable();
    }
    if (showTooltip) {
      this.radarChartWidgetConfigForm.get('tooltipValueType').enable();
      this.radarChartWidgetConfigForm.get('tooltipValueDecimals').enable();
      this.radarChartWidgetConfigForm.get('tooltipValueFont').enable();
      this.radarChartWidgetConfigForm.get('tooltipValueColor').enable();
      this.radarChartWidgetConfigForm.get('tooltipBackgroundColor').enable();
      this.radarChartWidgetConfigForm.get('tooltipBackgroundBlur').enable();
    } else {
      this.radarChartWidgetConfigForm.get('tooltipValueType').disable();
      this.radarChartWidgetConfigForm.get('tooltipValueDecimals').disable();
      this.radarChartWidgetConfigForm.get('tooltipValueFont').disable();
      this.radarChartWidgetConfigForm.get('tooltipValueColor').disable();
      this.radarChartWidgetConfigForm.get('tooltipBackgroundColor').disable();
      this.radarChartWidgetConfigForm.get('tooltipBackgroundBlur').disable();
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
    const units: string = this.radarChartWidgetConfigForm.get('units').value;
    const decimals: number = this.radarChartWidgetConfigForm.get('decimals').value;
    return formatValue(110, decimals, units, false);
  }

  private _tooltipValuePreviewFn(): string {
    const tooltipValueType: LatestChartTooltipValueType = this.radarChartWidgetConfigForm.get('tooltipValueType').value;
    const decimals: number = this.radarChartWidgetConfigForm.get('tooltipValueDecimals').value;
    if (tooltipValueType === LatestChartTooltipValueType.percentage) {
      return formatValue(35, decimals, '%', false);
    } else {
      const units: string = this.radarChartWidgetConfigForm.get('units').value;
      return formatValue(110, decimals, units, false);
    }
  }
}
