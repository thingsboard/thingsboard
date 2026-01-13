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
import { WidgetConfig } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { isUndefined } from '@core/utils';
import { cssSizeToStrSize, resolveCssSize } from '@shared/models/widget-settings.models';
import {
  mobileAppQrCodeWidgetDefaultSettings,
  MobileAppQrCodeWidgetSettings
} from '@home/components/widget/lib/cards/mobile-app-qr-code-widget.models';
import { badgePositionTranslationsMap } from '@app/shared/models/mobile-app.models';

@Component({
  selector: 'tb-mobile-app-qr-code-basic-config',
  templateUrl: './mobile-app-qr-code-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class MobileAppQrCodeBasicConfigComponent extends BasicWidgetConfigComponent {

  mobileAppQrCodeWidgetConfigForm: UntypedFormGroup;
  badgePositionTranslationsMap = badgePositionTranslationsMap;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.mobileAppQrCodeWidgetConfigForm;
  }

  protected setupConfig(widgetConfig: WidgetConfigComponentData) {
    super.setupConfig(widgetConfig);
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: MobileAppQrCodeWidgetSettings = {...mobileAppQrCodeWidgetDefaultSettings, ...(configData.config.settings || {})};
    const iconSize = resolveCssSize(configData.config.iconSize);

    this.mobileAppQrCodeWidgetConfigForm = this.fb.group({
      useSystemSettings: [settings.useSystemSettings],

      badgeEnabled: [settings.qrCodeConfig.badgeEnabled],
      badgePosition: [settings.qrCodeConfig.badgePosition],
      qrCodeLabelEnabled: [settings.qrCodeConfig.qrCodeLabelEnabled],
      qrCodeLabel: [settings.qrCodeConfig.qrCodeLabel],

      showTitle: [configData.config.showTitle, []],
      title: [configData.config.title, []],
      titleFont: [configData.config.titleFont, []],
      titleColor: [configData.config.titleColor, []],
      showTitleIcon: [configData.config.showTitleIcon, []],
      iconSize: [iconSize[0], [Validators.min(0)]],
      iconSizeUnit: [iconSize[1], []],
      titleIcon: [configData.config.titleIcon, []],
      iconColor: [configData.config.iconColor, []],

      background: [settings.background, []],

      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],
      padding: [settings.padding, []],

      actions: [configData.config.actions || {}, []]
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    this.widgetConfig.config.showTitle = config.showTitle;
    this.widgetConfig.config.title = config.title;
    this.widgetConfig.config.titleFont = config.titleFont;
    this.widgetConfig.config.titleColor = config.titleColor;

    this.widgetConfig.config.showTitleIcon = config.showTitleIcon;
    this.widgetConfig.config.iconSize = cssSizeToStrSize(config.iconSize, config.iconSizeUnit);
    this.widgetConfig.config.titleIcon = config.titleIcon;
    this.widgetConfig.config.iconColor = config.iconColor;

    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};
    this.widgetConfig.config.settings.useSystemSettings = config.useSystemSettings;
    this.widgetConfig.config.settings.qrCodeConfig = {
      badgeEnabled: config.badgeEnabled,
      badgePosition: config.badgePosition,
      qrCodeLabelEnabled: config.qrCodeLabelEnabled,
      qrCodeLabel: config.qrCodeLabel
    };

    this.widgetConfig.config.settings.background = config.background;
    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;
    this.widgetConfig.config.settings.padding = config.padding;

    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return ['showTitleIcon', 'badgeEnabled', 'qrCodeLabelEnabled', 'showTitleIcon', 'showTitle'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const useSystemSettings = this.mobileAppQrCodeWidgetConfigForm.get('useSystemSettings').value;

    if (!useSystemSettings) {
      const badgeEnabled = this.mobileAppQrCodeWidgetConfigForm.get('badgeEnabled').value;
      const qrCodeLabelEnabled = this.mobileAppQrCodeWidgetConfigForm.get('qrCodeLabelEnabled').value;
      const showTitleIcon: boolean = this.mobileAppQrCodeWidgetConfigForm.get('showTitleIcon').value;
      const showTitle: boolean = this.mobileAppQrCodeWidgetConfigForm.get('showTitle').value;

      if (badgeEnabled) {
        this.mobileAppQrCodeWidgetConfigForm.get('badgePosition').enable({emitEvent: false});
      } else {
        this.mobileAppQrCodeWidgetConfigForm.get('badgePosition').disable({emitEvent: false});
      }
      if (qrCodeLabelEnabled) {
        this.mobileAppQrCodeWidgetConfigForm.get('qrCodeLabel').enable({emitEvent: false});
      } else {
        this.mobileAppQrCodeWidgetConfigForm.get('qrCodeLabel').disable({emitEvent: false});
      }
      if (showTitle) {
        this.mobileAppQrCodeWidgetConfigForm.get('title').enable({emitEvent: false});
        this.mobileAppQrCodeWidgetConfigForm.get('titleFont').enable({emitEvent: false});
        this.mobileAppQrCodeWidgetConfigForm.get('titleColor').enable({emitEvent: false});
        this.mobileAppQrCodeWidgetConfigForm.get('showTitleIcon').enable({emitEvent: false});
        if (showTitleIcon) {
          this.mobileAppQrCodeWidgetConfigForm.get('titleIcon').enable({emitEvent: false});
          this.mobileAppQrCodeWidgetConfigForm.get('iconColor').enable({emitEvent: false});
          this.mobileAppQrCodeWidgetConfigForm.get('iconSize').enable({emitEvent: false});
          this.mobileAppQrCodeWidgetConfigForm.get('iconSizeUnit').enable({emitEvent: false});
        } else {
          this.mobileAppQrCodeWidgetConfigForm.get('titleIcon').disable({emitEvent: false});
          this.mobileAppQrCodeWidgetConfigForm.get('iconColor').disable({emitEvent: false});
          this.mobileAppQrCodeWidgetConfigForm.get('iconSize').disable({emitEvent: false});
          this.mobileAppQrCodeWidgetConfigForm.get('iconSizeUnit').disable({emitEvent: false});
        }
      } else {
        this.mobileAppQrCodeWidgetConfigForm.get('title').disable({emitEvent: false});
        this.mobileAppQrCodeWidgetConfigForm.get('titleFont').disable({emitEvent: false});
        this.mobileAppQrCodeWidgetConfigForm.get('titleColor').disable({emitEvent: false});
        this.mobileAppQrCodeWidgetConfigForm.get('showTitleIcon').disable({emitEvent: false});
        this.mobileAppQrCodeWidgetConfigForm.get('titleIcon').disable({emitEvent: false});
        this.mobileAppQrCodeWidgetConfigForm.get('iconColor').disable({emitEvent: false});
        this.mobileAppQrCodeWidgetConfigForm.get('iconSize').disable({emitEvent: false});
        this.mobileAppQrCodeWidgetConfigForm.get('iconSizeUnit').disable({emitEvent: false});
      }
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
