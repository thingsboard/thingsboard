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
import { formatValue, isUndefined } from '@core/utils';
import {
  cssSizeToStrSize,
  DateFormatProcessor,
  DateFormatSettings,
  resolveCssSize
} from '@shared/models/widget-settings.models';
import {
  signalStrengthDefaultSettings,
  signalStrengthLayoutImages,
  signalStrengthLayouts,
  signalStrengthLayoutTranslations,
  SignalStrengthWidgetSettings
} from '@home/components/widget/lib/indicator/signal-strength-widget.models';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

@Component({
    selector: 'tb-signal-strength-basic-config',
    templateUrl: './signal-strength-basic-config.component.html',
    styleUrls: ['../basic-config.scss'],
    standalone: false
})
export class SignalStrengthBasicConfigComponent extends BasicWidgetConfigComponent {

  public get displayTimewindowConfig(): boolean {
    const datasources = this.signalStrengthWidgetConfigForm.get('datasources').value;
    return datasourcesHasAggregation(datasources);
  }

  public onlyHistoryTimewindow(): boolean {
    const datasources = this.signalStrengthWidgetConfigForm.get('datasources').value;
    return datasourcesHasOnlyComparisonAggregation(datasources);
  }

  signalStrengthLayouts = signalStrengthLayouts;

  signalStrengthLayoutTranslationMap = signalStrengthLayoutTranslations;
  signalStrengthLayoutImageMap = signalStrengthLayoutImages;

  signalStrengthWidgetConfigForm: UntypedFormGroup;

  datePreviewFn = this._datePreviewFn.bind(this);
  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);
  tooltipDatePreviewFn = this._tooltipDatePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.signalStrengthWidgetConfigForm;
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{ name: 'rssi', label: 'rssi', type: DataKeyType.timeseries }];
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: SignalStrengthWidgetSettings = {...signalStrengthDefaultSettings, ...(configData.config.settings || {})};
    const iconSize = resolveCssSize(configData.config.iconSize);
    this.signalStrengthWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      datasources: [configData.config.datasources, []],

      layout: [settings.layout, []],

      showTitle: [configData.config.showTitle, []],
      title: [configData.config.title, []],
      titleFont: [configData.config.titleFont, []],
      titleColor: [configData.config.titleColor, []],

      showIcon: [configData.config.showTitleIcon, []],
      iconSize: [iconSize[0], [Validators.min(0)]],
      iconSizeUnit: [iconSize[1], []],
      icon: [configData.config.titleIcon, []],
      iconColor: [configData.config.iconColor, []],

      showDate: [settings.showDate, []],
      dateFormat: [settings.dateFormat, []],
      dateFont: [settings.dateFont, []],
      dateColor: [settings.dateColor, []],

      activeBarsColor: [settings.activeBarsColor, []],
      inactiveBarsColor: [settings.inactiveBarsColor, []],

      showTooltip: [settings.showTooltip, []],

      showTooltipValue: [settings.showTooltipValue, []],
      units: [configData.config.units, []],
      decimals: [configData.config.decimals, []],
      tooltipValueFont: [settings.tooltipValueFont, []],
      tooltipValueColor: [settings.tooltipValueColor, []],

      showTooltipDate: [settings.showTooltipDate, []],
      tooltipDateFormat: [settings.tooltipDateFormat, []],
      tooltipDateFont: [settings.tooltipDateFont, []],
      tooltipDateColor: [settings.tooltipDateColor, []],

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

    this.widgetConfig.config.settings.layout = config.layout;

    this.widgetConfig.config.settings.showDate = config.showDate;
    this.widgetConfig.config.settings.dateFormat = config.dateFormat;
    this.widgetConfig.config.settings.dateFont = config.dateFont;
    this.widgetConfig.config.settings.dateColor = config.dateColor;

    this.widgetConfig.config.settings.activeBarsColor = config.activeBarsColor;
    this.widgetConfig.config.settings.inactiveBarsColor = config.inactiveBarsColor;

    this.widgetConfig.config.settings.showTooltip = config.showTooltip;

    this.widgetConfig.config.settings.showTooltipValue = config.showTooltipValue;
    this.widgetConfig.config.units = config.units;
    this.widgetConfig.config.decimals = config.decimals;
    this.widgetConfig.config.settings.tooltipValueFont = config.tooltipValueFont;
    this.widgetConfig.config.settings.tooltipValueColor = config.tooltipValueColor;

    this.widgetConfig.config.settings.showTooltipDate = config.showTooltipDate;
    this.widgetConfig.config.settings.tooltipDateFormat = config.tooltipDateFormat;
    this.widgetConfig.config.settings.tooltipDateFont = config.tooltipDateFont;
    this.widgetConfig.config.settings.tooltipDateColor = config.tooltipDateColor;

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
    return ['showTitle', 'showIcon', 'showDate', 'showTooltip', 'showTooltipValue', 'showTooltipDate'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showTitle: boolean = this.signalStrengthWidgetConfigForm.get('showTitle').value;
    const showIcon: boolean = this.signalStrengthWidgetConfigForm.get('showIcon').value;
    const showDate: boolean = this.signalStrengthWidgetConfigForm.get('showDate').value;
    const showTooltip: boolean = this.signalStrengthWidgetConfigForm.get('showTooltip').value;
    const showTooltipValue: boolean = this.signalStrengthWidgetConfigForm.get('showTooltipValue').value;
    const showTooltipDate: boolean = this.signalStrengthWidgetConfigForm.get('showTooltipDate').value;

    if (showTitle) {
      this.signalStrengthWidgetConfigForm.get('title').enable();
      this.signalStrengthWidgetConfigForm.get('titleFont').enable();
      this.signalStrengthWidgetConfigForm.get('titleColor').enable();
      this.signalStrengthWidgetConfigForm.get('showIcon').enable({emitEvent: false});
      if (showIcon) {
        this.signalStrengthWidgetConfigForm.get('iconSize').enable();
        this.signalStrengthWidgetConfigForm.get('iconSizeUnit').enable();
        this.signalStrengthWidgetConfigForm.get('icon').enable();
        this.signalStrengthWidgetConfigForm.get('iconColor').enable();
      } else {
        this.signalStrengthWidgetConfigForm.get('iconSize').disable();
        this.signalStrengthWidgetConfigForm.get('iconSizeUnit').disable();
        this.signalStrengthWidgetConfigForm.get('icon').disable();
        this.signalStrengthWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.signalStrengthWidgetConfigForm.get('title').disable();
      this.signalStrengthWidgetConfigForm.get('titleFont').disable();
      this.signalStrengthWidgetConfigForm.get('titleColor').disable();
      this.signalStrengthWidgetConfigForm.get('showIcon').disable({emitEvent: false});
      this.signalStrengthWidgetConfigForm.get('iconSize').disable();
      this.signalStrengthWidgetConfigForm.get('iconSizeUnit').disable();
      this.signalStrengthWidgetConfigForm.get('icon').disable();
      this.signalStrengthWidgetConfigForm.get('iconColor').disable();
    }

    if (showDate) {
      this.signalStrengthWidgetConfigForm.get('dateFormat').enable();
      this.signalStrengthWidgetConfigForm.get('dateFont').enable();
      this.signalStrengthWidgetConfigForm.get('dateColor').enable();
    } else {
      this.signalStrengthWidgetConfigForm.get('dateFormat').disable();
      this.signalStrengthWidgetConfigForm.get('dateFont').disable();
      this.signalStrengthWidgetConfigForm.get('dateColor').disable();
    }

    if (showTooltip) {
      this.signalStrengthWidgetConfigForm.get('showTooltipValue').enable({emitEvent: false});
      this.signalStrengthWidgetConfigForm.get('showTooltipDate').enable({emitEvent: false});
      this.signalStrengthWidgetConfigForm.get('tooltipBackgroundColor').enable();
      this.signalStrengthWidgetConfigForm.get('tooltipBackgroundBlur').enable();
      if (showTooltipValue) {
        this.signalStrengthWidgetConfigForm.get('units').enable();
        this.signalStrengthWidgetConfigForm.get('decimals').enable();
        this.signalStrengthWidgetConfigForm.get('tooltipValueFont').enable();
        this.signalStrengthWidgetConfigForm.get('tooltipValueColor').enable();
      } else {
        this.signalStrengthWidgetConfigForm.get('units').disable();
        this.signalStrengthWidgetConfigForm.get('decimals').disable();
        this.signalStrengthWidgetConfigForm.get('tooltipValueFont').disable();
        this.signalStrengthWidgetConfigForm.get('tooltipValueColor').disable();
      }
      if (showTooltipDate) {
        this.signalStrengthWidgetConfigForm.get('tooltipDateFormat').enable();
        this.signalStrengthWidgetConfigForm.get('tooltipDateFont').enable();
        this.signalStrengthWidgetConfigForm.get('tooltipDateColor').enable();
      } else {
        this.signalStrengthWidgetConfigForm.get('tooltipDateFormat').disable();
        this.signalStrengthWidgetConfigForm.get('tooltipDateFont').disable();
        this.signalStrengthWidgetConfigForm.get('tooltipDateColor').disable();
      }
    } else {
      this.signalStrengthWidgetConfigForm.get('showTooltipValue').disable({emitEvent: false});
      this.signalStrengthWidgetConfigForm.get('showTooltipDate').disable({emitEvent: false});
      this.signalStrengthWidgetConfigForm.get('tooltipBackgroundColor').disable();
      this.signalStrengthWidgetConfigForm.get('tooltipBackgroundBlur').disable();
      this.signalStrengthWidgetConfigForm.get('units').disable();
      this.signalStrengthWidgetConfigForm.get('decimals').disable();
      this.signalStrengthWidgetConfigForm.get('tooltipValueFont').disable();
      this.signalStrengthWidgetConfigForm.get('tooltipValueColor').disable();
      this.signalStrengthWidgetConfigForm.get('tooltipDateFormat').disable();
      this.signalStrengthWidgetConfigForm.get('tooltipDateFont').disable();
      this.signalStrengthWidgetConfigForm.get('tooltipDateColor').disable();
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

  private _datePreviewFn(): string {
    const dateFormat: DateFormatSettings = this.signalStrengthWidgetConfigForm.get('dateFormat').value;
    const processor = DateFormatProcessor.fromSettings(this.$injector, dateFormat);
    processor.update(Date.now());
    return processor.formatted;
  }

  private _tooltipValuePreviewFn(): string {
    const units: string = getSourceTbUnitSymbol(this.widgetConfig.config.units);
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(-76, decimals, units, true);
  }

  private _tooltipDatePreviewFn(): string {
    const dateFormat: DateFormatSettings = this.signalStrengthWidgetConfigForm.get('tooltipDateFormat').value;
    const processor = DateFormatProcessor.fromSettings(this.$injector, {...dateFormat, ...{hideLastUpdatePrefix: true}});
    processor.update(Date.now());
    return processor.formatted;
  }
}
