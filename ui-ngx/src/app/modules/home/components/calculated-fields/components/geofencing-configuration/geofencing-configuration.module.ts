// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
} from '@home/components/calculated-fields/components/output/calculated-field-output.module';

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
