// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { DeviceComponent } from '@modules/home/pages/device/device.component';
import { DeviceCredentialsDialogComponent } from '@modules/home/pages/device/device-credentials-dialog.component';
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { DeviceGroupConfigFactory } from '@home/pages/device/device-group-config.factory';
import { DEVICE_GROUP_CONFIG_FACTORY } from '@home/models/group/group-entities-table-config.models';
import { DefaultDeviceConfigurationComponent } from './data/default-device-configuration.component';
import { DeviceConfigurationComponent } from './data/device-configuration.component';
import { DeviceDataComponent } from './data/device-data.component';
import { DefaultDeviceTransportConfigurationComponent } from './data/default-device-transport-configuration.component';
import { DeviceTransportConfigurationComponent } from './data/device-transport-configuration.component';
import { MqttDeviceTransportConfigurationComponent } from './data/mqtt-device-transport-configuration.component';
import { CoapDeviceTransportConfigurationComponent } from './data/coap-device-transport-configuration.component';
import { Lwm2mDeviceTransportConfigurationComponent } from './data/lwm2m-device-transport-configuration.component';
import { SnmpDeviceTransportConfigurationComponent } from './data/snmp-device-transport-configuration.component';
import { DeviceCredentialsModule } from '@home/components/device/device-credentials.module';
import { DeviceProfileCommonModule } from '@home/components/profile/device/common/device-profile-common.module';
import { DeviceCheckConnectivityDialogComponent } from './device-check-connectivity-dialog.component';
import { DeviceRoutingModule } from '@home/pages/device/device-routing.module';
import { DeviceTableHeaderComponent } from '@home/pages/device/device-table-header.component';

@NgModule({
  declarations: [
    DefaultDeviceConfigurationComponent,
    DeviceConfigurationComponent,
    DefaultDeviceTransportConfigurationComponent,
    MqttDeviceTransportConfigurationComponent,
    CoapDeviceTransportConfigurationComponent,
    Lwm2mDeviceTransportConfigurationComponent,
    SnmpDeviceTransportConfigurationComponent,
    DeviceTransportConfigurationComponent,
    DeviceDataComponent,
    DeviceComponent,
    DeviceTableHeaderComponent,
    DeviceCredentialsDialogComponent,
    DeviceCheckConnectivityDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    DeviceCredentialsModule,
    DeviceProfileCommonModule,
    DeviceRoutingModule
  ],
  providers: [
    {
      provide: DEVICE_GROUP_CONFIG_FACTORY,
      useClass: DeviceGroupConfigFactory
    }
  ]
})
export class DeviceModule { }
