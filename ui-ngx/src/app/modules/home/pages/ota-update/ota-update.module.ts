// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@home/components/home-components.module';
import { OtaUpdateRoutingModule } from '@home/pages/ota-update/ota-update-routing.module';
import { OtaUpdateComponent } from '@home/pages/ota-update/ota-update.component';
import { OtaUpdateTabsComponent } from '@home/pages/ota-update/ota-update-tabs.component';

@NgModule({
  declarations: [
    OtaUpdateComponent,
    OtaUpdateTabsComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    OtaUpdateRoutingModule
  ]
})
export class OtaUpdateModule { }
