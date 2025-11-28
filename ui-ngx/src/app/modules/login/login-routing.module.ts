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

import { inject, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, ResolveFn, Router, RouterModule, RouterStateSnapshot, Routes } from '@angular/router';

import { LoginComponent } from './pages/login/login.component';
import { AuthGuard } from '@core/guards/auth.guard';
import { ResetPasswordRequestComponent } from '@modules/login/pages/login/reset-password-request.component';
import { ResetPasswordComponent } from '@modules/login/pages/login/reset-password.component';
import { CreatePasswordComponent } from '@modules/login/pages/login/create-password.component';
import { TwoFactorAuthLoginComponent } from '@modules/login/pages/login/two-factor-auth-login.component';
import { Authority } from '@shared/models/authority.enum';
import { LinkExpiredComponent } from '@modules/login/pages/login/link-expired.component';
import { ForceTwoFactorAuthLoginComponent } from '@modules/login/pages/login/force-two-factor-auth-login.component';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '@core/auth/auth.service';
import { UserPasswordPolicy } from '@shared/models/settings.models';

const passwordPolicyResolver: ResolveFn<UserPasswordPolicy> = (route: ActivatedRouteSnapshot,
   state: RouterStateSnapshot,
   router = inject(Router),
   authService = inject(AuthService)) => {
    return authService.getUserPasswordPolicy({ignoreErrors: true}).pipe(
      catchError(() => {
        return of({} as UserPasswordPolicy);
      })
    );
};

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
    canActivate: [AuthGuard],
    resolve: {
      passwordPolicy: passwordPolicyResolver
    }
  },
  {
    path: 'login/resetExpiredPassword',
    component: ResetPasswordComponent,
    data: {
      title: 'login.reset-password',
      module: 'public',
      expiredPassword: true
    },
    canActivate: [AuthGuard],
    resolve: {
      passwordPolicy: passwordPolicyResolver
    }
  },
  {
    path: 'login/createPassword',
    component: CreatePasswordComponent,
    data: {
      title: 'login.create-password',
      module: 'public'
    },
    canActivate: [AuthGuard],
    resolve: {
      passwordPolicy: passwordPolicyResolver
    }
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
    path: 'login/force-mfa',
    component: ForceTwoFactorAuthLoginComponent,
    data: {
      title: 'login.two-factor-authentication',
      auth: [Authority.MFA_CONFIGURATION_TOKEN],
      module: 'public'
    },
    canActivate: [AuthGuard]
  },
  {
    path: 'activationLinkExpired',
    component: LinkExpiredComponent,
    data: {
      title: 'login.activation-link-expired',
      module: 'public'
    },
    canActivate: [AuthGuard]
  },
  {
    path: 'passwordResetLinkExpired',
    component: LinkExpiredComponent,
    data: {
      title: 'login.reset-password-link-expired',
      module: 'public',
      passwordLinkExpired: true
    },
    canActivate: [AuthGuard]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class LoginRoutingModule { }
