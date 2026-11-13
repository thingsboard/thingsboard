// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import {
  ModbusDeviceProfileTransportConfigurationComponent
} from '@home/components/profile/device/modbus/modbus-device-profile-transport-configuration.component';
import { ModbusDeviceProfileMappingsComponent } from '@home/components/profile/device/modbus/modbus-device-profile-mappings.component';

@NgModule({
  declarations: [
    ModbusDeviceProfileTransportConfigurationComponent,
    ModbusDeviceProfileMappingsComponent
  ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    ModbusDeviceProfileTransportConfigurationComponent
  ]
})
export class ModbusDeviceProfileTransportModule { }
