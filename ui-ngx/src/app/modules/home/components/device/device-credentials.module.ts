// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { CopyDeviceCredentialsComponent } from '@home/components/device/copy-device-credentials.component';
import { DeviceCredentialsComponent } from '@home/components/device/device-credentials.component';
import { DeviceCredentialsLwm2mComponent } from '@home/components/device/device-credentials-lwm2m.component';
import { DeviceCredentialsLwm2mServerComponent } from '@home/components/device/device-credentials-lwm2m-server.component';
import { DeviceCredentialsMqttBasicComponent } from '@home/components/device/device-credentials-mqtt-basic.component';

@NgModule({
  declarations: [
    CopyDeviceCredentialsComponent,
    DeviceCredentialsComponent,
    DeviceCredentialsLwm2mComponent,
    DeviceCredentialsLwm2mServerComponent,
    DeviceCredentialsMqttBasicComponent
  ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    CopyDeviceCredentialsComponent,
    DeviceCredentialsComponent,
    DeviceCredentialsLwm2mComponent,
    DeviceCredentialsLwm2mServerComponent,
    DeviceCredentialsMqttBasicComponent
  ]
})
export class DeviceCredentialsModule { }
