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

import { Component, Inject, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import { TwoFactorAuthAccountConfig, TwoFactorAuthProviderType } from '@shared/models/two-factor-auth.models';
import { MatStepper } from '@angular/material/stepper';

export interface EmailAuthDialogData {
  email: string;
}

@Component({
  selector: 'tb-email-auth-dialog',
  templateUrl: './email-auth-dialog.component.html',
  styleUrls: ['./email-auth-dialog.component.scss']
})
export class EmailAuthDialogComponent extends DialogComponent<EmailAuthDialogComponent> {

  private authAccountConfig: TwoFactorAuthAccountConfig;

  emailConfigForm: FormGroup;
  emailVerificationForm: FormGroup;

  @ViewChild('stepper', {static: false}) stepper: MatStepper;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private twoFaService: TwoFactorAuthenticationService,
              @Inject(MAT_DIALOG_DATA) public data: EmailAuthDialogData,
              public dialogRef: MatDialogRef<EmailAuthDialogComponent>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);

    this.emailConfigForm = this.fb.group({
      email: [this.data.email, [Validators.required, Validators.email]]
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
        this.authAccountConfig = {
          providerType: TwoFactorAuthProviderType.EMAIL,
          useByDefault: true,
          email: this.emailConfigForm.get('email').value as string
        };
        this.twoFaService.submitTwoFaAccountConfig(this.authAccountConfig).subscribe(() => {
          this.stepper.next();
        });
        break;
      case 1:
        this.twoFaService.verifyAndSaveTwoFaAccountConfig(this.authAccountConfig,
                                                          this.emailVerificationForm.get('verificationCode').value).subscribe(() => {
          this.stepper.next();
        });
        break;
    }
  }

  closeDialog() {
    return this.dialogRef.close(this.stepper.selectedIndex > 1);
  }

}
