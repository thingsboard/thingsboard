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
import { CalculatedFieldsTableComponent } from '@home/components/calculated-fields/calculated-fields-table.component';
import {
  CalculatedFieldDialogComponent
} from '@home/components/calculated-fields/components/dialog/calculated-field-dialog.component';
import {
  CalculatedFieldDebugDialogComponent
} from '@home/components/calculated-fields/components/debug-dialog/calculated-field-debug-dialog.component';
import {
  CalculatedFieldScriptTestDialogComponent
} from '@home/components/calculated-fields/components/test-dialog/calculated-field-script-test-dialog.component';
import {
  CalculatedFieldTestArgumentsComponent
} from '@home/components/calculated-fields/components/test-arguments/calculated-field-test-arguments.component';
import {
  EntityDebugSettingsButtonComponent
} from '@home/components/entity/debug/entity-debug-settings-button.component';
import { HomeComponentsModule } from '@home/components/home-components.module';
import {
  GeofencingConfigurationModule
} from '@home/components/calculated-fields/components/geofencing-configuration/geofencing-configuration.module';
import {
  SimpleConfigurationModule
} from '@home/components/calculated-fields/components/simple-configuration/simple-configuration.module';
import {
  PropagationConfigurationModule
} from '@home/components/calculated-fields/components/propagation-configuration/propagation-configuration.module';

@NgModule({
  declarations: [
    CalculatedFieldsTableComponent,
    CalculatedFieldDialogComponent,
    CalculatedFieldDebugDialogComponent,
    CalculatedFieldScriptTestDialogComponent,
    CalculatedFieldTestArgumentsComponent,
  ],
  imports: [
    CommonModule,
    SharedModule,
    GeofencingConfigurationModule,
    EntityDebugSettingsButtonComponent,
    HomeComponentsModule,
    SimpleConfigurationModule,
    PropagationConfigurationModule,
  ],
  exports: [
    CalculatedFieldsTableComponent,
  ]
})
export class CalculatedFieldsModule {}
