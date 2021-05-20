///
/// Copyright © 2016-2021 The Thingsboard Authors
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
