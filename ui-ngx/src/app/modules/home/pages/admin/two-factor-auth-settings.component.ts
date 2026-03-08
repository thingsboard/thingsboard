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

import { Component, OnDestroy, OnInit, QueryList, ViewChildren } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormArray, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import {
  TwoFactorAuthProviderConfigForm,
  twoFactorAuthProvidersData,
  TwoFactorAuthProviderType,
  TwoFactorAuthSettings,
  TwoFactorAuthSettingsForm
} from '@shared/models/two-factor-auth.models';
import { isNotEmptyStr } from '@core/utils';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MatExpansionPanel } from '@angular/material/expansion';

@Component({
    selector: 'tb-2fa-settings',
    templateUrl: './two-factor-auth-settings.component.html',
    styleUrls: ['./settings-card.scss', './two-factor-auth-settings.component.scss'],
    standalone: false
})
export class TwoFactorAuthSettingsComponent extends PageComponent implements OnInit, HasConfirmForm, OnDestroy {

  private readonly destroy$ = new Subject<void>();
  private readonly posIntValidation = [Validators.required, Validators.min(1), Validators.pattern(/^\d*$/)];

  twoFaFormGroup: UntypedFormGroup;
  twoFactorAuthProviderType = TwoFactorAuthProviderType;
  twoFactorAuthProvidersData = twoFactorAuthProvidersData;

  showMainLoadingBar = false;

  @ViewChildren(MatExpansionPanel) expansionPanel: QueryList<MatExpansionPanel>;

  constructor(protected store: Store<AppState>,
              private twoFaService: TwoFactorAuthenticationService,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.build2faSettingsForm();
    this.twoFaService.getTwoFaSettings().subscribe((setting) => {
      this.setAuthConfigFormValue(setting);
    });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  confirmForm(): UntypedFormGroup {
    return this.twoFaFormGroup;
  }

  save() {
    if (this.twoFaFormGroup.valid) {
      const setting = this.twoFaFormGroup.getRawValue() as TwoFactorAuthSettingsForm;
      this.joinRateLimit(setting, 'verificationCodeCheckRateLimit');
      const providers = setting.providers.filter(provider => provider.enable);
      providers.forEach(provider => delete provider.enable);
      const config = Object.assign(setting, {providers});
      this.twoFaService.saveTwoFaSettings(config).subscribe(
        (settings) => {
          this.setAuthConfigFormValue(settings);
          this.twoFaFormGroup.markAsUntouched();
          this.twoFaFormGroup.markAsPristine();
        }
      );
    } else {
      Object.keys(this.twoFaFormGroup.controls).forEach(field => {
        const control = this.twoFaFormGroup.get(field);
        control.markAsTouched({onlySelf: true});
      });
    }
  }

  toggleExtensionPanel($event: Event, index: number, currentState: boolean) {
    if ($event) {
      $event.stopPropagation();
    }
    if (currentState) {
      this.getByIndexPanel(index).open();
    } else {
      this.getByIndexPanel(index).close();
    }
  }

  trackByElement(i: number, item: any) {
    return item;
  }

  get providersForm(): UntypedFormArray {
    return this.twoFaFormGroup.get('providers') as UntypedFormArray;
  }

  private build2faSettingsForm(): void {
    this.twoFaFormGroup = this.fb.group({
      maxVerificationFailuresBeforeUserLockout: [30, [
        Validators.pattern(/^\d*$/),
        Validators.min(0),
        Validators.max(65535)
      ]],
      totalAllowedTimeForVerification: [3600, [
        Validators.required,
        Validators.min(60),
        Validators.pattern(/^\d*$/)
      ]],
      verificationCodeCheckRateLimitEnable: [false],
      verificationCodeCheckRateLimitNumber: [{value: 3, disabled: true}, this.posIntValidation],
      verificationCodeCheckRateLimitTime: [{value: 900, disabled: true}, this.posIntValidation],
      minVerificationCodeSendPeriod: ['30', [Validators.required, Validators.min(5), Validators.pattern(/^\d*$/)]],
      providers: this.fb.array([])
    });
    Object.values(TwoFactorAuthProviderType).forEach(provider => {
      this.buildProvidersSettingsForm(provider);
    });
    this.twoFaFormGroup.get('verificationCodeCheckRateLimitEnable').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value) {
        this.twoFaFormGroup.get('verificationCodeCheckRateLimitNumber').enable({emitEvent: false});
        this.twoFaFormGroup.get('verificationCodeCheckRateLimitTime').enable({emitEvent: false});
      } else {
        this.twoFaFormGroup.get('verificationCodeCheckRateLimitNumber').disable({emitEvent: false});
        this.twoFaFormGroup.get('verificationCodeCheckRateLimitTime').disable({emitEvent: false});
      }
    });
    this.providersForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value: TwoFactorAuthProviderConfigForm[]) => {
      const activeProvider = value.filter(provider => provider.enable);
      const indexBackupCode = Object.values(TwoFactorAuthProviderType).indexOf(TwoFactorAuthProviderType.BACKUP_CODE);
      if (!activeProvider.length ||
        activeProvider.length === 1 && activeProvider[0].providerType === TwoFactorAuthProviderType.BACKUP_CODE) {
        this.providersForm.at(indexBackupCode).get('enable').setValue(false, {emitEvent: false});
        this.providersForm.at(indexBackupCode).disable( {emitEvent: false});
        this.providersForm.at(indexBackupCode).get('providerType').enable({emitEvent: false});
      } else {
        this.providersForm.at(indexBackupCode).get('enable').enable( {emitEvent: false});
      }
    });
  }

  private setAuthConfigFormValue(settings: TwoFactorAuthSettings) {
    const [checkRateLimitNumber, checkRateLimitTime] = this.splitRateLimit(settings?.verificationCodeCheckRateLimit);
    const allowProvidersConfig = settings?.providers.map(provider => provider.providerType) || [];
    const processFormValue: TwoFactorAuthSettingsForm = Object.assign({}, settings, {
      verificationCodeCheckRateLimitEnable: checkRateLimitNumber > 0,
      verificationCodeCheckRateLimitNumber: checkRateLimitNumber || 3,
      verificationCodeCheckRateLimitTime: checkRateLimitTime || 900,
      providers: []
    });
    if (checkRateLimitNumber > 0) {
      this.getByIndexPanel(this.providersForm.length).open();
    }
    Object.values(TwoFactorAuthProviderType).forEach((provider, index) => {
      const findIndex = allowProvidersConfig.indexOf(provider);
      if (findIndex > -1) {
        processFormValue.providers.push(Object.assign(settings.providers[findIndex], {enable: true}));
        this.getByIndexPanel(index).open();
      } else {
        processFormValue.providers.push({enable: false});
      }
    });
    this.twoFaFormGroup.patchValue(processFormValue);
  }

  private buildProvidersSettingsForm(provider: TwoFactorAuthProviderType) {
    const formControlConfig: {[key: string]: any} = {
      providerType: [provider],
      enable: [false]
    };
    switch (provider) {
      case TwoFactorAuthProviderType.TOTP:
        formControlConfig.issuerName = [{value: 'ThingsBoard', disabled: true}, [Validators.required, Validators.pattern(/^(?!^\s+$).*$/)]];
        break;
      case TwoFactorAuthProviderType.SMS:
        formControlConfig.smsVerificationMessageTemplate = [{value: 'Verification code: ${code}', disabled: true}, [
          Validators.required,
          Validators.pattern(/\${code}/)
        ]];
        formControlConfig.verificationCodeLifetime = [{value: 120, disabled: true}, this.posIntValidation];
        break;
      case TwoFactorAuthProviderType.EMAIL:
        formControlConfig.verificationCodeLifetime = [{value: 120, disabled: true}, this.posIntValidation];
        break;
      case TwoFactorAuthProviderType.BACKUP_CODE:
        formControlConfig.codesQuantity = [{value: 10, disabled: true}, this.posIntValidation];
        break;
    }
    const newProviders = this.fb.group(formControlConfig);
    newProviders.get('enable').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value) {
        newProviders.enable({emitEvent: false});
      } else {
        newProviders.disable({emitEvent: false});
        newProviders.get('enable').enable({emitEvent: false});
        newProviders.get('providerType').enable({emitEvent: false});
      }
    });
    this.providersForm.push(newProviders, {emitEvent: false});
  }

  private getByIndexPanel(index: number) {
    return this.expansionPanel.find((_, i) => i === index);
  }

  private splitRateLimit(setting: string): [number, number] {
    if (isNotEmptyStr(setting)) {
      const [attemptNumber, time] = setting.split(':');
      return [parseInt(attemptNumber, 10), parseInt(time, 10)];
    }
    return [0, 0];
  }

  private joinRateLimit(processFormValue: TwoFactorAuthSettingsForm, property: string) {
    if (processFormValue[`${property}Enable`]) {
      processFormValue[property] = [processFormValue[`${property}Number`], processFormValue[`${property}Time`]].join(':');
    }
    delete processFormValue[`${property}Enable`];
    delete processFormValue[`${property}Number`];
    delete processFormValue[`${property}Time`];
  }
}
