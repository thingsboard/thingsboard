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
import {
  CalculatedFieldDialogComponent
} from '@home/components/calculated-fields/components/dialog/calculated-field-dialog.component';
import {
  CalculatedFieldScriptTestDialogComponent
} from '@home/components/calculated-fields/components/test-dialog/calculated-field-script-test-dialog.component';
import {
  CalculatedFieldTestArgumentsComponent
} from '@home/components/calculated-fields/components/test-arguments/calculated-field-test-arguments.component';
import {
  EntityDebugSettingsButtonComponent
} from '@home/components/entity/debug/entity-debug-settings-button.component';
import {
  GeofencingConfigurationModule
} from '@home/components/calculated-fields/components/geofencing-configuration/geofencing-configuration.module';
import {
  SimpleConfigurationModule
} from '@home/components/calculated-fields/components/simple-configuration/simple-configuration.module';
import {
  PropagationConfigurationModule
} from '@home/components/calculated-fields/components/propagation-configuration/propagation-configuration.module';
import {
  RelatedEntitiesAggregationComponentModule
} from '@home/components/calculated-fields/components/related-entities-aggregation-configuration/related-entities-aggregation-component.module';
import {
  EntityAggregationComponentModule
} from '@home/components/calculated-fields/components/entity-aggregation-configuration/entity-aggregation-component.module';
import {
  CalculatedFieldsHeaderComponent
} from '@home/components/calculated-fields/table-header/calculated-fields-header.component';
import {
  CalculatedFieldsFilterConfigComponent
} from '@home/components/calculated-fields/table-header/calculated-fields-filter-config.component';
import { CalculatedFieldComponent } from '@home/components/calculated-fields/calculated-field.component';

@NgModule({
  declarations: [
    CalculatedFieldDialogComponent,
    CalculatedFieldScriptTestDialogComponent,
    CalculatedFieldTestArgumentsComponent,
    CalculatedFieldsHeaderComponent,
    CalculatedFieldsFilterConfigComponent,
    CalculatedFieldComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    GeofencingConfigurationModule,
    EntityDebugSettingsButtonComponent,
    SimpleConfigurationModule,
    PropagationConfigurationModule,
    RelatedEntitiesAggregationComponentModule,
    EntityAggregationComponentModule,
  ],
  exports: [
    CalculatedFieldDialogComponent,
    CalculatedFieldScriptTestDialogComponent,
  ]
})
export class CalculatedFieldsModule {}
