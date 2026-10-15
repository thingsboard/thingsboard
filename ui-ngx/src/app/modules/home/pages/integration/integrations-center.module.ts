// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { IntegrationsCenterRoutingModule } from '@home/pages/integration/integrations-center-routing.module';

@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    SharedModule,
    IntegrationsCenterRoutingModule
  ]
})
export class IntegrationsCenterModule { }
