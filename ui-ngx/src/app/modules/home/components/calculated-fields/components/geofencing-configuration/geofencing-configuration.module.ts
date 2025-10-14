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
import {
  CalculatedFieldGeofencingZoneGroupsTableComponent
} from '@home/components/calculated-fields/components/geofencing-configuration/calculated-field-geofencing-zone-groups-table.component';
import {
  CalculatedFieldGeofencingZoneGroupsPanelComponent
} from '@home/components/calculated-fields/components/geofencing-configuration/calculated-field-geofencing-zone-groups-panel.component';
import { SharedModule } from '@shared/shared.module';
import {
  GeofencingConfigurationComponent
} from '@home/components/calculated-fields/components/geofencing-configuration/geofencing-configuration.component';
import {
  CalculatedFieldOutputModule
} from '@home/components/calculated-fields/components/output/caclculate-field-output.module';

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    CalculatedFieldOutputModule
  ],
  declarations: [
    CalculatedFieldGeofencingZoneGroupsTableComponent,
    CalculatedFieldGeofencingZoneGroupsPanelComponent,
    GeofencingConfigurationComponent
  ],
  exports: [
    CalculatedFieldGeofencingZoneGroupsTableComponent,
    CalculatedFieldGeofencingZoneGroupsPanelComponent,
    GeofencingConfigurationComponent
  ]
})
export class GeofencingConfigurationModule {

}
