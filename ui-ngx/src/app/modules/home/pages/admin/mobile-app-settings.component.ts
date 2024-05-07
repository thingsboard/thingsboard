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

import { Component, OnDestroy } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { Subject, takeUntil } from 'rxjs';
import { MobileAppService } from '@core/http/mobile-app.service';
import {
  BadgePosition,
  badgePositionTranslationsMap,
  BadgeStyle,
  badgeStyleTranslationsMap,
  MobileAppQRCodeSettings
} from '@shared/models/mobile-app.models';
import { AuthService } from '@core/auth/auth.service';

@Component({
  selector: 'tb-mobile-app-settings',
  templateUrl: './mobile-app-settings.component.html',
  styleUrls: ['mobile-app-settings.component.scss', './settings-card.scss']
})
export class MobileAppSettingsComponent extends PageComponent implements HasConfirmForm, OnDestroy {

  mobileAppSettingsForm: FormGroup;

  mobileAppSettings: MobileAppQRCodeSettings;

  private readonly destroy$ = new Subject<void>();

  badgePositionTranslationsMap = badgePositionTranslationsMap;
  badgeStyleTranslationsMap = badgeStyleTranslationsMap;

  constructor(protected store: Store<AppState>,
              private mobileAppService: MobileAppService,
              private fb: FormBuilder) {
    super(store);
    this.buildMobileAppSettingsForm();
    this.mobileAppService.getMobileAppSettings()
      .subscribe(settings => this.processMobileAppSettings(settings));
    this.mobileAppSettingsForm.get('useDefaultApp').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value) {
        this.mobileAppSettingsForm.get('androidConfig.appPackage').clearValidators();
        this.mobileAppSettingsForm.get('androidConfig.sha256CertFingerprints').clearValidators();
        this.mobileAppSettingsForm.get('iosConfig.appId').clearValidators();
      } else {
        this.mobileAppSettingsForm.get('androidConfig.appPackage').setValidators([Validators.required]);
        this.mobileAppSettingsForm.get('androidConfig.sha256CertFingerprints').setValidators([Validators.required]);
        this.mobileAppSettingsForm.get('iosConfig.appId').setValidators([Validators.required]);
      }
      this.mobileAppSettingsForm.get('androidConfig.appPackage').updateValueAndValidity();
      this.mobileAppSettingsForm.get('androidConfig.sha256CertFingerprints').updateValueAndValidity();
      this.mobileAppSettingsForm.get('iosConfig.appId').updateValueAndValidity();
    });
    this.mobileAppSettingsForm.get('androidConfig.enabled').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (!this.mobileAppSettingsForm.get('useDefaultApp').value) {
        if (value) {
          this.mobileAppSettingsForm.get('androidConfig.appPackage').setValidators([Validators.required]);
          this.mobileAppSettingsForm.get('androidConfig.sha256CertFingerprints').setValidators([Validators.required]);
        } else {
          this.mobileAppSettingsForm.get('androidConfig.appPackage').clearValidators();
          this.mobileAppSettingsForm.get('androidConfig.sha256CertFingerprints').clearValidators();
        }
        this.mobileAppSettingsForm.get('androidConfig.appPackage').updateValueAndValidity();
        this.mobileAppSettingsForm.get('androidConfig.sha256CertFingerprints').updateValueAndValidity();
      }
      if (this.mobileAppSettingsForm.get('qrCodeConfig.showOnHomePage').value) {
        if (value) {
          this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').enable();
          if (this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').value) {
            this.mobileAppSettingsForm.get('qrCodeConfig.badgeStyle').enable();
            this.mobileAppSettingsForm.get('qrCodeConfig.badgePosition').enable();
          }
        } else {
          if (!this.mobileAppSettingsForm.get('iosConfig.enabled').value) {
            this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').disable();
            this.mobileAppSettingsForm.get('qrCodeConfig.badgeStyle').disable();
            this.mobileAppSettingsForm.get('qrCodeConfig.badgePosition').disable();
          }
        }
      }
    });
    this.mobileAppSettingsForm.get('iosConfig.enabled').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (!this.mobileAppSettingsForm.get('useDefaultApp').value) {
        if (value) {
          this.mobileAppSettingsForm.get('iosConfig.appId').setValidators([Validators.required]);
        } else {
          this.mobileAppSettingsForm.get('iosConfig.appId').clearValidators();
        }
        this.mobileAppSettingsForm.get('iosConfig.appId').updateValueAndValidity();
      }
      if (this.mobileAppSettingsForm.get('qrCodeConfig.showOnHomePage').value) {
        if (value) {
          this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').enable();
          if (this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').value) {
            this.mobileAppSettingsForm.get('qrCodeConfig.badgeStyle').enable();
            this.mobileAppSettingsForm.get('qrCodeConfig.badgePosition').enable();
          }
        } else {
          if (!this.mobileAppSettingsForm.get('androidConfig.enabled').value) {
            this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').disable();
            this.mobileAppSettingsForm.get('qrCodeConfig.badgeStyle').disable();
            this.mobileAppSettingsForm.get('qrCodeConfig.badgePosition').disable();
          }
        }
      }
    });
    this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value) {
        if (this.mobileAppSettingsForm.get('androidConfig.enabled').value || this.mobileAppSettingsForm.get('iosConfig.enabled').value) {
          this.mobileAppSettingsForm.get('qrCodeConfig.badgeStyle').enable();
          this.mobileAppSettingsForm.get('qrCodeConfig.badgePosition').enable();
        }
      } else {
        this.mobileAppSettingsForm.get('qrCodeConfig.badgeStyle').disable();
        this.mobileAppSettingsForm.get('qrCodeConfig.badgePosition').disable();
      }
    });
    this.mobileAppSettingsForm.get('qrCodeConfig.qrCodeLabelEnabled').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value) {
        this.mobileAppSettingsForm.get('qrCodeConfig.qrCodeLabel').enable();
      } else {
        this.mobileAppSettingsForm.get('qrCodeConfig.qrCodeLabel').disable();
      }
    });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  private buildMobileAppSettingsForm() {
    this.mobileAppSettingsForm = this.fb.group({
      useDefaultApp: [true, []],
      androidConfig: this.fb.group({
        enabled: [true, []],
        appPackage: ['', []],
        sha256CertFingerprints: ['', []]
      }),
      iosConfig: this.fb.group({
        enabled: [true, []],
        appId: ['', []]
      }),
      qrCodeConfig: this.fb.group({
        showOnHomePage: [true, []],
        badgeEnabled: [true, []],
        badgeStyle: [{value: BadgeStyle.ORIGINAL, disabled: true}, []],
        badgePosition: [{value: BadgePosition.RIGHT, disabled: true}, []],
        qrCodeLabelEnabled: [true, []],
        qrCodeLabel: ['', []]
      })
    });
  }

  private processMobileAppSettings(mobileAppSettings: MobileAppQRCodeSettings): void {
    this.mobileAppSettings = {...mobileAppSettings};
    this.mobileAppSettingsForm.reset(this.mobileAppSettings);
  }

  save(): void {
    this.mobileAppSettings = {...this.mobileAppSettings, ...this.mobileAppSettingsForm.getRawValue()};
    this.mobileAppService.saveMobileAppSettings(this.mobileAppSettings)
      .subscribe((settings) => this.processMobileAppSettings(settings));
  }

  confirmForm(): FormGroup {
    return this.mobileAppSettingsForm;
  }

}
