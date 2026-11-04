// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { SignupRoutingModule } from '@modules/signup/signup-routing.module';
import { SignupComponent } from '@modules/signup/pages/signup/signup.component';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { NgxCaptchaModule } from 'ngx-captcha';
import { EmailVerificationComponent } from '@modules/signup/pages/signup/email-verification.component';
import { EmailVerifiedComponent } from '@modules/signup/pages/signup/email-verified.component';
import { SignupDialogComponent } from '@modules/signup/pages/signup/signup-dialog.component';
import { TbRecaptchaComponent } from '@modules/signup/pages/signup/tb-recaptcha.component';

@NgModule({
  declarations: [
    SignupComponent,
    SignupDialogComponent,
    EmailVerificationComponent,
    EmailVerifiedComponent,
    TbRecaptchaComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    NgxCaptchaModule,
    SignupRoutingModule
  ]
})
export class SignupModule { }
