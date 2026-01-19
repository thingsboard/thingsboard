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
import {
  DataKey,
  datasourcesHasAggregation,
  datasourcesHasOnlyComparisonAggregation,
  WidgetConfig,
  widgetTitleAutocompleteValues,
} from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { formatValue, isUndefined } from '@core/utils';
import {
  labelValueCardWidgetDefaultSettings,
  LabelValueCardWidgetSettings
} from '@home/components/widget/lib/cards/label-value-card-widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

@Component({
  selector: 'tb-label-value-card-basic-config',
  templateUrl: './label-value-card-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class LabelValueCardBasicConfigComponent extends BasicWidgetConfigComponent {

  public get displayTimewindowConfig(): boolean {
    const datasources = this.labelValueCardWidgetConfigForm.get('datasources').value;
    return datasourcesHasAggregation(datasources);
  }

  public onlyHistoryTimewindow(): boolean {
    const datasources = this.labelValueCardWidgetConfigForm.get('datasources').value;
    return datasourcesHasOnlyComparisonAggregation(datasources);
  }

  labelValueCardWidgetConfigForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  predefinedValues = widgetTitleAutocompleteValues;
  
  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.labelValueCardWidgetConfigForm;
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{ name: 'temperature', label: 'Temperature', type: DataKeyType.timeseries }];
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: LabelValueCardWidgetSettings = {...labelValueCardWidgetDefaultSettings, ...(configData.config.settings || {})};
    this.labelValueCardWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      datasources: [configData.config.datasources, []],
      autoScale: [settings.autoScale, []],

      showLabel: [settings.showLabel, []],
      label: [settings.label, []],
      labelFont: [settings.labelFont, []],
      labelColor: [settings.labelColor, []],

      showIcon: [settings.showIcon, []],
      iconSize: [settings.iconSize, [Validators.min(0)]],
      iconSizeUnit: [settings.iconSizeUnit, []],
      icon: [settings.icon, []],
      iconColor: [settings.iconColor, []],

      units: [configData.config.units, []],
      decimals: [configData.config.decimals, []],
      valueFont: [settings.valueFont, []],
      valueColor: [settings.valueColor, []],

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

    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};

    this.widgetConfig.config.settings.autoScale = config.autoScale;

    this.widgetConfig.config.settings.showLabel = config.showLabel;
    this.widgetConfig.config.settings.label = config.label;
    this.widgetConfig.config.settings.labelFont = config.labelFont;
    this.widgetConfig.config.settings.labelColor = config.labelColor;

    this.widgetConfig.config.settings.showIcon = config.showIcon;
    this.widgetConfig.config.settings.iconSize = config.iconSize;
    this.widgetConfig.config.settings.iconSizeUnit = config.iconSizeUnit;
    this.widgetConfig.config.settings.icon = config.icon;
    this.widgetConfig.config.settings.iconColor = config.iconColor;

    this.widgetConfig.config.units = config.units;
    this.widgetConfig.config.decimals = config.decimals;
    this.widgetConfig.config.settings.valueFont = config.valueFont;
    this.widgetConfig.config.settings.valueColor = config.valueColor;

    this.widgetConfig.config.settings.background = config.background;

    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;
    this.widgetConfig.config.settings.padding = config.padding;

    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showLabel', 'showIcon'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showLabel: boolean = this.labelValueCardWidgetConfigForm.get('showLabel').value;
    const showIcon: boolean = this.labelValueCardWidgetConfigForm.get('showIcon').value;

    if (showLabel) {
      this.labelValueCardWidgetConfigForm.get('label').enable();
      this.labelValueCardWidgetConfigForm.get('labelFont').enable();
      this.labelValueCardWidgetConfigForm.get('labelColor').enable();
    } else {
      this.labelValueCardWidgetConfigForm.get('label').disable();
      this.labelValueCardWidgetConfigForm.get('labelFont').disable();
      this.labelValueCardWidgetConfigForm.get('labelColor').disable();
    }

    if (showIcon) {
      this.labelValueCardWidgetConfigForm.get('iconSize').enable();
      this.labelValueCardWidgetConfigForm.get('iconSizeUnit').enable();
      this.labelValueCardWidgetConfigForm.get('icon').enable();
      this.labelValueCardWidgetConfigForm.get('iconColor').enable();
    } else {
      this.labelValueCardWidgetConfigForm.get('iconSize').disable();
      this.labelValueCardWidgetConfigForm.get('iconSizeUnit').disable();
      this.labelValueCardWidgetConfigForm.get('icon').disable();
      this.labelValueCardWidgetConfigForm.get('iconColor').disable();
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
    const units = getSourceTbUnitSymbol(this.labelValueCardWidgetConfigForm.get('units').value);
    const decimals: number = this.labelValueCardWidgetConfigForm.get('decimals').value;
    return formatValue(22, decimals, units, true);
  }
}
