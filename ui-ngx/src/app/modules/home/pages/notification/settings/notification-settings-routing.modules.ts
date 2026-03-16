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

import { Routes } from '@angular/router';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { Authority } from '@shared/models/authority.enum';
import { inject, NgModule } from '@angular/core';
import { NotificationSettingsComponent } from '@home/pages/notification/settings/notification-settings.component';
import { NotificationService } from '@core/http/notification.service';

export const notificationUserSettingsRoutes: Routes = [
  {
    path: 'notificationSettings',
    component: NotificationSettingsComponent,
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'account.notification-settings',
      breadcrumb: {
        label: 'account.notification-settings',
        icon: 'settings'
      }
    },
    resolve: {
      userSettings: () => inject(NotificationService).getNotificationUserSettings()
    }
  }
];
