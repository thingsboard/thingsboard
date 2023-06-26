///
/// Copyright © 2016-2023 The Thingsboard Authors
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
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { Authority } from '@shared/models/authority.enum';
import { ProfileComponent } from '@home/pages/profile/profile.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { SecurityComponent } from '@home/pages/security/security.component';
import { UserTwoFAProvidersResolver } from '@home/pages/security/security-routing.module';
import { NotificationSettingsComponent } from '@home/pages/notification/settings/notification-settings.component';
import {
  NotificationUserSettingsResolver
} from '@home/pages/notification/settings/notification-settings-routing.modules';
import { UserProfileResolver } from '@home/pages/profile/profile-routing.module';

const routes: Routes = [
  {
    path: 'account',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {
        label: 'account.account',
        icon: 'account_circle'
      }
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
      {
        path: 'profile',
        component: ProfileComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'account.personal-info',
          breadcrumb: {
            label: 'account.personal-info',
            icon: 'mdi:badge-account-horizontal',
          }
        },
        resolve: {
          user: UserProfileResolver
        }
      },
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
      },
      {
        path: 'notificationSettings',
        component: NotificationSettingsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'account.notification-settings',
          breadcrumb: {
            label: 'account.notification-settings',
            icon: 'settings'
          }
        },
        resolve: {
          userSettings: NotificationUserSettingsResolver
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    UserProfileResolver,
    UserTwoFAProvidersResolver,
    NotificationUserSettingsResolver
  ]
})
export class AccountRoutingModule { }
