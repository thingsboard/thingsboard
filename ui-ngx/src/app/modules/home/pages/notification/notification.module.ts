// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { NotificationRoutingModule } from '@home/pages/notification/notification-routing.module';
import { InboxTableHeaderComponent } from '@home/pages/notification/inbox/inbox-table-header.component';
import { InboxNotificationDialogComponent } from '@home/pages/notification/inbox/inbox-notification-dialog.component';
import { HomeComponentsModule } from '@home/components/home-components.module';
import { SentErrorDialogComponent } from '@home/pages/notification/sent/sent-error-dialog.component';
import { SentNotificationDialogComponent } from '@home/pages/notification/sent/sent-notification-dialog.componet';
import {
  RecipientNotificationDialogComponent
} from '@home/pages/notification/recipient/recipient-notification-dialog.component';
import {
  TemplateNotificationDialogComponent
} from '@home/pages/notification/template/template-notification-dialog.component';
import { EscalationFormComponent } from '@home/pages/notification/rule/escalation-form.component';
import { EscalationsComponent } from '@home/pages/notification/rule/escalations.component';
import { RuleNotificationDialogComponent } from '@home/pages/notification/rule/rule-notification-dialog.component';
import { NotificationSettingsComponent } from '@home/pages/notification/settings/notification-settings.component';
import {
  NotificationSettingFormComponent
} from '@home/pages/notification/settings/notification-setting-form.component';
import {
  NotificationTemplateConfigurationComponent
} from '@home/pages/notification/template/configuration/notification-template-configuration.component';
import {
  NotificationActionButtonConfigurationComponent
} from '@home/pages/notification/template/configuration/notification-action-button-configuration.component';

@NgModule({
  declarations: [
    InboxTableHeaderComponent,
    InboxNotificationDialogComponent,
    SentErrorDialogComponent,
    SentNotificationDialogComponent,
    RecipientNotificationDialogComponent,
    TemplateNotificationDialogComponent,
    EscalationFormComponent,
    EscalationsComponent,
    RuleNotificationDialogComponent,
    NotificationSettingsComponent,
    NotificationSettingFormComponent,
    NotificationTemplateConfigurationComponent,
    NotificationActionButtonConfigurationComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    NotificationRoutingModule,
    HomeComponentsModule
  ]
})
export class NotificationModule { }
