///
/// Copyright © 2016-2021 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { DeviceComponent } from '@modules/home/pages/device/device.component';
import { DeviceRoutingModule } from './device-routing.module';
import { DeviceTableHeaderComponent } from '@modules/home/pages/device/device-table-header.component';
import { DeviceCredentialsDialogComponent } from '@modules/home/pages/device/device-credentials-dialog.component';
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { DeviceTabsComponent } from '@home/pages/device/device-tabs.component';
import { DefaultDeviceConfigurationComponent } from './data/default-device-configuration.component';
import { DeviceConfigurationComponent } from './data/device-configuration.component';
import { DeviceDataComponent } from './data/device-data.component';
import { DefaultDeviceTransportConfigurationComponent } from './data/default-device-transport-configuration.component';
import { DeviceTransportConfigurationComponent } from './data/device-transport-configuration.component';
import { MqttDeviceTransportConfigurationComponent } from './data/mqtt-device-transport-configuration.component';
import { Lwm2mDeviceTransportConfigurationComponent } from './data/lwm2m-device-transport-configuration.component';
import { SnmpDeviceTransportConfigurationComponent } from './data/snmp-device-transport-configuration.component';

@NgModule({
  declarations: [
    DefaultDeviceConfigurationComponent,
    DeviceConfigurationComponent,
    DefaultDeviceTransportConfigurationComponent,
    MqttDeviceTransportConfigurationComponent,
    Lwm2mDeviceTransportConfigurationComponent,
    SnmpDeviceTransportConfigurationComponent,
    DeviceTransportConfigurationComponent,
    DeviceDataComponent,
    DeviceComponent,
    DeviceTabsComponent,
    DeviceTableHeaderComponent,
    DeviceCredentialsDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    DeviceRoutingModule
  ]
})
export class DeviceModule { }
