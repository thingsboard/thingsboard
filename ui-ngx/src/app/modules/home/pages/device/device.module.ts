///
/// Copyright © 2016-2020 The Thingsboard Authors
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
import { DeviceCredentialsDialogLwm2mComponent } from '@home/pages/device/device-credentials-dialog-lwm2m.component';
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { DeviceTabsComponent } from '@home/pages/device/device-tabs.component';

@NgModule({
  declarations: [
    DeviceComponent,
    DeviceTabsComponent,
    DeviceTableHeaderComponent,
    DeviceCredentialsDialogComponent,
    DeviceCredentialsDialogLwm2mComponent
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
