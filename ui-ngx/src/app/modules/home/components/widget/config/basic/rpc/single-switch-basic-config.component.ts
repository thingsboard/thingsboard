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
import { TargetDevice, WidgetConfig, } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { isUndefined } from '@core/utils';
import {
  singleSwitchDefaultSettings,
  singleSwitchLayoutImages,
  singleSwitchLayouts,
  singleSwitchLayoutTranslations,
  SingleSwitchWidgetSettings
} from '@home/components/widget/lib/rpc/single-switch-widget.models';
import { ValueType } from '@shared/models/constants';

@Component({
    selector: 'tb-single-switch-basic-config',
    templateUrl: './single-switch-basic-config.component.html',
    styleUrls: ['../basic-config.scss'],
    standalone: false
})
export class SingleSwitchBasicConfigComponent extends BasicWidgetConfigComponent {

  get targetDevice(): TargetDevice {
    return this.singleSwitchWidgetConfigForm.get('targetDevice').value;
  }

  singleSwitchLayouts = singleSwitchLayouts;

  singleSwitchLayoutTranslationMap = singleSwitchLayoutTranslations;
  singleSwitchLayoutImageMap = singleSwitchLayoutImages;

  valueType = ValueType;

  singleSwitchWidgetConfigForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.singleSwitchWidgetConfigForm;
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: SingleSwitchWidgetSettings = {...singleSwitchDefaultSettings, ...(configData.config.settings || {})};
    this.singleSwitchWidgetConfigForm = this.fb.group({
      targetDevice: [configData.config.targetDevice, []],

      initialState: [settings.initialState, []],
      onUpdateState: [settings.onUpdateState, []],
      offUpdateState: [settings.offUpdateState, []],
      disabledState: [settings.disabledState, []],

      layout: [settings.layout, []],
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

      switchColorOn: [settings.switchColorOn, []],
      switchColorOff: [settings.switchColorOff, []],
      switchColorDisabled: [settings.switchColorDisabled, []],

      tumblerColorOn: [settings.tumblerColorOn, []],
      tumblerColorOff: [settings.tumblerColorOff, []],
      tumblerColorDisabled: [settings.tumblerColorDisabled, []],

      showOnLabel: [settings.showOnLabel, []],
      onLabel: [settings.onLabel, []],
      onLabelFont: [settings.onLabelFont, []],
      onLabelColor: [settings.onLabelColor, []],

      showOffLabel: [settings.showOffLabel, []],
      offLabel: [settings.offLabel, []],
      offLabelFont: [settings.offLabelFont, []],
      offLabelColor: [settings.offLabelColor, []],

      background: [settings.background, []],

      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],
      padding: [settings.padding, []],

      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    this.widgetConfig.config.targetDevice = config.targetDevice;

    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};

    this.widgetConfig.config.settings.initialState = config.initialState;
    this.widgetConfig.config.settings.onUpdateState = config.onUpdateState;
    this.widgetConfig.config.settings.offUpdateState = config.offUpdateState;
    this.widgetConfig.config.settings.disabledState = config.disabledState;

    this.widgetConfig.config.settings.layout = config.layout;
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

    this.widgetConfig.config.settings.switchColorOn = config.switchColorOn;
    this.widgetConfig.config.settings.switchColorOff = config.switchColorOff;
    this.widgetConfig.config.settings.switchColorDisabled = config.switchColorDisabled;

    this.widgetConfig.config.settings.tumblerColorOn = config.tumblerColorOn;
    this.widgetConfig.config.settings.tumblerColorOff = config.tumblerColorOff;
    this.widgetConfig.config.settings.tumblerColorDisabled = config.tumblerColorDisabled;

    this.widgetConfig.config.settings.showOnLabel = config.showOnLabel;
    this.widgetConfig.config.settings.onLabel = config.onLabel;
    this.widgetConfig.config.settings.onLabelFont = config.onLabelFont;
    this.widgetConfig.config.settings.onLabelColor = config.onLabelColor;

    this.widgetConfig.config.settings.showOffLabel = config.showOffLabel;
    this.widgetConfig.config.settings.offLabel = config.offLabel;
    this.widgetConfig.config.settings.offLabelFont = config.offLabelFont;
    this.widgetConfig.config.settings.offLabelColor = config.offLabelColor;

    this.widgetConfig.config.settings.background = config.background;

    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;
    this.widgetConfig.config.settings.padding = config.padding;

    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showLabel', 'showIcon', 'showOnLabel', 'showOffLabel'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showLabel: boolean = this.singleSwitchWidgetConfigForm.get('showLabel').value;
    const showIcon: boolean = this.singleSwitchWidgetConfigForm.get('showIcon').value;
    const showOnLabel: boolean = this.singleSwitchWidgetConfigForm.get('showOnLabel').value;
    const showOffLabel: boolean = this.singleSwitchWidgetConfigForm.get('showOffLabel').value;

    if (showLabel) {
      this.singleSwitchWidgetConfigForm.get('label').enable();
      this.singleSwitchWidgetConfigForm.get('labelFont').enable();
      this.singleSwitchWidgetConfigForm.get('labelColor').enable();
    } else {
      this.singleSwitchWidgetConfigForm.get('label').disable();
      this.singleSwitchWidgetConfigForm.get('labelFont').disable();
      this.singleSwitchWidgetConfigForm.get('labelColor').disable();
    }

    if (showIcon) {
      this.singleSwitchWidgetConfigForm.get('iconSize').enable();
      this.singleSwitchWidgetConfigForm.get('iconSizeUnit').enable();
      this.singleSwitchWidgetConfigForm.get('icon').enable();
      this.singleSwitchWidgetConfigForm.get('iconColor').enable();
    } else {
      this.singleSwitchWidgetConfigForm.get('iconSize').disable();
      this.singleSwitchWidgetConfigForm.get('iconSizeUnit').disable();
      this.singleSwitchWidgetConfigForm.get('icon').disable();
      this.singleSwitchWidgetConfigForm.get('iconColor').disable();
    }

    if (showOnLabel) {
      this.singleSwitchWidgetConfigForm.get('onLabel').enable();
      this.singleSwitchWidgetConfigForm.get('onLabelFont').enable();
      this.singleSwitchWidgetConfigForm.get('onLabelColor').enable();
    } else {
      this.singleSwitchWidgetConfigForm.get('onLabel').disable();
      this.singleSwitchWidgetConfigForm.get('onLabelFont').disable();
      this.singleSwitchWidgetConfigForm.get('onLabelColor').disable();
    }

    if (showOffLabel) {
      this.singleSwitchWidgetConfigForm.get('offLabel').enable();
      this.singleSwitchWidgetConfigForm.get('offLabelFont').enable();
      this.singleSwitchWidgetConfigForm.get('offLabelColor').enable();
    } else {
      this.singleSwitchWidgetConfigForm.get('offLabel').disable();
      this.singleSwitchWidgetConfigForm.get('offLabelFont').disable();
      this.singleSwitchWidgetConfigForm.get('offLabelColor').disable();
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

}
