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

import { Component } from "@angular/core";
import { UntypedFormBuilder, UntypedFormGroup } from "@angular/forms";
import { WidgetSettings, WidgetSettingsComponent } from "@shared/models/widget.models";
import { AppState } from '@core/core.state';
import { Store } from "@ngrx/store";
import { badgePositionTranslationsMap } from '@shared/models/mobile-app.models';
import { mobileAppQrCodeWidgetDefaultSettings } from '@home/components/widget/lib/cards/mobile-app-qr-code-widget.models';

@Component({
    selector: 'tb-mobile-app-qr-code-widget-settings',
    templateUrl: './mobile-app-qr-code-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class MobileAppQrCodeWidgetSettingsComponent extends WidgetSettingsComponent {

  mobileAppQRCodeWidgetSettingsForm: UntypedFormGroup;

  badgePositionTranslationsMap = badgePositionTranslationsMap;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.mobileAppQRCodeWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return mobileAppQrCodeWidgetDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.mobileAppQRCodeWidgetSettingsForm = this.fb.group({
      useSystemSettings: [settings.useSystemSettings],
      qrCodeConfig: this.fb.group({
        badgeEnabled: [settings.qrCodeConfig.badgeEnabled],
        badgePosition: [settings.qrCodeConfig.badgePosition],
        qrCodeLabelEnabled: [settings.qrCodeConfig.qrCodeLabelEnabled],
        qrCodeLabel: [settings.qrCodeConfig.qrCodeLabel]
      }),
      background: [settings.background],
      padding: [settings.padding, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['useSystemSettings', 'qrCodeConfig.badgeEnabled', 'qrCodeConfig.qrCodeLabelEnabled'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useSystemSettings = this.mobileAppQRCodeWidgetSettingsForm.get('useSystemSettings').value;
    if (!useSystemSettings) {
      const badgeEnabled = this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgeEnabled').value;
      const qrCodeLabelEnabled = this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.qrCodeLabelEnabled').value;
      if (badgeEnabled) {
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgePosition').enable({emitEvent: false});
      } else {
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgePosition').disable({emitEvent: false});
      }
      if (qrCodeLabelEnabled) {
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.qrCodeLabel').enable({emitEvent: false});
      } else {
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.qrCodeLabel').disable({emitEvent: false});
      }
    }
  }

}
