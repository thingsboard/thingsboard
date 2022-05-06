///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActivatedRoute } from '@angular/router';
import { AbstractControl, FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { WINDOW } from '@core/services/window.service';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import { AuthState } from '@core/auth/auth.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { TwoFactorAuthProviderType, TwoFactorAuthSettings } from '@shared/models/two-factor-auth.models';

@Component({
  selector: 'tb-2fa-settings',
  templateUrl: './two-factor-auth-settings.component.html',
  styleUrls: ['./two-factor-auth-settings.component.scss', './settings-card.scss']
})
export class TwoFactorAuthSettingsComponent extends PageComponent implements OnInit, HasConfirmForm, OnDestroy {

  private authState: AuthState = getCurrentAuthState(this.store);
  private authUser = this.authState.authUser;

  twoFaFormGroup: FormGroup;
  twoFactorAuthProviderTypes = Object.keys(TwoFactorAuthProviderType);
  twoFactorAuthProviderType = TwoFactorAuthProviderType;

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private twoFaService: TwoFactorAuthenticationService,
              private fb: FormBuilder,
              private dialogService: DialogService,
              private translate: TranslateService,
              @Inject(WINDOW) private window: Window) {
    super(store);
  }

  ngOnInit() {
    this.build2faSettingsForm();
    this.twoFaService.getTwoFaSettings().subscribe((setting) => {
      this.initTwoFactorAuthForm(setting);
    });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
  }

  confirmForm(): FormGroup {
    return this.twoFaFormGroup;
  }

  isTenantAdmin(): boolean {
    return this.authUser.authority === Authority.TENANT_ADMIN;
  }

  save() {
    const setting = this.twoFaFormGroup.value;
    this.twoFaService.saveTwoFaSettings(setting).subscribe(
      (twoFactorAuthSettings) => {
        this.twoFaFormGroup.patchValue(twoFactorAuthSettings, {emitEvent: false});
        this.twoFaFormGroup.markAsUntouched();
        this.twoFaFormGroup.markAsPristine();
      }
    );
  }

  private build2faSettingsForm(): void {
    this.twoFaFormGroup = this.fb.group({
      useSystemTwoFactorAuthSettings: [this.isTenantAdmin()],
      maxVerificationFailuresBeforeUserLockout: [30, [
        Validators.required,
        Validators.pattern(/^\d*$/),
        Validators.min(0),
        Validators.max(65535)
      ]],
      totalAllowedTimeForVerification: [3600, [
        Validators.required,
        Validators.min(1),
        Validators.pattern(/^\d*$/)
      ]],
      verificationCodeCheckRateLimit: ['3:900', [Validators.required, Validators.pattern(/^[1-9]\d*:[1-9]\d*$/)]],
      verificationCodeSendRateLimit: ['1:60', [Validators.required, Validators.pattern(/^[1-9]\d*:[1-9]\d*$/)]],
      providers: this.fb.array([])
    });
  }

  private initTwoFactorAuthForm(settings: TwoFactorAuthSettings) {
    settings.providers.forEach(() => {
      this.addProvider();
    });
    this.twoFaFormGroup.patchValue(settings);
    this.twoFaFormGroup.markAsPristine();
  }

  addProvider() {
    const newProviders = this.fb.group({
      providerType: [TwoFactorAuthProviderType.TOTP],
      issuerName: ['ThingsBoard', Validators.required],
      smsVerificationMessageTemplate: [{
        value: 'Verification code: ${verificationCode}',
        disabled: true
      }, [
        Validators.required,
        Validators.pattern(/\${verificationCode}/)
      ]],
      verificationCodeLifetime: [{
        value: 120,
        disabled: true
      }, [
        Validators.required,
        Validators.min(1),
        Validators.pattern(/^\d*$/)
      ]]
    });
    newProviders.get('providerType').valueChanges.subscribe(type => {
      switch (type) {
        case TwoFactorAuthProviderType.SMS:
          newProviders.get('issuerName').disable({emitEvent: false});
          newProviders.get('smsVerificationMessageTemplate').enable({emitEvent: false});
          newProviders.get('verificationCodeLifetime').enable({emitEvent: false});
          break;
        case TwoFactorAuthProviderType.TOTP:
          newProviders.get('issuerName').enable({emitEvent: false});
          newProviders.get('smsVerificationMessageTemplate').disable({emitEvent: false});
          newProviders.get('verificationCodeLifetime').disable({emitEvent: false});
          break;
        case TwoFactorAuthProviderType.EMAIL:
          newProviders.get('issuerName').disable({emitEvent: false});
          newProviders.get('smsVerificationMessageTemplate').disable({emitEvent: false});
          newProviders.get('verificationCodeLifetime').enable({emitEvent: false});
          break;
      }
    });
    if (this.providersForm.length) {
      const selectedProviderTypes = this.providersForm.value.map(providers => providers.providerType);
      const allowProviders = this.twoFactorAuthProviderTypes.filter(provider => !selectedProviderTypes.includes(provider));
      newProviders.get('providerType').setValue(allowProviders[0]);
      newProviders.updateValueAndValidity();
    }
    this.providersForm.push(newProviders);
    this.providersForm.markAsDirty();
  }

  removeProviders($event: Event, index: number): void {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }
    this.providersForm.removeAt(index);
    this.providersForm.markAsTouched();
    this.providersForm.markAsDirty();
  }

  get providersForm(): FormArray {
    return this.twoFaFormGroup.get('providers') as FormArray;
  }

  trackByElement(i: number, item: any) {
    return item;
  }

  selectedTypes(type: TwoFactorAuthProviderType, index: number): boolean {
    const selectedProviderTypes: TwoFactorAuthProviderType[] = this.providersForm.value.map(providers => providers.providerType);
    selectedProviderTypes.splice(index, 1);
    return selectedProviderTypes.includes(type);
  }

}
