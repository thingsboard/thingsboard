///
/// Copyright © 2016-2022 The Thingsboard Authors
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
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { NotificationCenterRoutingModule } from './notification-center-routing.module';
import { NotificationCenterComponent } from './notification-center.component';
import { HomeComponentsModule } from '@home/components/home-components.module';
import {
  TargetNotificationDialogComponent
} from '@home/pages/notification-center/targets-table/target-notification-dialog.componet';
import {
  NotificationTableComponent
} from '@home/pages/notification-center/notification-table/notification-table.component';
import {
  TemplateNotificationDialogComponent
} from '@home/pages/notification-center/template-table/template-notification-dialog.component';
import {
  TargetTableHeaderComponent
} from '@home/pages/notification-center/targets-table/target-table-header.component';
import {
  TemplateTableHeaderComponent
} from '@home/pages/notification-center/template-table/template-table-header.component';
import {
  RequestNotificationDialogComponent
} from '@home/pages/notification-center/request-table/request-notification-dialog.componet';
import {
  TemplateAutocompleteComponent
} from '@home/pages/notification-center/template-table/template-autocomplete.component';
import { InboxTableHeaderComponent } from '@home/pages/notification-center/inbox-table/inbox-table-header.component';
import { RuleTableHeaderComponent } from '@home/pages/notification-center/rule-table/rule-table-header.component';
import { RuleNotificationDialogComponent } from '@home/pages/notification-center/rule-table/rule-notification-dialog.component';
import { EscalationsComponent } from '@home/pages/notification-center/rule-table/escalations.component';
import { EscalationFormComponent } from '@home/pages/notification-center/rule-table/escalation-form.component';

@NgModule({
  declarations: [
    NotificationCenterComponent,
    TargetNotificationDialogComponent,
    TemplateNotificationDialogComponent,
    RequestNotificationDialogComponent,
    TargetTableHeaderComponent,
    TemplateTableHeaderComponent,
    NotificationTableComponent,
    TemplateAutocompleteComponent,
    InboxTableHeaderComponent,
    RuleTableHeaderComponent,
    RuleNotificationDialogComponent,
    EscalationsComponent,
    EscalationFormComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    NotificationCenterRoutingModule,
    HomeComponentsModule
  ]
})
export class NotificationCenterModule { }
