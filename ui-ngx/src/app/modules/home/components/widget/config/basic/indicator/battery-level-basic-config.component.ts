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
  WidgetConfig,
  widgetTitleAutocompleteValues,
} from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';
import { formatValue, isUndefined } from '@core/utils';
import { cssSizeToStrSize, resolveCssSize } from '@shared/models/widget-settings.models';
import {
  batteryLevelDefaultSettings,
  BatteryLevelLayout,
  batteryLevelLayoutImages,
  batteryLevelLayouts,
  batteryLevelLayoutTranslations,
  BatteryLevelWidgetSettings
} from '@home/components/widget/lib/indicator/battery-level-widget.models';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

@Component({
  selector: 'tb-battery-level-basic-config',
  templateUrl: './battery-level-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class BatteryLevelBasicConfigComponent extends BasicWidgetConfigComponent {

  public get displayTimewindowConfig(): boolean {
    const datasources = this.batteryLevelWidgetConfigForm.get('datasources').value;
    return datasourcesHasAggregation(datasources);
  }

  public onlyHistoryTimewindow(): boolean {
    const datasources = this.batteryLevelWidgetConfigForm.get('datasources').value;
    return datasourcesHasOnlyComparisonAggregation(datasources);
  }

  batteryLevelLayouts = batteryLevelLayouts;

  batteryLevelLayoutTranslationMap = batteryLevelLayoutTranslations;
  batteryLevelLayoutImageMap = batteryLevelLayoutImages;

  batteryLevelWidgetConfigForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  get sectionsCountEnabled(): boolean {
    const layout: BatteryLevelLayout = this.batteryLevelWidgetConfigForm.get('layout').value;
    return [BatteryLevelLayout.vertical_divided, BatteryLevelLayout.horizontal_divided].includes(layout);
  }

  public get datasource(): Datasource {
    const datasources: Datasource[] = this.widgetConfig.config.datasources;
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  predefinedValues = widgetTitleAutocompleteValues;
  
  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.batteryLevelWidgetConfigForm;
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{ name: 'batteryLevel', label: 'batteryLevel', type: DataKeyType.timeseries }];
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: BatteryLevelWidgetSettings = {...batteryLevelDefaultSettings, ...(configData.config.settings || {})};
    const iconSize = resolveCssSize(configData.config.iconSize);
    this.batteryLevelWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      datasources: [configData.config.datasources, []],

      layout: [settings.layout, []],
      sectionsCount: [settings.sectionsCount, [Validators.min(2), Validators.max(20)]],

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
      autoScaleValueSize: [settings.autoScaleValueSize, []],
      valueFont: [settings.valueFont, []],
      valueColor: [settings.valueColor, []],

      batteryLevelColor: [settings.batteryLevelColor, []],
      batteryShapeColor: [settings.batteryShapeColor, []],

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
    this.widgetConfig.config.settings.sectionsCount = config.sectionsCount;

    this.widgetConfig.config.settings.showValue = config.showValue;
    this.widgetConfig.config.settings.autoScaleValueSize = config.autoScaleValueSize === true;
    this.widgetConfig.config.settings.valueFont = config.valueFont;
    this.widgetConfig.config.settings.valueColor = config.valueColor;

    this.widgetConfig.config.settings.batteryLevelColor = config.batteryLevelColor;
    this.widgetConfig.config.settings.batteryShapeColor = config.batteryShapeColor;

    this.widgetConfig.config.settings.background = config.background;

    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;
    this.widgetConfig.config.settings.padding = config.padding;

    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showTitle', 'showIcon', 'showValue', 'layout'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showTitle: boolean = this.batteryLevelWidgetConfigForm.get('showTitle').value;
    const showIcon: boolean = this.batteryLevelWidgetConfigForm.get('showIcon').value;
    const showValue: boolean = this.batteryLevelWidgetConfigForm.get('showValue').value;
    const layout: BatteryLevelLayout = this.batteryLevelWidgetConfigForm.get('layout').value;
    const divided = [BatteryLevelLayout.vertical_divided, BatteryLevelLayout.horizontal_divided].includes(layout);

    if (showTitle) {
      this.batteryLevelWidgetConfigForm.get('title').enable();
      this.batteryLevelWidgetConfigForm.get('titleFont').enable();
      this.batteryLevelWidgetConfigForm.get('titleColor').enable();
      this.batteryLevelWidgetConfigForm.get('showIcon').enable({emitEvent: false});
      if (showIcon) {
        this.batteryLevelWidgetConfigForm.get('iconSize').enable();
        this.batteryLevelWidgetConfigForm.get('iconSizeUnit').enable();
        this.batteryLevelWidgetConfigForm.get('icon').enable();
        this.batteryLevelWidgetConfigForm.get('iconColor').enable();
      } else {
        this.batteryLevelWidgetConfigForm.get('iconSize').disable();
        this.batteryLevelWidgetConfigForm.get('iconSizeUnit').disable();
        this.batteryLevelWidgetConfigForm.get('icon').disable();
        this.batteryLevelWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.batteryLevelWidgetConfigForm.get('title').disable();
      this.batteryLevelWidgetConfigForm.get('titleFont').disable();
      this.batteryLevelWidgetConfigForm.get('titleColor').disable();
      this.batteryLevelWidgetConfigForm.get('showIcon').disable({emitEvent: false});
      this.batteryLevelWidgetConfigForm.get('iconSize').disable();
      this.batteryLevelWidgetConfigForm.get('iconSizeUnit').disable();
      this.batteryLevelWidgetConfigForm.get('icon').disable();
      this.batteryLevelWidgetConfigForm.get('iconColor').disable();
    }

    if (showValue) {
      this.batteryLevelWidgetConfigForm.get('autoScaleValueSize').enable();
      this.batteryLevelWidgetConfigForm.get('valueFont').enable();
      this.batteryLevelWidgetConfigForm.get('valueColor').enable();
    } else {
      this.batteryLevelWidgetConfigForm.get('autoScaleValueSize').disable();
      this.batteryLevelWidgetConfigForm.get('valueFont').disable();
      this.batteryLevelWidgetConfigForm.get('valueColor').disable();
    }
    if (divided) {
      this.batteryLevelWidgetConfigForm.get('sectionsCount').enable();
    } else {
      this.batteryLevelWidgetConfigForm.get('sectionsCount').disable();
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
    return formatValue(55, decimals, units, true);
  }
}
