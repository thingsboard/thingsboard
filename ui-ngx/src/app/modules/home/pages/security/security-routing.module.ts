// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
