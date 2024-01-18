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
import { formatValue, isDefinedAndNotNull, isUndefined } from '@core/utils';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';
import {
  doughnutDefaultSettings,
  DoughnutLayout,
  doughnutLayoutImages,
  doughnutLayouts,
  doughnutLayoutTranslations,
  DoughnutTooltipValueType,
  doughnutTooltipValueTypes,
  doughnutTooltipValueTypeTranslations,
  DoughnutWidgetSettings,
  horizontalDoughnutLayoutImages
} from '@home/components/widget/lib/chart/doughnut-widget.models';

@Component({
  selector: 'tb-doughnut-basic-config',
  templateUrl: './doughnut-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class DoughnutBasicConfigComponent extends BasicWidgetConfigComponent {

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.doughnutWidgetConfigForm.get('datasources').value;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  public get displayTimewindowConfig(): boolean {
    const datasources = this.doughnutWidgetConfigForm.get('datasources').value;
    return datasourcesHasAggregation(datasources);
  }

  public onlyHistoryTimewindow(): boolean {
    const datasources = this.doughnutWidgetConfigForm.get('datasources').value;
    return datasourcesHasOnlyComparisonAggregation(datasources);
  }

  doughnutLayouts = doughnutLayouts;

  doughnutLayoutTranslationMap = doughnutLayoutTranslations;

  horizontal = false;

  doughnutLayoutImageMap: Map<DoughnutLayout, string>;

  legendPositions = legendPositions;

  legendPositionTranslationMap = legendPositionTranslationMap;

  doughnutTooltipValueTypes = doughnutTooltipValueTypes;

  doughnutTooltipValueTypeTranslationMap = doughnutTooltipValueTypeTranslations;

  doughnutWidgetConfigForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  get totalEnabled(): boolean {
    const layout: DoughnutLayout = this.doughnutWidgetConfigForm.get('layout').value;
    return layout === DoughnutLayout.with_total;
  }

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.doughnutWidgetConfigForm;
  }

  protected setupConfig(widgetConfig: WidgetConfigComponentData) {
    const params = widgetConfig.typeParameters as any;
    this.horizontal = isDefinedAndNotNull(params.horizontal) ? params.horizontal : false;
    this.doughnutLayoutImageMap = this.horizontal ? horizontalDoughnutLayoutImages : doughnutLayoutImages;
    super.setupConfig(widgetConfig);
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{ name: 'windPower', label: 'Wind power', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#08872B' },
            { name: 'solarPower', label: 'Solar power', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#FF4D5A' }];
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: DoughnutWidgetSettings = {...doughnutDefaultSettings(this.horizontal), ...(configData.config.settings || {})};
    this.doughnutWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      datasources: [configData.config.datasources, []],

      series: [this.getSeries(configData.config.datasources), []],

      layout: [settings.layout, []],
      autoScale: [settings.autoScale, []],
      clockwise: [settings.clockwise, []],
      sortSeries: [settings.sortSeries, []],

      showTitle: [configData.config.showTitle, []],
      title: [configData.config.title, []],
      titleFont: [configData.config.titleFont, []],
      titleColor: [configData.config.titleColor, []],
      showTitleIcon: [configData.config.showTitleIcon, []],
      titleIcon: [configData.config.titleIcon, []],
      iconColor: [configData.config.iconColor, []],

      totalValueFont: [settings.totalValueFont, []],
      totalValueColor: [settings.totalValueColor, []],

      units: [configData.config.units, []],
      decimals: [configData.config.decimals, []],

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

    this.widgetConfig.config.settings.layout = config.layout;
    this.widgetConfig.config.settings.autoScale = config.autoScale;
    this.widgetConfig.config.settings.clockwise = config.clockwise;
    this.widgetConfig.config.settings.sortSeries = config.sortSeries;

    this.widgetConfig.config.settings.totalValueFont = config.totalValueFont;
    this.widgetConfig.config.settings.totalValueColor = config.totalValueColor;

    this.widgetConfig.config.units = config.units;
    this.widgetConfig.config.decimals = config.decimals;

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
    return ['layout', 'showTitle', 'showTitleIcon', 'showLegend', 'showTooltip'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const layout: DoughnutLayout = this.doughnutWidgetConfigForm.get('layout').value;
    const showTitle: boolean = this.doughnutWidgetConfigForm.get('showTitle').value;
    const showTitleIcon: boolean = this.doughnutWidgetConfigForm.get('showTitleIcon').value;
    const showLegend: boolean = this.doughnutWidgetConfigForm.get('showLegend').value;
    const showTooltip: boolean = this.doughnutWidgetConfigForm.get('showTooltip').value;

    const totalEnabled = layout === DoughnutLayout.with_total;

    if (showTitle) {
      this.doughnutWidgetConfigForm.get('title').enable();
      this.doughnutWidgetConfigForm.get('titleFont').enable();
      this.doughnutWidgetConfigForm.get('titleColor').enable();
      this.doughnutWidgetConfigForm.get('showTitleIcon').enable({emitEvent: false});
      if (showTitleIcon) {
        this.doughnutWidgetConfigForm.get('titleIcon').enable();
        this.doughnutWidgetConfigForm.get('iconColor').enable();
      } else {
        this.doughnutWidgetConfigForm.get('titleIcon').disable();
        this.doughnutWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.doughnutWidgetConfigForm.get('title').disable();
      this.doughnutWidgetConfigForm.get('titleFont').disable();
      this.doughnutWidgetConfigForm.get('titleColor').disable();
      this.doughnutWidgetConfigForm.get('showTitleIcon').disable({emitEvent: false});
      this.doughnutWidgetConfigForm.get('titleIcon').disable();
      this.doughnutWidgetConfigForm.get('iconColor').disable();
    }
    if (showLegend) {
      this.doughnutWidgetConfigForm.get('legendPosition').enable();
      this.doughnutWidgetConfigForm.get('legendLabelFont').enable();
      this.doughnutWidgetConfigForm.get('legendLabelColor').enable();
      this.doughnutWidgetConfigForm.get('legendValueFont').enable();
      this.doughnutWidgetConfigForm.get('legendValueColor').enable();
    } else {
      this.doughnutWidgetConfigForm.get('legendPosition').disable();
      this.doughnutWidgetConfigForm.get('legendLabelFont').disable();
      this.doughnutWidgetConfigForm.get('legendLabelColor').disable();
      this.doughnutWidgetConfigForm.get('legendValueFont').disable();
      this.doughnutWidgetConfigForm.get('legendValueColor').disable();
    }
    if (showTooltip) {
      this.doughnutWidgetConfigForm.get('tooltipValueType').enable();
      this.doughnutWidgetConfigForm.get('tooltipValueDecimals').enable();
      this.doughnutWidgetConfigForm.get('tooltipValueFont').enable();
      this.doughnutWidgetConfigForm.get('tooltipValueColor').enable();
      this.doughnutWidgetConfigForm.get('tooltipBackgroundColor').enable();
      this.doughnutWidgetConfigForm.get('tooltipBackgroundBlur').enable();
    } else {
      this.doughnutWidgetConfigForm.get('tooltipValueType').disable();
      this.doughnutWidgetConfigForm.get('tooltipValueDecimals').disable();
      this.doughnutWidgetConfigForm.get('tooltipValueFont').disable();
      this.doughnutWidgetConfigForm.get('tooltipValueColor').disable();
      this.doughnutWidgetConfigForm.get('tooltipBackgroundColor').disable();
      this.doughnutWidgetConfigForm.get('tooltipBackgroundBlur').disable();
    }
    if (totalEnabled) {
      this.doughnutWidgetConfigForm.get('totalValueFont').enable();
      this.doughnutWidgetConfigForm.get('totalValueColor').enable();
    } else {
      this.doughnutWidgetConfigForm.get('totalValueFont').disable();
      this.doughnutWidgetConfigForm.get('totalValueColor').disable();
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
    const units: string = this.doughnutWidgetConfigForm.get('units').value;
    const decimals: number = this.doughnutWidgetConfigForm.get('decimals').value;
    return formatValue(110, decimals, units, false);
  }

  private _tooltipValuePreviewFn(): string {
    const tooltipValueType: DoughnutTooltipValueType = this.doughnutWidgetConfigForm.get('tooltipValueType').value;
    const decimals: number = this.doughnutWidgetConfigForm.get('tooltipValueDecimals').value;
    if (tooltipValueType === DoughnutTooltipValueType.percentage) {
      return formatValue(35, decimals, '%', false);
    } else {
      const units: string = this.doughnutWidgetConfigForm.get('units').value;
      return formatValue(110, decimals, units, false);
    }
  }
}
