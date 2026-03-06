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

import { Component, ElementRef, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import {
  AccountTwoFaSettings,
  TotpTwoFactorAuthAccountConfig,
  TwoFactorAuthProviderType
} from '@shared/models/two-factor-auth.models';
import { MatStepper } from '@angular/material/stepper';
import { unwrapModule } from '@core/utils';

@Component({
    selector: 'tb-totp-auth-dialog',
    templateUrl: './totp-auth-dialog.component.html',
    styleUrls: ['./authentication-dialog.component.scss'],
    standalone: false
})
export class TotpAuthDialogComponent extends DialogComponent<TotpAuthDialogComponent> {

  private authAccountConfig: TotpTwoFactorAuthAccountConfig;
  private config: AccountTwoFaSettings;

  totpConfigForm: UntypedFormGroup;
  totpAuthURL: string;

  @ViewChild('stepper', {static: false}) stepper: MatStepper;
  @ViewChild('canvas', {static: false}) canvasRef: ElementRef<HTMLCanvasElement>;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private twoFaService: TwoFactorAuthenticationService,
              public dialogRef: MatDialogRef<TotpAuthDialogComponent>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
    this.twoFaService.generateTwoFaAccountConfig(TwoFactorAuthProviderType.TOTP).subscribe(accountConfig => {
      this.authAccountConfig = accountConfig as TotpTwoFactorAuthAccountConfig;
      this.totpAuthURL = this.authAccountConfig.authUrl;
      this.authAccountConfig.useByDefault = true;
      import('qrcode').then((QRCode) => {
        unwrapModule(QRCode).toCanvas(this.canvasRef.nativeElement, this.totpAuthURL);
        this.canvasRef.nativeElement.style.width = 'auto';
        this.canvasRef.nativeElement.style.height = 'auto';
      });
    });
    this.totpConfigForm = this.fb.group({
      verificationCode: ['', [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(6),
        Validators.pattern(/^\d*$/)
      ]]
    });
  }

  onSaveConfig() {
    if (this.totpConfigForm.valid) {
      this.twoFaService.verifyAndSaveTwoFaAccountConfig(this.authAccountConfig,
        this.totpConfigForm.get('verificationCode').value).subscribe((config) => {
          this.config = config;
          this.stepper.next();
        });
    } else {
      Object.keys(this.totpConfigForm.controls).forEach(field => {
        const control = this.totpConfigForm.get(field);
        control.markAsTouched({onlySelf: true});
      });
    }
  }

  closeDialog() {
    return this.dialogRef.close(this.config);
  }

}
