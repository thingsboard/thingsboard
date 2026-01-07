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
import { TargetDevice, TargetDeviceType, WidgetConfig, } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { isUndefined } from '@core/utils';
import { ValueType } from '@shared/models/constants';
import { cssSizeToStrSize, resolveCssSize } from '@shared/models/widget-settings.models';
import {
  toggleButtonDefaultSettings,
  ToggleButtonWidgetSettings
} from '@home/components/widget/lib/button/toggle-button-widget.models';

type ButtonAppearanceType = 'checked' | 'unchecked';

@Component({
  selector: 'tb-toggle-button-basic-config',
  templateUrl: './toggle-button-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class ToggleButtonBasicConfigComponent extends BasicWidgetConfigComponent {

  get targetDevice(): TargetDevice {
    return this.toggleButtonWidgetConfigForm.get('targetDevice').value;
  }

  valueType = ValueType;

  buttonAppearanceType: ButtonAppearanceType = 'checked';

  toggleButtonWidgetConfigForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.toggleButtonWidgetConfigForm;
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: ToggleButtonWidgetSettings = {...toggleButtonDefaultSettings, ...(configData.config.settings || {})};
    const iconSize = resolveCssSize(configData.config.iconSize);
    this.toggleButtonWidgetConfigForm = this.fb.group({
      targetDevice: [configData.config.targetDevice, []],

      initialState: [settings.initialState, []],
      checkState: [settings.checkState, []],
      uncheckState: [settings.uncheckState, []],
      disabledState: [settings.disabledState, []],

      showTitle: [configData.config.showTitle, []],
      title: [configData.config.title, []],
      titleFont: [configData.config.titleFont, []],
      titleColor: [configData.config.titleColor, []],

      showIcon: [configData.config.showTitleIcon, []],
      iconSize: [iconSize[0], [Validators.min(0)]],
      iconSizeUnit: [iconSize[1], []],
      icon: [configData.config.titleIcon, []],
      iconColor: [configData.config.iconColor, []],

      autoScale: [settings.autoScale, []],
      horizontalFill: [settings.horizontalFill, []],
      verticalFill: [settings.verticalFill, []],

      checkedAppearance: [settings.checkedAppearance, []],
      uncheckedAppearance: [settings.uncheckedAppearance, []],

      background: [settings.background, []],

      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],
      padding: [settings.padding, []],

      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    this.widgetConfig.config.targetDevice = config.targetDevice;

    this.widgetConfig.config.showTitle = config.showTitle;
    this.widgetConfig.config.title = config.title;
    this.widgetConfig.config.titleFont = config.titleFont;
    this.widgetConfig.config.titleColor = config.titleColor;

    this.widgetConfig.config.showTitleIcon = config.showIcon;
    this.widgetConfig.config.iconSize = cssSizeToStrSize(config.iconSize, config.iconSizeUnit);
    this.widgetConfig.config.titleIcon = config.icon;
    this.widgetConfig.config.iconColor = config.iconColor;

    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};

    this.widgetConfig.config.settings.initialState = config.initialState;
    this.widgetConfig.config.settings.checkState = config.checkState;
    this.widgetConfig.config.settings.uncheckState = config.uncheckState;
    this.widgetConfig.config.settings.disabledState = config.disabledState;

    this.widgetConfig.config.settings.autoScale = config.autoScale;
    this.widgetConfig.config.settings.horizontalFill = config.horizontalFill;
    this.widgetConfig.config.settings.verticalFill = config.verticalFill;

    this.widgetConfig.config.settings.checkedAppearance = config.checkedAppearance;
    this.widgetConfig.config.settings.uncheckedAppearance = config.uncheckedAppearance;

    this.widgetConfig.config.settings.background = config.background;

    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;
    this.widgetConfig.config.settings.padding = config.padding;

    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showTitle', 'showIcon', 'autoScale'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showTitle: boolean = this.toggleButtonWidgetConfigForm.get('showTitle').value;
    const showIcon: boolean = this.toggleButtonWidgetConfigForm.get('showIcon').value;
    const autoScale: boolean = this.toggleButtonWidgetConfigForm.get('autoScale').value;
    if (showTitle) {
      this.toggleButtonWidgetConfigForm.get('title').enable();
      this.toggleButtonWidgetConfigForm.get('titleFont').enable();
      this.toggleButtonWidgetConfigForm.get('titleColor').enable();
      this.toggleButtonWidgetConfigForm.get('showIcon').enable({emitEvent: false});
      if (showIcon) {
        this.toggleButtonWidgetConfigForm.get('iconSize').enable();
        this.toggleButtonWidgetConfigForm.get('iconSizeUnit').enable();
        this.toggleButtonWidgetConfigForm.get('icon').enable();
        this.toggleButtonWidgetConfigForm.get('iconColor').enable();
      } else {
        this.toggleButtonWidgetConfigForm.get('iconSize').disable();
        this.toggleButtonWidgetConfigForm.get('iconSizeUnit').disable();
        this.toggleButtonWidgetConfigForm.get('icon').disable();
        this.toggleButtonWidgetConfigForm.get('iconColor').disable();
      }
    } else {
      this.toggleButtonWidgetConfigForm.get('title').disable();
      this.toggleButtonWidgetConfigForm.get('titleFont').disable();
      this.toggleButtonWidgetConfigForm.get('titleColor').disable();
      this.toggleButtonWidgetConfigForm.get('showIcon').disable({emitEvent: false});
      this.toggleButtonWidgetConfigForm.get('iconSize').disable();
      this.toggleButtonWidgetConfigForm.get('iconSizeUnit').disable();
      this.toggleButtonWidgetConfigForm.get('icon').disable();
      this.toggleButtonWidgetConfigForm.get('iconColor').disable();
    }
    if (autoScale) {
      this.toggleButtonWidgetConfigForm.get('horizontalFill').disable();
      this.toggleButtonWidgetConfigForm.get('verticalFill').disable();
    } else {
      this.toggleButtonWidgetConfigForm.get('horizontalFill').enable();
      this.toggleButtonWidgetConfigForm.get('verticalFill').enable();
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

  protected readonly TargetDeviceType = TargetDeviceType;
}
