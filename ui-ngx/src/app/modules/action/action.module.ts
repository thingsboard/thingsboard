// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';
import { ActionRoutingModule } from '@modules/action/action-routing.module';

@NgModule({
  declarations:
    [],
  imports: [
    CommonModule,
    SharedModule,
    ActionRoutingModule
  ]
})
export class ActionModule { }
