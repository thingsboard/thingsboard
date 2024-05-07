///
/// Copyright © 2016-2024 The Thingsboard Authors
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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from "@angular/forms";
import { WidgetSettings, WidgetSettingsComponent } from "@shared/models/widget.models";
import { AppState } from '@core/core.state';
import { Store } from "@ngrx/store";
import { badgePositionTranslationsMap, badgeStyleTranslationsMap } from '@shared/models/mobile-app.models';
import { mobileAppQrCodeWidgetDefaultSettings } from '@home/components/widget/lib/cards/mobile-app-qr-code-widge.models';

@Component({
  selector: 'tb-mobile-app-qr-code-widget-settings',
  templateUrl: './mobile-app-qr-code-widget-settings.component.html',
  styleUrls: ['/mobile-app-qr-code-widget-settings.component.scss', './../widget-settings.scss']
})
export class MobileAppQrCodeWidgetSettingsComponent extends WidgetSettingsComponent {

  mobileAppQRCodeWidgetSettingsForm: UntypedFormGroup;

  badgePositionTranslationsMap = badgePositionTranslationsMap;
  badgeStyleTranslationsMap = badgeStyleTranslationsMap;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.mobileAppQRCodeWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {...mobileAppQrCodeWidgetDefaultSettings};
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.mobileAppQRCodeWidgetSettingsForm = this.fb.group({
      useDefaultApp: [settings.useDefaultApp, []],
      androidConfig: this.fb.group({
        enabled: [settings.androidConfig.enabled, []],
        appPackage: [settings.androidConfig.appPackage, []],
        sha256CertFingerprints: [settings.androidConfig.sha256CertFingerprints, []]
      }),
      iosConfig: this.fb.group({
        enabled: [settings.iosConfig.enabled, []],
        appId: [settings.iosConfig.appId, []]
      }),
      qrCodeConfig: this.fb.group({
        badgeEnabled: [settings.qrCodeConfig.badgeEnabled, []],
        badgeStyle: [{value: settings.qrCodeConfig.badgeStyle, disabled: true}, []],
        badgePosition: [{value: settings.qrCodeConfig.badgePosition, disabled: true}, []],
        qrCodeLabelEnabled: [settings.qrCodeConfig.qrCodeLabelEnabled, []],
        qrCodeLabel: [settings.qrCodeConfig.qrCodeLabel, []]
      })
    });
  }

  protected validatorTriggers(): string[] {
    return ['useDefaultApp', 'androidConfig.enabled', 'iosConfig.enabled', 'qrCodeConfig.badgeEnabled', 'qrCodeConfig.qrCodeLabelEnabled'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useDefaultApp = this.mobileAppQRCodeWidgetSettingsForm.get('useDefaultApp').value;
    const androidEnabled = this.mobileAppQRCodeWidgetSettingsForm.get('androidConfig.enabled').value;
    const iosEnabled = this.mobileAppQRCodeWidgetSettingsForm.get('iosConfig.enabled').value;
    const badgeEnabled = this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgeEnabled').value;
    const qrCodeLabelEnabled = this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.qrCodeLabelEnabled').value;

    if (useDefaultApp) {
      this.mobileAppQRCodeWidgetSettingsForm.get('androidConfig.appPackage').clearValidators();
      this.mobileAppQRCodeWidgetSettingsForm.get('androidConfig.sha256CertFingerprints').clearValidators();
      this.mobileAppQRCodeWidgetSettingsForm.get('iosConfig.appId').clearValidators();
    } else {
      this.mobileAppQRCodeWidgetSettingsForm.get('androidConfig.appPackage').setValidators([Validators.required]);
      this.mobileAppQRCodeWidgetSettingsForm.get('androidConfig.sha256CertFingerprints').setValidators([Validators.required]);
      this.mobileAppQRCodeWidgetSettingsForm.get('iosConfig.appId').setValidators([Validators.required]);
    }

    if (androidEnabled) {
      if (!useDefaultApp) {
        this.mobileAppQRCodeWidgetSettingsForm.get('androidConfig.appPackage').setValidators([Validators.required]);
        this.mobileAppQRCodeWidgetSettingsForm.get('androidConfig.sha256CertFingerprints').setValidators([Validators.required]);
      }
      this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgeEnabled').enable({emitEvent: false});
      if (badgeEnabled) {
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgeStyle').enable();
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgePosition').enable();
      }
    } else {
      if (!useDefaultApp) {
        this.mobileAppQRCodeWidgetSettingsForm.get('androidConfig.appPackage').clearValidators();
        this.mobileAppQRCodeWidgetSettingsForm.get('androidConfig.sha256CertFingerprints').clearValidators();
      }
      if (!iosEnabled) {
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgeEnabled').disable({emitEvent: false});
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgeStyle').disable();
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgePosition').disable();
      }
    }

    if (iosEnabled) {
      if (!useDefaultApp) {
        this.mobileAppQRCodeWidgetSettingsForm.get('iosConfig.appId').setValidators([Validators.required]);
      }
      this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgeEnabled').enable({emitEvent: false});
      if (badgeEnabled) {
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgeStyle').enable();
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgePosition').enable();
      }
    } else {
      if (!useDefaultApp) {
        this.mobileAppQRCodeWidgetSettingsForm.get('iosConfig.appId').clearValidators();
      }
      if (!androidEnabled) {
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgeEnabled').disable({emitEvent: false});
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgeStyle').disable();
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgePosition').disable();
      }
    }

    if (badgeEnabled) {
      if (androidEnabled || iosEnabled) {
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgeStyle').enable();
        this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgePosition').enable();
      }
    } else {
      this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgeStyle').disable();
      this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.badgePosition').disable();
    }

    if (qrCodeLabelEnabled) {
      this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.qrCodeLabel').enable();
    } else {
      this.mobileAppQRCodeWidgetSettingsForm.get('qrCodeConfig.qrCodeLabel').disable();
    }

    this.mobileAppQRCodeWidgetSettingsForm.get('androidConfig.appPackage').updateValueAndValidity({emitEvent});
    this.mobileAppQRCodeWidgetSettingsForm.get('androidConfig.sha256CertFingerprints').updateValueAndValidity({emitEvent});
    this.mobileAppQRCodeWidgetSettingsForm.get('iosConfig.appId').updateValueAndValidity({emitEvent});
  }

}
