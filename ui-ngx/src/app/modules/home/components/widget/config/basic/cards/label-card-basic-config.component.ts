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
import { WidgetConfig, } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { isUndefined } from '@core/utils';
import {
  labelCardWidgetDefaultSettings,
  LabelCardWidgetSettings
} from '@home/components/widget/lib/cards/label-card-widget.models';

@Component({
  selector: 'tb-label-card-basic-config',
  templateUrl: './label-card-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class LabelCardBasicConfigComponent extends BasicWidgetConfigComponent {

  labelCardWidgetConfigForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.labelCardWidgetConfigForm;
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: LabelCardWidgetSettings = {...labelCardWidgetDefaultSettings, ...(configData.config.settings || {})};
    this.labelCardWidgetConfigForm = this.fb.group({
      autoScale: [settings.autoScale, []],

      label: [settings.label, []],
      labelFont: [settings.labelFont, []],
      labelColor: [settings.labelColor, []],

      showIcon: [settings.showIcon, []],
      iconSize: [settings.iconSize, [Validators.min(0)]],
      iconSizeUnit: [settings.iconSizeUnit, []],
      icon: [settings.icon, []],
      iconColor: [settings.iconColor, []],

      background: [settings.background, []],

      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],
      padding: [settings.padding, []],

      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};

    this.widgetConfig.config.settings.autoScale = config.autoScale;

    this.widgetConfig.config.settings.label = config.label;
    this.widgetConfig.config.settings.labelFont = config.labelFont;
    this.widgetConfig.config.settings.labelColor = config.labelColor;

    this.widgetConfig.config.settings.showIcon = config.showIcon;
    this.widgetConfig.config.settings.iconSize = config.iconSize;
    this.widgetConfig.config.settings.iconSizeUnit = config.iconSizeUnit;
    this.widgetConfig.config.settings.icon = config.icon;
    this.widgetConfig.config.settings.iconColor = config.iconColor;

    this.widgetConfig.config.settings.background = config.background;

    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;
    this.widgetConfig.config.settings.padding = config.padding;

    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showIcon'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const showIcon: boolean = this.labelCardWidgetConfigForm.get('showIcon').value;
    if (showIcon) {
      this.labelCardWidgetConfigForm.get('iconSize').enable();
      this.labelCardWidgetConfigForm.get('iconSizeUnit').enable();
      this.labelCardWidgetConfigForm.get('icon').enable();
      this.labelCardWidgetConfigForm.get('iconColor').enable();
    } else {
      this.labelCardWidgetConfigForm.get('iconSize').disable();
      this.labelCardWidgetConfigForm.get('iconSizeUnit').disable();
      this.labelCardWidgetConfigForm.get('icon').disable();
      this.labelCardWidgetConfigForm.get('iconColor').disable();
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
