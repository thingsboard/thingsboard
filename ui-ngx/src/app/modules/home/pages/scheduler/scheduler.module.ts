// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { SchedulerRoutingModule } from '@home/pages/scheduler/scheduler-routing.module';

@NgModule({
  declarations: [
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    SchedulerRoutingModule
  ]
})
export class SchedulerModule { }
