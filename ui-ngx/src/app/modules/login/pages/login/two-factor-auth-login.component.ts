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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { AuthService } from '@core/auth/auth.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { UntypedFormBuilder, Validators } from '@angular/forms';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import {
  twoFactorAuthProvidersLoginData,
  TwoFactorAuthProviderType,
  TwoFaProviderInfo
} from '@shared/models/two-factor-auth.models';
import { TranslateService } from '@ngx-translate/core';
import { interval, Subscription } from 'rxjs';
import { isEqual } from '@core/utils';
import { ActionNotificationShow } from '@core/notification/notification.actions';

@Component({
  selector: 'tb-two-factor-auth-login',
  templateUrl: './two-factor-auth-login.component.html',
  styleUrls: ['./two-factor-auth-login.component.scss']
})
export class TwoFactorAuthLoginComponent extends PageComponent implements OnInit, OnDestroy {

  private providersInfo: TwoFaProviderInfo[];
  private prevProvider: TwoFactorAuthProviderType;
  private timer: Subscription;
  private minVerificationPeriod = 0;
  private timerID: NodeJS.Timeout;

  showResendAction = false;
  selectedProvider: TwoFactorAuthProviderType;
  allowProviders: TwoFactorAuthProviderType[] = [];

  providersData = twoFactorAuthProvidersLoginData;
  providerDescription = '';
  hideResendButton = true;
  countDownTime = 0;

  maxLengthInput = 6;
  inputMode = 'numeric';
  pattern = '[0-9]*';

  verificationForm = this.fb.group({
    verificationCode: ['', [
      Validators.required,
      Validators.minLength(6),
      Validators.maxLength(6),
      Validators.pattern(/^\d*$/)
    ]]
  });

  constructor(protected store: Store<AppState>,
              private twoFactorAuthService: TwoFactorAuthenticationService,
              private authService: AuthService,
              private translate: TranslateService,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.providersInfo = this.authService.twoFactorAuthProviders;
    Object.values(TwoFactorAuthProviderType).forEach(provider => {
      const providerConfig = this.providersInfo.find(config => config.type === provider);
      if (providerConfig) {
        if (providerConfig.default) {
          this.selectedProvider = providerConfig.type;
          this.providerDescription = this.translate.instant(this.providersData.get(providerConfig.type).description, {
            contact: providerConfig.contact
          });
          this.minVerificationPeriod = providerConfig?.minVerificationCodeSendPeriod || 30;
        }
        this.allowProviders.push(providerConfig.type);
      }
    });
    if (this.selectedProvider !== TwoFactorAuthProviderType.TOTP) {
      this.sendCode();
      this.showResendAction = true;
    }
    this.timer = interval(1000).subscribe(() => this.updatedTime());
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.timer.unsubscribe();
    clearTimeout(this.timerID);
  }

  sendVerificationCode() {
    if (this.verificationForm.valid && this.selectedProvider) {
      this.authService.checkTwoFaVerificationCode(this.selectedProvider, this.verificationForm.get('verificationCode').value).subscribe(
        () => {},
        (error) => {
          if (error.status === 400) {
            this.verificationForm.get('verificationCode').setErrors({incorrectCode: true});
          } else if (error.status === 429) {
            this.verificationForm.get('verificationCode').setErrors({tooManyRequest: true});
            this.timerID = setTimeout(() => {
              let errors = this.verificationForm.get('verificationCode').errors;
              delete errors.tooManyRequest;
              if (isEqual(errors, {})) {
                errors = null;
              }
              this.verificationForm.get('verificationCode').setErrors(errors);
            }, 5000);
          } else {
            this.store.dispatch(new ActionNotificationShow({
              message: error.error.message,
              type: 'error',
              verticalPosition: 'top',
              horizontalPosition: 'left'
            }));
          }
        }
      );
    }
  }

  selectProvider(type: TwoFactorAuthProviderType) {
    this.prevProvider = type === null ? this.selectedProvider : null;
    this.selectedProvider = type;
    this.showResendAction = false;
    if (type !== null) {
      this.verificationForm.get('verificationCode').reset();
      const providerConfig = this.providersInfo.find(config => config.type === type);
      this.providerDescription = this.translate.instant(this.providersData.get(providerConfig.type).description, {
        contact: providerConfig.contact
      });
      if (type !== TwoFactorAuthProviderType.TOTP && type !== TwoFactorAuthProviderType.BACKUP_CODE) {
        this.sendCode();
        this.showResendAction = true;
        this.minVerificationPeriod = providerConfig?.minVerificationCodeSendPeriod || 30;
      }
      if (type === TwoFactorAuthProviderType.BACKUP_CODE) {
        this.verificationForm.get('verificationCode').setValidators([
          Validators.required,
          Validators.minLength(8),
          Validators.maxLength(8),
          Validators.pattern(/^[\dabcdef]*$/)
        ]);
        this.maxLengthInput = 8;
        this.inputMode = 'text';
        this.pattern = '[0-9abcdef]*';
      } else {
        this.verificationForm.get('verificationCode').setValidators([
          Validators.required,
          Validators.minLength(6),
          Validators.maxLength(6),
          Validators.pattern(/^\d*$/)
        ]);
        this.maxLengthInput = 6;
        this.inputMode = 'numeric';
        this.pattern = '[0-9]*';
      }
      this.verificationForm.get('verificationCode').updateValueAndValidity({emitEvent: false});
    }
  }

  sendCode($event?: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.hideResendButton = true;
    this.countDownTime = 0;
    this.twoFactorAuthService.requestTwoFaVerificationCodeSend(this.selectedProvider).subscribe(() => {
      this.countDownTime = this.minVerificationPeriod;
    }, () => {
      this.countDownTime = this.minVerificationPeriod;
    });
  }

  cancelLogin() {
    if (this.prevProvider) {
      this.selectProvider(this.prevProvider);
    } else {
      this.authService.logout();
    }
  }

  private updatedTime() {
    if (this.countDownTime > 0) {
      this.countDownTime--;
      if (this.countDownTime === 0) {
        this.hideResendButton = false;
      }
    }
  }
}
