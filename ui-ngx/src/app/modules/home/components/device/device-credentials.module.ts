///
/// Copyright © 2016-2025 The Thingsboard Authors
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
