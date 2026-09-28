// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { RoleComponent } from '@home/pages/role/role.component';
import { RoleTabsComponent } from '@home/pages/role/role-tabs.component';
import { RoleRoutingModule } from '@home/pages/role/role-routing.module';

@NgModule({
  declarations: [
    RoleComponent,
    RoleTabsComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    RoleRoutingModule
  ]
})
export class RoleModule { }
