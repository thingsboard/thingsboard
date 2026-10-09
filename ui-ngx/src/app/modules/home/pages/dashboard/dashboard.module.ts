// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { DashboardFormComponent } from '@modules/home/pages/dashboard/dashboard-form.component';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { PublicDashboardLinkDialogComponent } from '@home/pages/dashboard/public-dashboard-link.dialog.component';
import { DASHBOARD_GROUP_CONFIG_FACTORY } from '@home/models/group/group-entities-table-config.models';
import { DashboardGroupConfigFactory } from '@home/pages/dashboard/dashboard-group-config.factory';
import { DashboardRoutingModule } from '@home/pages/dashboard/dashboard-routing.module';
import { DashboardTableHeaderComponent } from '@home/pages/dashboard/dashboard-table-header.component';
import { ImportDashboardFileDialogComponent } from "@home/pages/dashboard/import-dashboard-file-dialog.component";

@NgModule({
  declarations: [
    DashboardFormComponent,
    DashboardTableHeaderComponent,
    PublicDashboardLinkDialogComponent,
    ImportDashboardFileDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    DashboardRoutingModule
  ],
  providers: [
    {
      provide: DASHBOARD_GROUP_CONFIG_FACTORY,
      useClass: DashboardGroupConfigFactory
    }
  ]
})
export class DashboardModule {
}
