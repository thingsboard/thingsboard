// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '@home/dialogs/home-dialogs.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import {
  ScheduledReportTableHeaderComponent
} from '@home/pages/reporting/scheduling/scheduled-report-table-header.component';
import { ScheduledReportRoutingModule } from '@home/pages/reporting/scheduling/scheduled-report-routing.module';
import { ReportComponentsModule } from '@home/pages/reporting/components/report-components.module';

@NgModule({
  declarations: [
    ScheduledReportTableHeaderComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    ReportComponentsModule,
    ScheduledReportRoutingModule
  ]
})
export class ScheduledReportModule { }
