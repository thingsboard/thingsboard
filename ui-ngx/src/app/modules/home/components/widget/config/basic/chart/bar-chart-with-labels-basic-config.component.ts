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
} from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';
import { formatValue, isUndefined, mergeDeep } from '@core/utils';
import {
  cssSizeToStrSize,
  DateFormatProcessor,
  DateFormatSettings,
  resolveCssSize
} from '@shared/models/widget-settings.models';
import {
  barChartWithLabelsDefaultSettings,
  BarChartWithLabelsWidgetSettings
} from '@home/components/widget/lib/chart/bar-chart-with-labels-widget.models';
import { TimeSeriesChartType } from '@home/components/widget/lib/chart/time-series-chart.models';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

@Component({
  selector: 'tb-bar-chart-with-labels-basic-config',
  templateUrl: './bar-chart-with-labels-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class BarChartWithLabelsBasicConfigComponent extends BasicWidgetConfigComponent {

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.barChartWidgetConfigForm.get('datasources').value;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  legendPositions = legendPositions;

  legendPositionTranslationMap = legendPositionTranslationMap;

  barChartWidgetConfigForm: UntypedFormGroup;

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  tooltipDatePreviewFn = this._tooltipDatePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.barChartWidgetConfigForm;
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{ name: 'temperature', label: 'Temperature', type: DataKeyType.timeseries }];
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: BarChartWithLabelsWidgetSettings = mergeDeep<BarChartWithLabelsWidgetSettings>({} as BarChartWithLabelsWidgetSettings,
      barChartWithLabelsDefaultSettings, configData.config.settings as BarChartWithLabelsWidgetSettings);
    const iconSize = resolveCssSize(configData.config.iconSize);
    this.barChartWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      datasources: [configData.config.datasources, []],
      series: [this.getSeries(configData.config.datasources), []],

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

      units: [configData.config.units, []],
      decimals: [configData.config.decimals, []],

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
    this.setSeries(config.series, this.widgetConfig.config.datasources);

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

    this.widgetConfig.config.settings.showBarLabel = config.showBarLabel;
    this.widgetConfig.config.settings.barLabelFont = config.barLabelFont;
    this.widgetConfig.config.settings.barLabelColor = config.barLabelColor;
    this.widgetConfig.config.settings.showBarValue = config.showBarValue;
    this.widgetConfig.config.settings.barValueFont = config.barValueFont;
    this.widgetConfig.config.settings.barValueColor = config.barValueColor;

    this.widgetConfig.config.settings.showBarBorder = config.showBarBorder;
    this.widgetConfig.config.settings.barBorderWidth = config.barBorderWidth;
    this.widgetConfig.config.settings.barBorderRadius = config.barBorderRadius;
    this.widgetConfig.config.settings.barBackgroundSettings = config.barBackgroundSettings;
    this.widgetConfig.config.settings.noAggregationBarWidthSettings = config.noAggregationBarWidthSettings;

    this.widgetConfig.config.units = config.units;
    this.widgetConfig.config.decimals = config.decimals;

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
    return ['showTitle', 'showIcon', 'showBarLabel', 'showBarValue', 'showBarBorder', 'showLegend', 'showTooltip', 'tooltipShowDate'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showTitle: boolean = this.barChartWidgetConfigForm.get('showTitle').value;
    const showIcon: boolean = this.barChartWidgetConfigForm.get('showIcon').value;
    const showBarLabel: boolean = this.barChartWidgetConfigForm.get('showBarLabel').value;
    const showBarValue: boolean = this.barChartWidgetConfigForm.get('showBarValue').value;
    const showBarBorder: boolean = this.barChartWidgetConfigForm.get('showBarBorder').value;
    const showLegend: boolean = this.barChartWidgetConfigForm.get('showLegend').value;
    const showTooltip: boolean = this.barChartWidgetConfigForm.get('showTooltip').value;
    const tooltipShowDate: boolean = this.barChartWidgetConfigForm.get('tooltipShowDate').value;

    if (showTitle) {
      this.barChartWidgetConfigForm.get('title').enable();
      this.barChartWidgetConfigForm.get('titleFont').enable();
      this.barChartWidgetConfigForm.get('titleColor').enable();
      this.barChartWidgetConfigForm.get('showIcon').enable({emitEvent: false});
      if (showIcon) {
        this.barChartWidgetConfigForm.get('iconSize').enable();
        this.barChartWidgetConfigForm.get('iconSizeUnit').enable();
        this.barChartWidgetConfigForm.get('icon').enable();
        this.barChartWidgetConfigForm.get('iconColor').enable();
      } else {
        this.barChartWidgetConfigForm.get('iconSize').disable();
        this.barChartWidgetConfigForm.get('iconSizeUnit').disable();
        this.barChartWidgetConfigForm.get('icon').disable();
        this.barChartWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.barChartWidgetConfigForm.get('title').disable();
      this.barChartWidgetConfigForm.get('titleFont').disable();
      this.barChartWidgetConfigForm.get('titleColor').disable();
      this.barChartWidgetConfigForm.get('showIcon').disable({emitEvent: false});
      this.barChartWidgetConfigForm.get('iconSize').disable();
      this.barChartWidgetConfigForm.get('iconSizeUnit').disable();
      this.barChartWidgetConfigForm.get('icon').disable();
      this.barChartWidgetConfigForm.get('iconColor').disable();
    }

    if (showBarLabel) {
      this.barChartWidgetConfigForm.get('barLabelFont').enable();
      this.barChartWidgetConfigForm.get('barLabelColor').enable();
    } else {
      this.barChartWidgetConfigForm.get('barLabelFont').disable();
      this.barChartWidgetConfigForm.get('barLabelColor').disable();
    }

    if (showBarValue) {
      this.barChartWidgetConfigForm.get('barValueFont').enable();
      this.barChartWidgetConfigForm.get('barValueColor').enable();
    } else {
      this.barChartWidgetConfigForm.get('barValueFont').disable();
      this.barChartWidgetConfigForm.get('barValueColor').disable();
    }
    if (showBarBorder) {
      this.barChartWidgetConfigForm.get('barBorderWidth').enable();
    } else {
      this.barChartWidgetConfigForm.get('barBorderWidth').disable();
    }
    if (showLegend) {
      this.barChartWidgetConfigForm.get('legendPosition').enable();
      this.barChartWidgetConfigForm.get('legendLabelFont').enable();
      this.barChartWidgetConfigForm.get('legendLabelColor').enable();
    } else {
      this.barChartWidgetConfigForm.get('legendPosition').disable();
      this.barChartWidgetConfigForm.get('legendLabelFont').disable();
      this.barChartWidgetConfigForm.get('legendLabelColor').disable();
    }

    if (showTooltip) {
      this.barChartWidgetConfigForm.get('tooltipLabelFont').enable();
      this.barChartWidgetConfigForm.get('tooltipLabelColor').enable();
      this.barChartWidgetConfigForm.get('tooltipValueFont').enable();
      this.barChartWidgetConfigForm.get('tooltipValueColor').enable();
      this.barChartWidgetConfigForm.get('tooltipShowDate').enable({emitEvent: false});
      this.barChartWidgetConfigForm.get('tooltipBackgroundColor').enable();
      this.barChartWidgetConfigForm.get('tooltipBackgroundBlur').enable();
      if (tooltipShowDate) {
        this.barChartWidgetConfigForm.get('tooltipDateFormat').enable();
        this.barChartWidgetConfigForm.get('tooltipDateFont').enable();
        this.barChartWidgetConfigForm.get('tooltipDateColor').enable();
        this.barChartWidgetConfigForm.get('tooltipDateInterval').enable();
      } else {
        this.barChartWidgetConfigForm.get('tooltipDateFormat').disable();
        this.barChartWidgetConfigForm.get('tooltipDateFont').disable();
        this.barChartWidgetConfigForm.get('tooltipDateColor').disable();
        this.barChartWidgetConfigForm.get('tooltipDateInterval').disable();
      }
    } else {
      this.barChartWidgetConfigForm.get('tooltipLabelFont').disable();
      this.barChartWidgetConfigForm.get('tooltipLabelColor').disable();
      this.barChartWidgetConfigForm.get('tooltipValueFont').disable();
      this.barChartWidgetConfigForm.get('tooltipValueColor').disable();
      this.barChartWidgetConfigForm.get('tooltipShowDate').disable({emitEvent: false});
      this.barChartWidgetConfigForm.get('tooltipDateFormat').disable();
      this.barChartWidgetConfigForm.get('tooltipDateFont').disable();
      this.barChartWidgetConfigForm.get('tooltipDateColor').disable();
      this.barChartWidgetConfigForm.get('tooltipDateInterval').disable();
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

  private _tooltipValuePreviewFn(): string {
    const units: string = getSourceTbUnitSymbol(this.barChartWidgetConfigForm.get('units').value);
    const decimals: number = this.barChartWidgetConfigForm.get('decimals').value;
    return formatValue(22, decimals, units, false);
  }

  private _tooltipDatePreviewFn(): string {
    const dateFormat: DateFormatSettings = this.barChartWidgetConfigForm.get('tooltipDateFormat').value;
    const processor = DateFormatProcessor.fromSettings(this.$injector, dateFormat);
    processor.update(Date.now());
    return processor.formatted;
  }

  protected readonly TimeSeriesChartType = TimeSeriesChartType;
}
