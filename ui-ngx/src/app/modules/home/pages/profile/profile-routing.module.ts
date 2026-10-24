// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { inject, Injectable, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, ResolveFn, RouterModule, RouterStateSnapshot, Routes } from '@angular/router';

import { ProfileComponent } from './profile.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { Authority } from '@shared/models/authority.enum';
import { User } from '@shared/models/user.model';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UserService } from '@core/http/user.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Observable } from 'rxjs';
import { CustomTranslationService } from '@core/http/custom-translation.service';
import { map } from 'rxjs/operators';

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

export const allowLocalesResolver: ResolveFn<Array<Array<string>>> = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  customTranslation = inject(CustomTranslationService)
): Observable<Array<Array<string>>> => customTranslation.getAvailableLocales().pipe(
      map(locales => Object.entries(locales)
        .sort((a, b) => a[0] > b[0] ? 1 : -1))
  );

export const profileRoutes: Routes = [
  {
    path: 'profile',
    component: ProfileComponent,
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'profile.profile',
      breadcrumb: {
        label: 'profile.profile',
        icon: 'account_circle'
      }
    },
    resolve: {
      user: UserProfileResolver,
      locales: allowLocalesResolver
    }
  }
];

const routes: Routes = [
  {
    path: 'profile',
    redirectTo: 'account/profile'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    UserProfileResolver
  ]
})
export class ProfileRoutingModule { }
