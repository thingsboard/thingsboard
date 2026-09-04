// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
