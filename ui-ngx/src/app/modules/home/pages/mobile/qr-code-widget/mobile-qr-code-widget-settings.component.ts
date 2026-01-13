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
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { MobileApplicationService } from '@core/http/mobile-application.service';
import { BadgePosition, badgePositionTranslationsMap, QrCodeSettings } from '@shared/models/mobile-app.models';
import { ActionUpdateMobileQrCodeEnabled } from '@core/auth/auth.actions';
import { EntityType } from '@shared/models/entity-type.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-mobile-qr-code-widget',
  templateUrl: './mobile-qr-code-widget-settings.component.html',
  styleUrls: ['mobile-qr-code-widget-settings.component.scss', '../../admin/settings-card.scss']
})
export class MobileQrCodeWidgetSettingsComponent extends PageComponent implements HasConfirmForm {

  readonly badgePositionTranslationsMap = badgePositionTranslationsMap;
  readonly entityType = EntityType;

  mobileAppSettingsForm = this.fb.group({
    useDefaultApp: [true],
    mobileAppBundleId: [{value: null, disabled: true}, Validators.required],
    androidEnabled: [true],
    iosEnabled: [true],
    qrCodeConfig: this.fb.group({
      showOnHomePage: [true],
      badgeEnabled: [true],
      badgePosition: [BadgePosition.RIGHT],
      qrCodeLabelEnabled: [true],
      qrCodeLabel: ['', [Validators.required, Validators.maxLength(50)]]
    })
  });

  private mobileAppSettings: QrCodeSettings;

  constructor(protected store: Store<AppState>,
              private mobileAppService: MobileApplicationService,
              private fb: FormBuilder) {
    super(store);
    this.mobileAppService.getMobileAppSettings()
      .subscribe(settings => this.processMobileAppSettings(settings));
    this.mobileAppSettingsForm.get('useDefaultApp').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      if (value) {
        this.mobileAppSettingsForm.get('mobileAppBundleId').disable({emitEvent: false});
      } else {
        this.mobileAppSettingsForm.get('mobileAppBundleId').enable({emitEvent: false});
      }
    });
    this.mobileAppSettingsForm.get('androidEnabled').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(() => {
      this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').updateValueAndValidity({onlySelf: true});
    });
    this.mobileAppSettingsForm.get('iosEnabled').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(() => {
      this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').updateValueAndValidity({onlySelf: true});
    });
    this.mobileAppSettingsForm.get('qrCodeConfig.showOnHomePage').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      if (value) {
        this.mobileAppSettingsForm.get('qrCodeConfig').enable({emitEvent: false});
      } else {
        this.mobileAppSettingsForm.get('qrCodeConfig').disable({emitEvent: false});
        this.mobileAppSettingsForm.get('qrCodeConfig.showOnHomePage').enable({emitEvent: false});
      }
      this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').updateValueAndValidity({onlySelf: true});
      this.mobileAppSettingsForm.get('qrCodeConfig.qrCodeLabelEnabled').updateValueAndValidity({onlySelf: true});
    });
    this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      if (value) {
        const formValue = this.mobileAppSettingsForm.getRawValue();
        if (formValue.androidEnabled  || formValue.iosEnabled) {
          this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').enable({emitEvent: false});
          this.mobileAppSettingsForm.get('qrCodeConfig.badgePosition').enable({emitEvent: false});
        } else {
          this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').disable({emitEvent: false});
          this.mobileAppSettingsForm.get('qrCodeConfig.badgePosition').disable({emitEvent: false});
        }
      } else {
        this.mobileAppSettingsForm.get('qrCodeConfig.badgePosition').disable({emitEvent: false});
      }
    });
    this.mobileAppSettingsForm.get('qrCodeConfig.qrCodeLabelEnabled').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      if (value && this.mobileAppSettingsForm.get('qrCodeConfig.showOnHomePage').value) {
        this.mobileAppSettingsForm.get('qrCodeConfig.qrCodeLabel').enable({emitEvent: false});
      } else {
        this.mobileAppSettingsForm.get('qrCodeConfig.qrCodeLabel').disable({emitEvent: false});
      }
    });
  }

  private processMobileAppSettings(mobileAppSettings: QrCodeSettings): void {
    this.mobileAppSettings = {...mobileAppSettings};
    this.mobileAppSettingsForm.reset(this.mobileAppSettings);
  }

  save(): void {
    const showOnHomePagePreviousValue = this.mobileAppSettings.qrCodeConfig.showOnHomePage;
    this.mobileAppSettings = {...this.mobileAppSettings, ...this.mobileAppSettingsForm.getRawValue()};
    this.mobileAppService.saveMobileAppSettings(this.mobileAppSettings)
      .subscribe((settings) => {
        const showOnHomePageValue = settings.qrCodeConfig.showOnHomePage;
        if (showOnHomePagePreviousValue !== showOnHomePageValue) {
          this.store.dispatch(new ActionUpdateMobileQrCodeEnabled({mobileQrEnabled: showOnHomePageValue}));
        }
        this.processMobileAppSettings(settings);
      });
  }

  confirmForm(): FormGroup {
    return this.mobileAppSettingsForm;
  }
}
