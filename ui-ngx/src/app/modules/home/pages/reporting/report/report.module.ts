// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '@home/dialogs/home-dialogs.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { ReportComponentsModule } from '@home/pages/reporting/components/report-components.module';
import { ReportTableHeaderComponent } from '@home/pages/reporting/report/report-table-header.component';
import { ReportRoutingModule } from '@home/pages/reporting/report/report-routing.module';
import {
  ManageReportPublicAccessModalComponent
} from '@home/pages/reporting/report/manage-report-public-access-modal.component';

@NgModule({
  declarations: [
    ReportTableHeaderComponent,
    ManageReportPublicAccessModalComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    ReportComponentsModule,
    ReportRoutingModule
  ]
})
export class ReportModule { }
