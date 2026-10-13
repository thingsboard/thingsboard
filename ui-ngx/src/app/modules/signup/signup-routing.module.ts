// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { AuthGuard } from '@core/guards/auth.guard';
import { SignupComponent } from '@modules/signup/pages/signup/signup.component';
import { EmailVerificationComponent } from '@modules/signup/pages/signup/email-verification.component';
import { EmailVerifiedComponent } from '@modules/signup/pages/signup/email-verified.component';
import { TbRecaptchaComponent } from '@modules/signup/pages/signup/tb-recaptcha.component';
import { passwordPolicyResolver } from '@modules/login/login-routing.module';

const routes: Routes = [
  {
    path: 'signup',
    component: SignupComponent,
    data: {
      title: 'signup.signup',
      module: 'public'
    },
    canActivate: [AuthGuard],
    resolve: {
      passwordPolicy: passwordPolicyResolver
    }
  },
  {
    path: 'signup/emailVerification',
    component: EmailVerificationComponent,
    data: {
      title: 'signup.email-verification',
      module: 'public'
    },
    canActivate: [AuthGuard]
  },
  {
    path: 'signup/emailVerified',
    component: EmailVerifiedComponent,
    data: {
      title: 'signup.account-activation-title',
      module: 'public'
    },
    canActivate: [AuthGuard]
  },
  {
    path: 'signup/recaptcha',
    component: TbRecaptchaComponent,
    data: {
      title: 'signup.recaptcha-title',
      module: 'public'
    },
    canActivate: [AuthGuard]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class SignupRoutingModule { }
