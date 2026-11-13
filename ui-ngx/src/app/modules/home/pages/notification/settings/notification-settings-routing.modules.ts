// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Routes } from '@angular/router';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { Authority } from '@shared/models/authority.enum';
import { inject } from '@angular/core';
import { NotificationSettingsComponent } from '@home/pages/notification/settings/notification-settings.component';
import { NotificationService } from '@core/http/notification.service';
import { MenuId } from '@core/services/menu.models';

export const notificationUserSettingsRoutes: Routes = [
  {
    path: 'notificationSettings',
    component: NotificationSettingsComponent,
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      requiredMenuLink: MenuId.notification_inbox,
      title: 'account.notification-settings',
      breadcrumb: {
        label: 'account.notification-settings',
        icon: 'mdi:cog-outline'
      }
    },
    resolve: {
      userSettings: () => inject(NotificationService).getNotificationUserSettings()
    }
  }
];
