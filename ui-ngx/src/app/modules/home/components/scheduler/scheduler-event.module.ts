// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';
import { SchedulerEventsComponent } from '@home/components/scheduler/scheduler-events.component';
import { SchedulerEventDialogComponent } from '@home/components/scheduler/scheduler-event-dialog.component';
import { SchedulerEventTypeAutocompleteComponent } from '@home/components/scheduler/scheduler-event-type-autocomplete.component';
import { SchedulerEventConfigComponent } from '@home/components/scheduler/scheduler-event-config.component';
import { SchedulerEventTemplateConfigComponent } from '@home/components/scheduler/scheduler-event-template-config.component';
import { SendRpcRequestComponent } from '@home/components/scheduler/config/send-rpc-request.component';
import { UpdateAttributesComponent } from '@home/components/scheduler/config/update-attributes.component';
import { AttributeKeyValueTableComponent } from '@home/components/scheduler/config/attribute-key-value-table.component';
import { GenerateDashboardReportComponent } from '@home/components/scheduler/config/generate-dashboard-report.component';
import { DashboardReportConfigComponent } from '@home/components/scheduler/config/dashboard-report-config.component';
import { SelectDashboardStateDialogComponent } from '@home/components/scheduler/config/select-dashboard-state-dialog.component';
import { EmailConfigComponent } from '@home/components/scheduler/config/email-config.component';
import { SchedulerEventScheduleComponent } from '@home/components/scheduler/scheduler-event-schedule.component';
import { ReportConfigComponent } from '@home/components/scheduler/config/report-config.component';
import { GenerateReportComponent } from '@home/components/scheduler/config/generate-report.component';

@NgModule({
  declarations:
    [
      SchedulerEventsComponent,
      SchedulerEventTypeAutocompleteComponent,
      SchedulerEventConfigComponent,
      SchedulerEventTemplateConfigComponent,
      SendRpcRequestComponent,
      UpdateAttributesComponent,
      AttributeKeyValueTableComponent,
      GenerateDashboardReportComponent,
      GenerateReportComponent,
      DashboardReportConfigComponent,
      ReportConfigComponent,
      EmailConfigComponent,
      SelectDashboardStateDialogComponent,
      SchedulerEventScheduleComponent,
      SchedulerEventDialogComponent
    ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    SchedulerEventsComponent,
    SchedulerEventTypeAutocompleteComponent,
    SchedulerEventConfigComponent,
    SchedulerEventTemplateConfigComponent,
    SendRpcRequestComponent,
    UpdateAttributesComponent,
    AttributeKeyValueTableComponent,
    GenerateDashboardReportComponent,
    GenerateReportComponent,
    DashboardReportConfigComponent,
    ReportConfigComponent,
    EmailConfigComponent,
    SelectDashboardStateDialogComponent,
    SchedulerEventScheduleComponent,
    SchedulerEventDialogComponent
  ]
})
export class SchedulerEventModule { }
