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

import { Injectable, NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SecurityComponent } from './security.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { Authority } from '@shared/models/authority.enum';
import { User } from '@shared/models/user.model';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UserService } from '@core/http/user.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Observable } from 'rxjs';
import { TwoFactorAuthProviderType } from '@shared/models/two-factor-auth.models';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';

@Injectable()
export class UserProfileResolver  {

  constructor(private store: Store<AppState>,
              private userService: UserService) {
  }

  resolve(): Observable<User> {
    const userId = getCurrentAuthUser(this.store).userId;
    return this.userService.getUser(userId);
  }
}

@Injectable()
export class UserTwoFAProvidersResolver  {

  constructor(private twoFactorAuthService: TwoFactorAuthenticationService) {
  }

  resolve(): Observable<Array<TwoFactorAuthProviderType>> {
    return this.twoFactorAuthService.getAvailableTwoFaProviders();
  }
}

export const securityRoutes: Routes = [
  {
    path: 'security',
    component: SecurityComponent,
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'security.security',
      breadcrumb: {
        label: 'security.security',
        icon: 'lock'
      }
    },
    resolve: {
      user: UserProfileResolver,
      providers: UserTwoFAProvidersResolver
    }
  }
];

const routes: Routes = [
  {
    path: 'security',
    redirectTo: '/account/security'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    UserProfileResolver,
    UserTwoFAProvidersResolver
  ]
})
export class SecurityRoutingModule { }
