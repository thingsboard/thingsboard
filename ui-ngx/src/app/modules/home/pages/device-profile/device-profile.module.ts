// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { DeviceProfileTabsComponent } from './device-profile-tabs.component';
import { DeviceProfileRoutingModule } from './device-profile-routing.module';

@NgModule({
  declarations: [
    DeviceProfileTabsComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    DeviceProfileRoutingModule
  ]
})
export class DeviceProfileModule { }
