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

import { ChangeDetectorRef, Component, Injector } from '@angular/core';
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
} from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';
import { formatValue, isDefinedAndNotNull, isUndefined } from '@core/utils';
import { DateFormatProcessor, DateFormatSettings, getLabel, setLabel } from '@shared/models/widget-settings.models';
import {
  valueCardDefaultSettings,
  ValueCardLayout,
  valueCardLayoutImages,
  valueCardLayouts,
  valueCardLayoutTranslations,
  ValueCardWidgetSettings
} from '@home/components/widget/lib/cards/value-card-widget.models';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

@Component({
  selector: 'tb-value-card-basic-config',
  templateUrl: './value-card-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class ValueCardBasicConfigComponent extends BasicWidgetConfigComponent {

  public get displayTimewindowConfig(): boolean {
    const datasources = this.valueCardWidgetConfigForm.get('datasources').value;
    return datasourcesHasAggregation(datasources);
  }

  public onlyHistoryTimewindow(): boolean {
    const datasources = this.valueCardWidgetConfigForm.get('datasources').value;
    return datasourcesHasOnlyComparisonAggregation(datasources);
  }

  valueCardLayouts: ValueCardLayout[] = [];

  valueCardLayoutTranslationMap = valueCardLayoutTranslations;
  valueCardLayoutImageMap = valueCardLayoutImages;

  horizontal = false;

  valueCardWidgetConfigForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  datePreviewFn = this._datePreviewFn.bind(this);

  get dateEnabled(): boolean {
    const layout: ValueCardLayout = this.valueCardWidgetConfigForm.get('layout').value;
    return ![ValueCardLayout.vertical, ValueCardLayout.simplified].includes(layout);
  }

  get iconEnabled(): boolean {
    const layout: ValueCardLayout = this.valueCardWidgetConfigForm.get('layout').value;
    return layout !== ValueCardLayout.simplified;
  }

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private cd: ChangeDetectorRef,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.valueCardWidgetConfigForm;
  }

  protected setupConfig(widgetConfig: WidgetConfigComponentData) {
    const params = widgetConfig.typeParameters as any;
    this.horizontal  = isDefinedAndNotNull(params.horizontal) ? params.horizontal : false;
    this.valueCardLayouts = valueCardLayouts(this.horizontal);
    super.setupConfig(widgetConfig);
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{ name: 'temperature', label: 'Temperature', type: DataKeyType.timeseries }];
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: ValueCardWidgetSettings = {...valueCardDefaultSettings(this.horizontal), ...(configData.config.settings || {})};
    this.valueCardWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      datasources: [configData.config.datasources, []],
      layout: [settings.layout, []],
      autoScale: [settings.autoScale, []],

      showLabel: [settings.showLabel, []],
      label: [getLabel(configData.config.datasources), []],
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

      showDate: [settings.showDate, []],
      dateFormat: [settings.dateFormat, []],
      dateFont: [settings.dateFont, []],
      dateColor: [settings.dateColor, []],

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

    this.widgetConfig.config.settings.layout = config.layout;
    this.widgetConfig.config.settings.autoScale = config.autoScale;

    this.widgetConfig.config.settings.showLabel = config.showLabel;
    setLabel(config.label, this.widgetConfig.config.datasources);
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

    this.widgetConfig.config.settings.showDate = config.showDate;
    this.widgetConfig.config.settings.dateFormat = config.dateFormat;
    this.widgetConfig.config.settings.dateFont = config.dateFont;
    this.widgetConfig.config.settings.dateColor = config.dateColor;

    this.widgetConfig.config.settings.background = config.background;

    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;
    this.widgetConfig.config.settings.padding = config.padding;

    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['layout', 'showLabel', 'showIcon', 'showDate'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const layout: ValueCardLayout = this.valueCardWidgetConfigForm.get('layout').value;
    const showLabel: boolean = this.valueCardWidgetConfigForm.get('showLabel').value;
    const showIcon: boolean = this.valueCardWidgetConfigForm.get('showIcon').value;
    const showDate: boolean = this.valueCardWidgetConfigForm.get('showDate').value;

    const dateEnabled = ![ValueCardLayout.vertical, ValueCardLayout.simplified].includes(layout);
    const iconEnabled = layout !== ValueCardLayout.simplified;

    if (showLabel) {
      this.valueCardWidgetConfigForm.get('label').enable();
      this.valueCardWidgetConfigForm.get('labelFont').enable();
      this.valueCardWidgetConfigForm.get('labelColor').enable();
    } else {
      this.valueCardWidgetConfigForm.get('label').disable();
      this.valueCardWidgetConfigForm.get('labelFont').disable();
      this.valueCardWidgetConfigForm.get('labelColor').disable();
    }

    if (iconEnabled) {
      this.valueCardWidgetConfigForm.get('showIcon').enable({emitEvent: false});
      if (showIcon) {
        this.valueCardWidgetConfigForm.get('iconSize').enable();
        this.valueCardWidgetConfigForm.get('iconSizeUnit').enable();
        this.valueCardWidgetConfigForm.get('icon').enable();
        this.valueCardWidgetConfigForm.get('iconColor').enable();
      } else {
        this.valueCardWidgetConfigForm.get('iconSize').disable();
        this.valueCardWidgetConfigForm.get('iconSizeUnit').disable();
        this.valueCardWidgetConfigForm.get('icon').disable();
        this.valueCardWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.valueCardWidgetConfigForm.get('showIcon').disable({emitEvent: false});
      this.valueCardWidgetConfigForm.get('iconSize').disable();
      this.valueCardWidgetConfigForm.get('iconSizeUnit').disable();
      this.valueCardWidgetConfigForm.get('icon').disable();
      this.valueCardWidgetConfigForm.get('iconColor').disable();
    }

    if (dateEnabled) {
      this.valueCardWidgetConfigForm.get('showDate').enable({emitEvent: false});
      if (showDate) {
        this.valueCardWidgetConfigForm.get('dateFormat').enable();
        this.valueCardWidgetConfigForm.get('dateFont').enable();
        this.valueCardWidgetConfigForm.get('dateColor').enable();
      } else {
        this.valueCardWidgetConfigForm.get('dateFormat').disable();
        this.valueCardWidgetConfigForm.get('dateFont').disable();
        this.valueCardWidgetConfigForm.get('dateColor').disable();
      }
    } else {
      this.valueCardWidgetConfigForm.get('showDate').disable({emitEvent: false});
      this.valueCardWidgetConfigForm.get('dateFormat').disable();
      this.valueCardWidgetConfigForm.get('dateFont').disable();
      this.valueCardWidgetConfigForm.get('dateColor').disable();
    }
    this.valueCardWidgetConfigForm.get('showIcon').updateValueAndValidity({emitEvent: false});
    this.valueCardWidgetConfigForm.get('showDate').updateValueAndValidity({emitEvent: false});
    this.valueCardWidgetConfigForm.get('label').updateValueAndValidity({emitEvent});
    this.valueCardWidgetConfigForm.get('labelFont').updateValueAndValidity({emitEvent});
    this.valueCardWidgetConfigForm.get('labelColor').updateValueAndValidity({emitEvent});
    this.valueCardWidgetConfigForm.get('iconSize').updateValueAndValidity({emitEvent});
    this.valueCardWidgetConfigForm.get('iconSizeUnit').updateValueAndValidity({emitEvent});
    this.valueCardWidgetConfigForm.get('icon').updateValueAndValidity({emitEvent});
    this.valueCardWidgetConfigForm.get('iconColor').updateValueAndValidity({emitEvent});
    this.valueCardWidgetConfigForm.get('dateFormat').updateValueAndValidity({emitEvent});
    this.valueCardWidgetConfigForm.get('dateFont').updateValueAndValidity({emitEvent});
    this.valueCardWidgetConfigForm.get('dateColor').updateValueAndValidity({emitEvent});
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
    const units = getSourceTbUnitSymbol(this.valueCardWidgetConfigForm.get('units').value);
    const decimals: number = this.valueCardWidgetConfigForm.get('decimals').value;
    return formatValue(22, decimals, units, true);
  }

  private _datePreviewFn(): string {
    const dateFormat: DateFormatSettings = this.valueCardWidgetConfigForm.get('dateFormat').value;
    const processor = DateFormatProcessor.fromSettings(this.$injector, dateFormat);
    processor.update(Date.now());
    return processor.formatted;
  }
}
