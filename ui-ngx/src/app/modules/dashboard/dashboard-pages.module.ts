// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { HomeDialogsModule } from '@app/modules/home/dialogs/home-dialogs.module';
import { DashboardPagesRoutingModule } from './dashboard-pages.routing.module';

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    DashboardPagesRoutingModule
  ]
})
export class DashboardPagesModule { }
