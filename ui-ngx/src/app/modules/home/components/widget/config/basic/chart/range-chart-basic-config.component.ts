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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import {
  DataKey,
  Datasource,
  legendPositions,
  legendPositionTranslationMap,
  WidgetConfig,
  widgetTitleAutocompleteValues,
} from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';
import { formatValue, isUndefined, mergeDeepIgnoreArray } from '@core/utils';
import {
  cssSizeToStrSize,
  DateFormatProcessor,
  DateFormatSettings,
  resolveCssSize
} from '@shared/models/widget-settings.models';
import {
  rangeChartDefaultSettings,
  RangeChartWidgetSettings
} from '@home/components/widget/lib/chart/range-chart-widget.models';
import {
  lineSeriesStepTypes,
  lineSeriesStepTypeTranslations
} from '@home/components/widget/lib/chart/time-series-chart.models';
import {
  chartLabelPositions,
  chartLabelPositionTranslations,
  chartLineTypes,
  chartLineTypeTranslations,
  chartShapes,
  chartShapeTranslations
} from '@home/components/widget/lib/chart/chart.models';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

@Component({
  selector: 'tb-range-chart-basic-config',
  templateUrl: './range-chart-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class RangeChartBasicConfigComponent extends BasicWidgetConfigComponent {

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.rangeChartWidgetConfigForm.get('datasources').value;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  lineSeriesStepTypes = lineSeriesStepTypes;

  lineSeriesStepTypeTranslations = lineSeriesStepTypeTranslations;

  chartLineTypes = chartLineTypes;

  chartLineTypeTranslations = chartLineTypeTranslations;

  chartLabelPositions = chartLabelPositions;

  chartLabelPositionTranslations = chartLabelPositionTranslations;

  chartShapes = chartShapes;

  chartShapeTranslations = chartShapeTranslations;

  legendPositions = legendPositions;

  legendPositionTranslationMap = legendPositionTranslationMap;

  rangeChartWidgetConfigForm: UntypedFormGroup;

  pointLabelPreviewFn = this._pointLabelPreviewFn.bind(this);

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  tooltipDatePreviewFn = this._tooltipDatePreviewFn.bind(this);

  predefinedValues = widgetTitleAutocompleteValues;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.rangeChartWidgetConfigForm;
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{ name: 'temperature', label: 'Temperature', type: DataKeyType.timeseries }];
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: RangeChartWidgetSettings = mergeDeepIgnoreArray<RangeChartWidgetSettings>({} as RangeChartWidgetSettings,
      rangeChartDefaultSettings, configData.config.settings as RangeChartWidgetSettings);
    const iconSize = resolveCssSize(configData.config.iconSize);
    this.rangeChartWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      datasources: [configData.config.datasources, []],

      showTitle: [configData.config.showTitle, []],
      title: [configData.config.title, []],
      titleFont: [configData.config.titleFont, []],
      titleColor: [configData.config.titleColor, []],

      showIcon: [configData.config.showTitleIcon, []],
      iconSize: [iconSize[0], [Validators.min(0)]],
      iconSizeUnit: [iconSize[1], []],
      icon: [configData.config.titleIcon, []],
      iconColor: [configData.config.iconColor, []],

      dataZoom: [settings.dataZoom, []],

      units: [configData.config.units, []],
      decimals: [configData.config.decimals, []],
      rangeColors: [settings.rangeColors, []],
      outOfRangeColor: [settings.outOfRangeColor, []],
      showRangeThresholds: [settings.showRangeThresholds, []],
      rangeThreshold: [settings.rangeThreshold, []],
      fillArea: [settings.fillArea, []],
      fillAreaOpacity: [settings.fillAreaOpacity, [Validators.min(0), Validators.max(1)]],

      showLine: [settings.showLine, []],
      step: [settings.step, []],
      stepType: [settings.stepType, []],
      smooth: [settings.smooth, []],
      lineType: [settings.lineType, []],
      lineWidth: [settings.lineWidth, [Validators.min(0)]],

      showPoints: [settings.showPoints, []],
      showPointLabel: [settings.showPointLabel, []],
      pointLabelPosition: [settings.pointLabelPosition, []],
      pointLabelFont: [settings.pointLabelFont, []],
      pointLabelColor: [settings.pointLabelColor, []],
      enablePointLabelBackground: [settings.enablePointLabelBackground, []],
      pointLabelBackground: [settings.pointLabelBackground, []],
      pointShape: [settings.pointShape, []],
      pointSize: [settings.pointSize, [Validators.min(0)]],

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

      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],
      padding: [settings.padding, []],

      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    setTimewindowConfig(this.widgetConfig.config, config.timewindowConfig);
    this.widgetConfig.config.datasources = config.datasources;

    this.widgetConfig.config.showTitle = config.showTitle;
    this.widgetConfig.config.title = config.title;
    this.widgetConfig.config.titleFont = config.titleFont;
    this.widgetConfig.config.titleColor = config.titleColor;

    this.widgetConfig.config.showTitleIcon = config.showIcon;
    this.widgetConfig.config.iconSize = cssSizeToStrSize(config.iconSize, config.iconSizeUnit);
    this.widgetConfig.config.titleIcon = config.icon;
    this.widgetConfig.config.iconColor = config.iconColor;

    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};


    this.widgetConfig.config.settings.dataZoom = config.dataZoom;

    this.widgetConfig.config.units = config.units;
    this.widgetConfig.config.decimals = config.decimals;

    this.widgetConfig.config.settings.rangeColors = config.rangeColors;
    this.widgetConfig.config.settings.outOfRangeColor = config.outOfRangeColor;
    this.widgetConfig.config.settings.showRangeThresholds = config.showRangeThresholds;
    this.widgetConfig.config.settings.rangeThreshold = config.rangeThreshold;
    this.widgetConfig.config.settings.fillArea = config.fillArea;
    this.widgetConfig.config.settings.fillAreaOpacity = config.fillAreaOpacity;

    this.widgetConfig.config.settings.showLine = config.showLine;
    this.widgetConfig.config.settings.step = config.step;
    this.widgetConfig.config.settings.stepType = config.stepType;
    this.widgetConfig.config.settings.smooth = config.smooth;
    this.widgetConfig.config.settings.lineType = config.lineType;
    this.widgetConfig.config.settings.lineWidth = config.lineWidth;

    this.widgetConfig.config.settings.showPoints = config.showPoints;
    this.widgetConfig.config.settings.showPointLabel = config.showPointLabel;
    this.widgetConfig.config.settings.pointLabelPosition = config.pointLabelPosition;
    this.widgetConfig.config.settings.pointLabelFont = config.pointLabelFont;
    this.widgetConfig.config.settings.pointLabelColor = config.pointLabelColor;
    this.widgetConfig.config.settings.enablePointLabelBackground = config.enablePointLabelBackground;
    this.widgetConfig.config.settings.pointLabelBackground = config.pointLabelBackground;
    this.widgetConfig.config.settings.pointShape = config.pointShape;
    this.widgetConfig.config.settings.pointSize = config.pointSize;

    this.widgetConfig.config.settings.grid = config.grid;

    this.widgetConfig.config.settings.yAxis = config.yAxis;
    this.widgetConfig.config.settings.xAxis = config.xAxis;

    this.widgetConfig.config.settings.thresholds = config.thresholds;

    this.widgetConfig.config.settings.animation = config.animation;

    this.widgetConfig.config.settings.showLegend = config.showLegend;
    this.widgetConfig.config.settings.legendPosition = config.legendPosition;
    this.widgetConfig.config.settings.legendLabelFont = config.legendLabelFont;
    this.widgetConfig.config.settings.legendLabelColor = config.legendLabelColor;

    this.widgetConfig.config.settings.showTooltip = config.showTooltip;
    this.widgetConfig.config.settings.tooltipLabelFont = config.tooltipLabelFont;
    this.widgetConfig.config.settings.tooltipLabelColor = config.tooltipLabelColor;
    this.widgetConfig.config.settings.tooltipValueFont = config.tooltipValueFont;
    this.widgetConfig.config.settings.tooltipValueColor = config.tooltipValueColor;
    this.widgetConfig.config.settings.tooltipShowDate = config.tooltipShowDate;
    this.widgetConfig.config.settings.tooltipDateFormat = config.tooltipDateFormat;
    this.widgetConfig.config.settings.tooltipDateFont = config.tooltipDateFont;
    this.widgetConfig.config.settings.tooltipDateColor = config.tooltipDateColor;
    this.widgetConfig.config.settings.tooltipDateInterval = config.tooltipDateInterval;
    this.widgetConfig.config.settings.tooltipBackgroundColor = config.tooltipBackgroundColor;
    this.widgetConfig.config.settings.tooltipBackgroundBlur = config.tooltipBackgroundBlur;

    this.widgetConfig.config.settings.background = config.background;

    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;
    this.widgetConfig.config.settings.padding = config.padding;

    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showTitle', 'showIcon', 'showRangeThresholds', 'fillArea', 'showLine',
      'step', 'showPointLabel', 'enablePointLabelBackground', 'showLegend', 'showTooltip', 'tooltipShowDate'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showTitle: boolean = this.rangeChartWidgetConfigForm.get('showTitle').value;
    const showIcon: boolean = this.rangeChartWidgetConfigForm.get('showIcon').value;
    const showRangeThresholds: boolean = this.rangeChartWidgetConfigForm.get('showRangeThresholds').value;
    const fillArea: boolean = this.rangeChartWidgetConfigForm.get('fillArea').value;
    const showLine: boolean = this.rangeChartWidgetConfigForm.get('showLine').value;
    const step: boolean = this.rangeChartWidgetConfigForm.get('step').value;
    const showPointLabel: boolean = this.rangeChartWidgetConfigForm.get('showPointLabel').value;
    const enablePointLabelBackground: boolean = this.rangeChartWidgetConfigForm.get('enablePointLabelBackground').value;
    const showLegend: boolean = this.rangeChartWidgetConfigForm.get('showLegend').value;
    const showTooltip: boolean = this.rangeChartWidgetConfigForm.get('showTooltip').value;
    const tooltipShowDate: boolean = this.rangeChartWidgetConfigForm.get('tooltipShowDate').value;

    if (showTitle) {
      this.rangeChartWidgetConfigForm.get('title').enable();
      this.rangeChartWidgetConfigForm.get('titleFont').enable();
      this.rangeChartWidgetConfigForm.get('titleColor').enable();
      this.rangeChartWidgetConfigForm.get('showIcon').enable({emitEvent: false});
      if (showIcon) {
        this.rangeChartWidgetConfigForm.get('iconSize').enable();
        this.rangeChartWidgetConfigForm.get('iconSizeUnit').enable();
        this.rangeChartWidgetConfigForm.get('icon').enable();
        this.rangeChartWidgetConfigForm.get('iconColor').enable();
      } else {
        this.rangeChartWidgetConfigForm.get('iconSize').disable();
        this.rangeChartWidgetConfigForm.get('iconSizeUnit').disable();
        this.rangeChartWidgetConfigForm.get('icon').disable();
        this.rangeChartWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.rangeChartWidgetConfigForm.get('title').disable();
      this.rangeChartWidgetConfigForm.get('titleFont').disable();
      this.rangeChartWidgetConfigForm.get('titleColor').disable();
      this.rangeChartWidgetConfigForm.get('showIcon').disable({emitEvent: false});
      this.rangeChartWidgetConfigForm.get('iconSize').disable();
      this.rangeChartWidgetConfigForm.get('iconSizeUnit').disable();
      this.rangeChartWidgetConfigForm.get('icon').disable();
      this.rangeChartWidgetConfigForm.get('iconColor').disable();
    }

    if (showRangeThresholds) {
      this.rangeChartWidgetConfigForm.get('rangeThreshold').enable();
    } else {
      this.rangeChartWidgetConfigForm.get('rangeThreshold').disable();
    }

    if (fillArea) {
      this.rangeChartWidgetConfigForm.get('fillAreaOpacity').enable();
    } else {
      this.rangeChartWidgetConfigForm.get('fillAreaOpacity').disable();
    }

    if (showLine) {
      this.rangeChartWidgetConfigForm.get('step').enable({emitEvent: false});
      if (step) {
        this.rangeChartWidgetConfigForm.get('stepType').enable();
        this.rangeChartWidgetConfigForm.get('smooth').disable();
      } else {
        this.rangeChartWidgetConfigForm.get('stepType').disable();
        this.rangeChartWidgetConfigForm.get('smooth').enable();
      }
      this.rangeChartWidgetConfigForm.get('lineType').enable();
      this.rangeChartWidgetConfigForm.get('lineWidth').enable();
    } else {
      this.rangeChartWidgetConfigForm.get('step').disable({emitEvent: false});
      this.rangeChartWidgetConfigForm.get('stepType').disable();
      this.rangeChartWidgetConfigForm.get('smooth').disable();
      this.rangeChartWidgetConfigForm.get('lineType').disable();
      this.rangeChartWidgetConfigForm.get('lineWidth').disable();
    }
    if (showPointLabel) {
      this.rangeChartWidgetConfigForm.get('pointLabelPosition').enable();
      this.rangeChartWidgetConfigForm.get('pointLabelFont').enable();
      this.rangeChartWidgetConfigForm.get('pointLabelColor').enable();
      this.rangeChartWidgetConfigForm.get('enablePointLabelBackground').enable({emitEvent: false});
      if (enablePointLabelBackground) {
        this.rangeChartWidgetConfigForm.get('pointLabelBackground').enable();
      } else {
        this.rangeChartWidgetConfigForm.get('pointLabelBackground').disable();
      }
    } else {
      this.rangeChartWidgetConfigForm.get('pointLabelPosition').disable();
      this.rangeChartWidgetConfigForm.get('pointLabelFont').disable();
      this.rangeChartWidgetConfigForm.get('pointLabelColor').disable();
      this.rangeChartWidgetConfigForm.get('enablePointLabelBackground').disable({emitEvent: false});
      this.rangeChartWidgetConfigForm.get('pointLabelBackground').disable();
    }

    if (showLegend) {
      this.rangeChartWidgetConfigForm.get('legendPosition').enable();
      this.rangeChartWidgetConfigForm.get('legendLabelFont').enable();
      this.rangeChartWidgetConfigForm.get('legendLabelColor').enable();
    } else {
      this.rangeChartWidgetConfigForm.get('legendPosition').disable();
      this.rangeChartWidgetConfigForm.get('legendLabelFont').disable();
      this.rangeChartWidgetConfigForm.get('legendLabelColor').disable();
    }

    if (showTooltip) {
      this.rangeChartWidgetConfigForm.get('tooltipLabelFont').enable();
      this.rangeChartWidgetConfigForm.get('tooltipLabelColor').enable();
      this.rangeChartWidgetConfigForm.get('tooltipValueFont').enable();
      this.rangeChartWidgetConfigForm.get('tooltipValueColor').enable();
      this.rangeChartWidgetConfigForm.get('tooltipShowDate').enable({emitEvent: false});
      this.rangeChartWidgetConfigForm.get('tooltipBackgroundColor').enable();
      this.rangeChartWidgetConfigForm.get('tooltipBackgroundBlur').enable();
      if (tooltipShowDate) {
        this.rangeChartWidgetConfigForm.get('tooltipDateFormat').enable();
        this.rangeChartWidgetConfigForm.get('tooltipDateFont').enable();
        this.rangeChartWidgetConfigForm.get('tooltipDateColor').enable();
        this.rangeChartWidgetConfigForm.get('tooltipDateInterval').enable();
      } else {
        this.rangeChartWidgetConfigForm.get('tooltipDateFormat').disable();
        this.rangeChartWidgetConfigForm.get('tooltipDateFont').disable();
        this.rangeChartWidgetConfigForm.get('tooltipDateColor').disable();
        this.rangeChartWidgetConfigForm.get('tooltipDateInterval').disable();
      }
    } else {
      this.rangeChartWidgetConfigForm.get('tooltipLabelFont').disable();
      this.rangeChartWidgetConfigForm.get('tooltipLabelColor').disable();
      this.rangeChartWidgetConfigForm.get('tooltipValueFont').disable();
      this.rangeChartWidgetConfigForm.get('tooltipValueColor').disable();
      this.rangeChartWidgetConfigForm.get('tooltipShowDate').disable({emitEvent: false});
      this.rangeChartWidgetConfigForm.get('tooltipDateFormat').disable();
      this.rangeChartWidgetConfigForm.get('tooltipDateFont').disable();
      this.rangeChartWidgetConfigForm.get('tooltipDateColor').disable();
      this.rangeChartWidgetConfigForm.get('tooltipDateInterval').disable();
      this.rangeChartWidgetConfigForm.get('tooltipBackgroundColor').disable();
      this.rangeChartWidgetConfigForm.get('tooltipBackgroundBlur').disable();
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

  private _pointLabelPreviewFn(): string {
    const units: string = getSourceTbUnitSymbol(this.rangeChartWidgetConfigForm.get('units').value);
    const decimals: number = this.rangeChartWidgetConfigForm.get('decimals').value;
    return formatValue(22, decimals, units, false);
  }

  private _tooltipValuePreviewFn(): string {
    const units: string = getSourceTbUnitSymbol(this.rangeChartWidgetConfigForm.get('units').value);
    const decimals: number = this.rangeChartWidgetConfigForm.get('decimals').value;
    return formatValue(22, decimals, units, false);
  }

  private _tooltipDatePreviewFn(): string {
    const dateFormat: DateFormatSettings = this.rangeChartWidgetConfigForm.get('tooltipDateFormat').value;
    const processor = DateFormatProcessor.fromSettings(this.$injector, dateFormat);
    processor.update(Date.now());
    return processor.formatted;
  }
}
