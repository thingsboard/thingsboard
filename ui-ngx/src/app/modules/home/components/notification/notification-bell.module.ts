// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';
import { NotificationBellComponent } from '@home/components/notification/notification-bell.component';
import { ShowNotificationPopoverComponent } from '@home/components/notification/show-notification-popover.component';

@NgModule({
  declarations:
    [
      NotificationBellComponent,
      ShowNotificationPopoverComponent
    ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    NotificationBellComponent,
    ShowNotificationPopoverComponent
  ]
})
export class NotificationBellModule { }
