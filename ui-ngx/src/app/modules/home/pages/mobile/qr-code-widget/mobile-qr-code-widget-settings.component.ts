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
import { MobileApplicationService } from '@core/http/mobile-application.service';
import {
  BadgePosition,
  badgePositionTranslationsMap,
  QrCodeSettings
} from '@shared/models/mobile-app.models';
import { ActionUpdateMobileQrCodeEnabled } from '@core/auth/auth.actions';
import { EntityType } from '@shared/models/entity-type.models';

@Component({
  selector: 'tb-mobile-qr-code-widget',
  templateUrl: './mobile-qr-code-widget-settings.component.html',
  styleUrls: ['mobile-qr-code-widget-settings.component.scss', '../../admin/settings-card.scss']
})
export class MobileQrCodeWidgetSettingsComponent extends PageComponent implements HasConfirmForm, OnDestroy {

  mobileAppSettingsForm: FormGroup;
  mobileAppSettings: QrCodeSettings;

  readonly badgePositionTranslationsMap = badgePositionTranslationsMap;
  readonly entityType = EntityType;

  private readonly destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              private mobileAppService: MobileApplicationService,
              private fb: FormBuilder) {
    super(store);
    this.buildMobileAppSettingsForm();
    this.mobileAppService.getMobileAppSettings()
      .subscribe(settings => this.processMobileAppSettings(settings));
    this.mobileAppSettingsForm.get('useDefaultApp').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value) {
        this.mobileAppSettingsForm.get('mobileAppBundleId').disable({emitEvent: false});
        // this.mobileAppSettingsForm.get('androidConfig.appPackage').disable({emitEvent: false});
        // this.mobileAppSettingsForm.get('androidConfig.sha256CertFingerprints').disable({emitEvent: false});
        // this.mobileAppSettingsForm.get('androidConfig.storeLink').disable({emitEvent: false});
        // this.mobileAppSettingsForm.get('iosConfig.appId').disable({emitEvent: false});
        // this.mobileAppSettingsForm.get('iosConfig.storeLink').disable({emitEvent: false});
      } else {
        this.mobileAppSettingsForm.get('mobileAppBundleId').enable({emitEvent: false});

        // if (this.mobileAppSettingsForm.get('androidConfig.enabled').value) {
        //   this.mobileAppSettingsForm.get('androidConfig.appPackage').enable({emitEvent: false});
        //   this.mobileAppSettingsForm.get('androidConfig.sha256CertFingerprints').enable({emitEvent: false});
        //   this.mobileAppSettingsForm.get('androidConfig.storeLink').enable({emitEvent: false});
        // }
        // if (this.mobileAppSettingsForm.get('iosConfig.enabled').value) {
        //   this.mobileAppSettingsForm.get('iosConfig.appId').enable({emitEvent: false});
        //   this.mobileAppSettingsForm.get('iosConfig.storeLink').enable({emitEvent: false});
        // }
      }
    });
    // this.mobileAppSettingsForm.get('androidConfig.enabled').valueChanges.pipe(
    //   takeUntil(this.destroy$)
    // ).subscribe(value => {
    //   this.androidEnableChanged(value);
    // });
    // this.mobileAppSettingsForm.get('iosConfig.enabled').valueChanges.pipe(
    //   takeUntil(this.destroy$)
    // ).subscribe(value => {
    //   this.iosEnableChanged(value);
    // });
    this.mobileAppSettingsForm.get('qrCodeConfig.showOnHomePage').valueChanges.pipe(
      takeUntil(this.destroy$)
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
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value) {
        // if (this.mobileAppSettingsForm.get('androidConfig.enabled').value || this.mobileAppSettingsForm.get('iosConfig.enabled').value) {
        //   this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').enable({emitEvent: false});
        //   this.mobileAppSettingsForm.get('qrCodeConfig.badgePosition').enable({emitEvent: false});
        // } else {
        //   this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').disable({emitEvent: false});
        //   this.mobileAppSettingsForm.get('qrCodeConfig.badgePosition').disable({emitEvent: false});
        // }
      } else {
        this.mobileAppSettingsForm.get('qrCodeConfig.badgePosition').disable({emitEvent: false});
      }
    });
    this.mobileAppSettingsForm.get('qrCodeConfig.qrCodeLabelEnabled').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value && this.mobileAppSettingsForm.get('qrCodeConfig.showOnHomePage').value) {
        this.mobileAppSettingsForm.get('qrCodeConfig.qrCodeLabel').enable({emitEvent: false});
      } else {
        this.mobileAppSettingsForm.get('qrCodeConfig.qrCodeLabel').disable({emitEvent: false});
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
      useDefaultApp: [true],
      mobileAppBundleId: [{value: null, disabled: true}, Validators.required],
      // androidConfig: this.fb.group({
      //   enabled: [true],
      //   appPackage: [{value: '', disabled: true}, [Validators.required]],
      //   sha256CertFingerprints: [{value: '', disabled: true}, [Validators.required]],
      //   storeLink: ['', [Validators.required]]
      // }),
      // iosConfig: this.fb.group({
      //   enabled: [true],
      //   appId: [{value: '', disabled: true}, [Validators.required]],
      //   storeLink: ['', [Validators.required]]
      // }),
      qrCodeConfig: this.fb.group({
        showOnHomePage: [true],
        badgeEnabled: [true],
        badgePosition: [BadgePosition.RIGHT],
        qrCodeLabelEnabled: [true],
        qrCodeLabel: ['', [Validators.required, Validators.maxLength(50)]]
      })
    });
  }

  private processMobileAppSettings(mobileAppSettings: QrCodeSettings): void {
    this.mobileAppSettings = {...mobileAppSettings};
    this.mobileAppSettingsForm.reset(this.mobileAppSettings);
  }

  // private androidEnableChanged(value: boolean): void {
  //   if (value) {
  //     if (!this.mobileAppSettingsForm.get('useDefaultApp').value) {
  //       this.mobileAppSettingsForm.get('androidConfig.appPackage').enable({emitEvent: false});
  //       this.mobileAppSettingsForm.get('androidConfig.sha256CertFingerprints').enable({emitEvent: false});
  //       this.mobileAppSettingsForm.get('androidConfig.storeLink').enable({emitEvent: false});
  //     }
  //   } else {
  //     this.mobileAppSettingsForm.get('androidConfig.appPackage').disable({emitEvent: false});
  //     this.mobileAppSettingsForm.get('androidConfig.sha256CertFingerprints').disable({emitEvent: false});
  //     this.mobileAppSettingsForm.get('androidConfig.storeLink').disable({emitEvent: false});
  //   }
  //   this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').updateValueAndValidity({onlySelf: true});
  // }
  //
  // private iosEnableChanged(value: boolean): void {
  //   if (value) {
  //     if (!this.mobileAppSettingsForm.get('useDefaultApp').value) {
  //       this.mobileAppSettingsForm.get('iosConfig.appId').enable({emitEvent: false});
  //       this.mobileAppSettingsForm.get('iosConfig.storeLink').enable({emitEvent: false});
  //     }
  //   } else {
  //     this.mobileAppSettingsForm.get('iosConfig.appId').disable({emitEvent: false});
  //     this.mobileAppSettingsForm.get('iosConfig.storeLink').disable({emitEvent: false});
  //   }
  //   this.mobileAppSettingsForm.get('qrCodeConfig.badgeEnabled').updateValueAndValidity({onlySelf: true});
  // }

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
