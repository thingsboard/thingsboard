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
import { Lwm2mDeviceProfileTransportConfigurationComponent } from './lwm2m-device-profile-transport-configuration.component';
import { Lwm2mObjectListComponent } from './lwm2m-object-list.component';
import { Lwm2mObserveAttrTelemetryComponent } from './lwm2m-observe-attr-telemetry.component';
import { Lwm2mObserveAttrTelemetryResourceComponent } from './lwm2m-observe-attr-telemetry-resource.component';
import { Lwm2mDeviceConfigServerComponent } from './lwm2m-device-config-server.component';
import { Lwm2mObjectAddInstancesComponent } from './lwm2m-object-add-instances.component';
import { Lwm2mObjectAddInstancesListComponent } from './lwm2m-object-add-instances-list.component';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';

@NgModule({
  declarations:
    [
      Lwm2mDeviceProfileTransportConfigurationComponent,
      Lwm2mObjectListComponent,
      Lwm2mObserveAttrTelemetryComponent,
      Lwm2mObserveAttrTelemetryResourceComponent,
      Lwm2mDeviceConfigServerComponent,
      Lwm2mObjectAddInstancesComponent,
      Lwm2mObjectAddInstancesListComponent
    ],
  imports: [
    CommonModule,
    SharedModule
   ],
  exports: [
    Lwm2mDeviceProfileTransportConfigurationComponent,
    Lwm2mObjectListComponent,
    Lwm2mObserveAttrTelemetryComponent,
    Lwm2mObserveAttrTelemetryResourceComponent,
    Lwm2mDeviceConfigServerComponent,
    Lwm2mObjectAddInstancesComponent,
    Lwm2mObjectAddInstancesListComponent
  ],
  providers: [
  ]
})
export class Lwm2mProfileComponentsModule { }
