// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { ReportingRoutingModule } from '@home/pages/reporting/reporting-routing.module';
import { ReportTemplateModule } from '@home/pages/reporting/template/report-template.module';
import { ScheduledReportModule } from '@home/pages/reporting/scheduling/scheduled-report.module';
import { ReportModule } from '@home/pages/reporting/report/report.module';

@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    SharedModule,
    ReportTemplateModule,
    ScheduledReportModule,
    ReportModule,
    ReportingRoutingModule
  ]
})
export class ReportingModule { }
