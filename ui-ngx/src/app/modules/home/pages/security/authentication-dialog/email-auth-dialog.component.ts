///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { Component, Inject, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import {
  AccountTwoFaSettings,
  TwoFactorAuthAccountConfig,
  TwoFactorAuthProviderType
} from '@shared/models/two-factor-auth.models';
import { MatStepper } from '@angular/material/stepper';
import { validateEmail } from '@app/core/utils';

export interface EmailAuthDialogData {
  email: string;
}

@Component({
  selector: 'tb-email-auth-dialog',
  templateUrl: './email-auth-dialog.component.html',
  styleUrls: ['./authentication-dialog.component.scss']
})
export class EmailAuthDialogComponent extends DialogComponent<EmailAuthDialogComponent> {

  private authAccountConfig: TwoFactorAuthAccountConfig;
  private config: AccountTwoFaSettings;

  emailConfigForm: UntypedFormGroup;
  emailVerificationForm: UntypedFormGroup;

  @ViewChild('stepper', {static: false}) stepper: MatStepper;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private twoFaService: TwoFactorAuthenticationService,
              @Inject(MAT_DIALOG_DATA) public data: EmailAuthDialogData,
              public dialogRef: MatDialogRef<EmailAuthDialogComponent>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);

    this.emailConfigForm = this.fb.group({
          email: [this.data.email, [Validators.required, validateEmail]]
    });

    this.emailVerificationForm = this.fb.group({
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
        if (this.emailConfigForm.valid) {
          this.authAccountConfig = {
            providerType: TwoFactorAuthProviderType.EMAIL,
            useByDefault: true,
            email: this.emailConfigForm.get('email').value as string
          };
          this.twoFaService.submitTwoFaAccountConfig(this.authAccountConfig).subscribe(() => {
            this.stepper.next();
          });
        } else {
          this.showFormErrors(this.emailConfigForm);
        }
        break;
      case 1:
        if (this.emailVerificationForm.valid) {
          this.twoFaService.verifyAndSaveTwoFaAccountConfig(this.authAccountConfig,
            this.emailVerificationForm.get('verificationCode').value).subscribe((config) => {
              this.config = config;
              this.stepper.next();
            });
        } else {
          this.showFormErrors(this.emailVerificationForm);
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
