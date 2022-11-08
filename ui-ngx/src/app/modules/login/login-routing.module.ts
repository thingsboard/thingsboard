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

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { LoginComponent } from './pages/login/login.component';
import { AuthGuard } from '@core/guards/auth.guard';
import { ResetPasswordRequestComponent } from '@modules/login/pages/login/reset-password-request.component';
import { ResetPasswordComponent } from '@modules/login/pages/login/reset-password.component';
import { CreatePasswordComponent } from '@modules/login/pages/login/create-password.component';
import { TwoFactorAuthLoginComponent } from '@modules/login/pages/login/two-factor-auth-login.component';
import { Authority } from '@shared/models/authority.enum';
import { TwoFactorAuthForceComponent } from '@modules/login/pages/mfa/two-factor-auth-force.component';
import { UserTwoFAProvidersResolver } from '@shared/shared.module';

const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent,
    data: {
      title: 'login.login',
      module: 'public'
    },
    canActivate: [AuthGuard]
  },
  {
    path: 'login/resetPasswordRequest',
    component: ResetPasswordRequestComponent,
    data: {
      title: 'login.request-password-reset',
      module: 'public'
    },
    canActivate: [AuthGuard]
  },
  {
    path: 'login/resetPassword',
    component: ResetPasswordComponent,
    data: {
      title: 'login.reset-password',
      module: 'public'
    },
    canActivate: [AuthGuard]
  },
  {
    path: 'login/resetExpiredPassword',
    component: ResetPasswordComponent,
    data: {
      title: 'login.reset-password',
      module: 'public',
      expiredPassword: true
    },
    canActivate: [AuthGuard]
  },
  {
    path: 'login/createPassword',
    component: CreatePasswordComponent,
    data: {
      title: 'login.create-password',
      module: 'public'
    },
    canActivate: [AuthGuard]
  },
  {
    path: 'login/mfa',
    component: TwoFactorAuthLoginComponent,
    data: {
      title: 'login.two-factor-authentication',
      auth: [Authority.PRE_VERIFICATION_TOKEN],
      module: 'public'
    },
    canActivate: [AuthGuard]
  },
  {
    path: 'login/mfa-force',
    component: TwoFactorAuthForceComponent,
    data: {
      title: 'login.two-factor-authentication',
      auth: [Authority.TWO_FACTOR_FORCE_SAVE_SETTINGS_TOKEN],
      module: 'public'
    },
    canActivate: [AuthGuard],
    resolve: {
      providers: UserTwoFAProvidersResolver
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class LoginRoutingModule { }
