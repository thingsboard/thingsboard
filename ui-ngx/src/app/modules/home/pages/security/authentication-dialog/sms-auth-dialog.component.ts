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

import { Component, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import {
  AccountTwoFaSettings,
  TwoFactorAuthAccountConfig,
  TwoFactorAuthProviderType
} from '@shared/models/two-factor-auth.models';
import { phoneNumberPattern } from '@shared/models/settings.models';
import { MatStepper } from '@angular/material/stepper';

@Component({
  selector: 'tb-sms-auth-dialog',
  templateUrl: './sms-auth-dialog.component.html',
  styleUrls: ['./authentication-dialog.component.scss']
})
export class SMSAuthDialogComponent extends DialogComponent<SMSAuthDialogComponent> {

  private authAccountConfig: TwoFactorAuthAccountConfig;
  private config: AccountTwoFaSettings;

  phoneNumberPattern = phoneNumberPattern;

  smsConfigForm: UntypedFormGroup;
  smsVerificationForm: UntypedFormGroup;

  @ViewChild('stepper', {static: false}) stepper: MatStepper;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private twoFaService: TwoFactorAuthenticationService,
              public dialogRef: MatDialogRef<SMSAuthDialogComponent>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);

    this.smsConfigForm = this.fb.group({
      phone: ['', [Validators.required, Validators.pattern(phoneNumberPattern)]]
    });

    this.smsVerificationForm = this.fb.group({
      verificationCode: ['', [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(6),
        Validators.pattern(/^\d*$/)
      ]]
    });
  }

  nextStep() {
    switch (this.stepper.selectedIndex) {
      case 0:
        if (this.smsConfigForm.valid) {
          this.authAccountConfig = {
            providerType: TwoFactorAuthProviderType.SMS,
            useByDefault: true,
            phoneNumber: this.smsConfigForm.get('phone').value as string
          };
          this.twoFaService.submitTwoFaAccountConfig(this.authAccountConfig).subscribe(() => {
            this.stepper.next();
          });
        } else {
          this.showFormErrors(this.smsConfigForm);
        }
        break;
      case 1:
        if (this.smsVerificationForm.valid) {
          this.twoFaService.verifyAndSaveTwoFaAccountConfig(this.authAccountConfig,
            this.smsVerificationForm.get('verificationCode').value).subscribe((config) => {
              this.config = config;
              this.stepper.next();
            });
        } else {
          this.showFormErrors(this.smsVerificationForm);
        }
        break;
    }
  }

  closeDialog() {
    return this.dialogRef.close(this.config);
  }

  private showFormErrors(form: UntypedFormGroup) {
    Object.keys(form.controls).forEach(field => {
      const control = form.get(field);
      control.markAsTouched({onlySelf: true});
    });
  }
}
