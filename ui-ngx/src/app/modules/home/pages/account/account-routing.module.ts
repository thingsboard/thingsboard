// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { inject, NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { Authority } from '@shared/models/authority.enum';
import { securityRoutes } from '@home/pages/security/security-routing.module';
import { profileRoutes } from '@home/pages/profile/profile-routing.module';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  notificationUserSettingsRoutes
} from '@home/pages/notification/settings/notification-settings-routing.modules';

const routes: Routes = [
  {
    path: 'account',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      showMainLoadingBar: false,
      breadcrumb: {
        label: 'account.account',
        icon: 'account_circle'
      },
      useChildrenRoutesForTabs: true,
    },
    resolve: {
      replaceUrl: () => getCurrentAuthState(inject(Store<AppState>)).forceFullscreen
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          redirectTo: '/account/profile',
        }
      },
      ...profileRoutes,
      ...securityRoutes,
      ...notificationUserSettingsRoutes
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AccountRoutingModule { }
