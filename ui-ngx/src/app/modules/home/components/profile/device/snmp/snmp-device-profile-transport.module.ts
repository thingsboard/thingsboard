// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { SnmpDeviceProfileTransportConfigurationComponent } from '@home/components/profile/device/snmp/snmp-device-profile-transport-configuration.component';
import { SnmpDeviceProfileCommunicationConfigComponent } from '@home/components/profile/device/snmp/snmp-device-profile-communication-config.component';
import { SnmpDeviceProfileMappingComponent } from '@home/components/profile/device/snmp/snmp-device-profile-mapping.component';

@NgModule({
  declarations: [
    SnmpDeviceProfileTransportConfigurationComponent,
    SnmpDeviceProfileCommunicationConfigComponent,
    SnmpDeviceProfileMappingComponent
  ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    SnmpDeviceProfileTransportConfigurationComponent
  ]
})
export class SnmpDeviceProfileTransportModule { }
