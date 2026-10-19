// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { DashboardFormComponent } from '@modules/home/pages/dashboard/dashboard-form.component';
import { ManageDashboardCustomersDialogComponent } from '@modules/home/pages/dashboard/manage-dashboard-customers-dialog.component';
import { DashboardRoutingModule } from './dashboard-routing.module';
import { MakeDashboardPublicDialogComponent } from '@modules/home/pages/dashboard/make-dashboard-public-dialog.component';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { DashboardTabsComponent } from '@home/pages/dashboard/dashboard-tabs.component';

@NgModule({
  declarations: [
    DashboardFormComponent,
    DashboardTabsComponent,
    ManageDashboardCustomersDialogComponent,
    MakeDashboardPublicDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    DashboardRoutingModule
  ]
})
export class DashboardModule { }
