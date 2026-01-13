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

import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { DataKey, Datasource, WidgetConfig, } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';
import { formatValue, isUndefined } from '@core/utils';
import { cssSizeToStrSize, getDataKey, resolveCssSize } from '@shared/models/widget-settings.models';
import {
  valueCartCardLayouts,
  valueChartCardDefaultSettings,
  valueChartCardLayoutImages,
  valueChartCardLayoutTranslations,
  ValueChartCardWidgetSettings
} from '@home/components/widget/lib/cards/value-chart-card-widget.models';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

@Component({
  selector: 'tb-value-chart-card-basic-config',
  templateUrl: './value-chart-card-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class ValueChartCardBasicConfigComponent extends BasicWidgetConfigComponent {

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.valueChartCardWidgetConfigForm.get('datasources').value;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  valueChartCardLayouts = valueCartCardLayouts;

  valueChartCardLayoutTranslationMap = valueChartCardLayoutTranslations;
  valueChartCardLayoutImageMap = valueChartCardLayoutImages;

  valueChartCardWidgetConfigForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.valueChartCardWidgetConfigForm;
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [
      { name: 'temperature', label: 'Temperature', type: DataKeyType.timeseries, color: 'rgba(63, 82, 221, 1)'}
    ];
  }

  protected defaultLatestDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{ name: 'temperature', label: 'Latest', type: DataKeyType.timeseries}];
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: ValueChartCardWidgetSettings = {...valueChartCardDefaultSettings, ...(configData.config.settings || {})};
    const dataKey = getDataKey(configData.config.datasources);
    const iconSize = resolveCssSize(configData.config.iconSize);
    this.valueChartCardWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      datasources: [configData.config.datasources, []],

      layout: [settings.layout, []],
      autoScale: [settings.autoScale, []],

      showTitle: [configData.config.showTitle, []],
      title: [configData.config.title, []],
      titleFont: [configData.config.titleFont, []],
      titleColor: [configData.config.titleColor, []],

      showIcon: [configData.config.showTitleIcon, []],
      iconSize: [iconSize[0], [Validators.min(0)]],
      iconSizeUnit: [iconSize[1], []],
      icon: [configData.config.titleIcon, []],
      iconColor: [configData.config.iconColor, []],

      showValue: [settings.showValue, []],
      units: [configData.config.units, []],
      decimals: [configData.config.decimals, []],
      valueFont: [settings.valueFont, []],
      valueColor: [settings.valueColor, []],

      chartColor: [dataKey?.color, []],

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

    this.widgetConfig.config.settings.layout = config.layout;
    this.widgetConfig.config.settings.autoScale = config.autoScale;

    this.widgetConfig.config.settings.showValue = config.showValue;
    this.widgetConfig.config.units = config.units;
    this.widgetConfig.config.decimals = config.decimals;
    this.widgetConfig.config.settings.valueFont = config.valueFont;
    this.widgetConfig.config.settings.valueColor = config.valueColor;

    const dataKey = getDataKey(this.widgetConfig.config.datasources);
    if (dataKey) {
      dataKey.color = config.chartColor;
      this.updateLatestValues(dataKey, this.widgetConfig.config.datasources);
    }

    this.widgetConfig.config.settings.background = config.background;

    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;
    this.widgetConfig.config.settings.padding = config.padding;

    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showTitle', 'showIcon', 'showValue'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showTitle: boolean = this.valueChartCardWidgetConfigForm.get('showTitle').value;
    const showIcon: boolean = this.valueChartCardWidgetConfigForm.get('showIcon').value;
    const showValue: boolean = this.valueChartCardWidgetConfigForm.get('showValue').value;

    if (showTitle) {
      this.valueChartCardWidgetConfigForm.get('title').enable();
      this.valueChartCardWidgetConfigForm.get('titleFont').enable();
      this.valueChartCardWidgetConfigForm.get('titleColor').enable();
      this.valueChartCardWidgetConfigForm.get('showIcon').enable({emitEvent: false});
      if (showIcon) {
        this.valueChartCardWidgetConfigForm.get('iconSize').enable();
        this.valueChartCardWidgetConfigForm.get('iconSizeUnit').enable();
        this.valueChartCardWidgetConfigForm.get('icon').enable();
        this.valueChartCardWidgetConfigForm.get('iconColor').enable();
      } else {
        this.valueChartCardWidgetConfigForm.get('iconSize').disable();
        this.valueChartCardWidgetConfigForm.get('iconSizeUnit').disable();
        this.valueChartCardWidgetConfigForm.get('icon').disable();
        this.valueChartCardWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.valueChartCardWidgetConfigForm.get('title').disable();
      this.valueChartCardWidgetConfigForm.get('titleFont').disable();
      this.valueChartCardWidgetConfigForm.get('titleColor').disable();
      this.valueChartCardWidgetConfigForm.get('showIcon').disable({emitEvent: false});
      this.valueChartCardWidgetConfigForm.get('iconSize').disable();
      this.valueChartCardWidgetConfigForm.get('iconSizeUnit').disable();
      this.valueChartCardWidgetConfigForm.get('icon').disable();
      this.valueChartCardWidgetConfigForm.get('iconColor').disable();
    }

    if (showValue) {
      this.valueChartCardWidgetConfigForm.get('units').enable();
      this.valueChartCardWidgetConfigForm.get('decimals').enable();
      this.valueChartCardWidgetConfigForm.get('valueFont').enable();
      this.valueChartCardWidgetConfigForm.get('valueColor').enable();
    } else {
      this.valueChartCardWidgetConfigForm.get('units').disable();
      this.valueChartCardWidgetConfigForm.get('decimals').disable();
      this.valueChartCardWidgetConfigForm.get('valueFont').disable();
      this.valueChartCardWidgetConfigForm.get('valueColor').disable();
    }
  }

  private updateLatestValues(sourceDataKey: DataKey, datasources?: Datasource[]) {
    if (datasources && datasources.length) {
      let latestDataKeys = datasources[0].latestDataKeys;
      if (!latestDataKeys) {
        latestDataKeys = [];
        datasources[0].latestDataKeys = latestDataKeys;
      }
      let dataKey: DataKey;
      if (!latestDataKeys.length) {
        dataKey = {...sourceDataKey};
        latestDataKeys.push(dataKey);
      } else {
        dataKey = latestDataKeys[0];
        dataKey = {...dataKey, ...sourceDataKey};
        latestDataKeys[0] = dataKey;
      }
      dataKey.label = 'Latest';
      dataKey.units = null;
      dataKey.decimals = null;
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
    const units: string = getSourceTbUnitSymbol(this.widgetConfig.config.units);
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(22, decimals, units, true);
  }
}
