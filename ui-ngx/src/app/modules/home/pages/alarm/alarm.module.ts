// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { AlarmRoutingModule } from '@home/pages/alarm/alarm-routing.module';
import { AlarmRulesTabsComponent } from '@home/pages/alarm/alarm-rules-tabs.component';

@NgModule({
  declarations: [
    AlarmRulesTabsComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    AlarmRoutingModule
  ]
})
export class AlarmModule { }
