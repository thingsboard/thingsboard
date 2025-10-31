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
  CalculatedFieldOutputModule
} from '@home/components/calculated-fields/components/output/calculated-field-output.module';
import {
  CalculatedFieldArgumentsTableModule
} from '@home/components/calculated-fields/components/calculated-field-arguments/calculated-field-arguments-table.module';
import {
  EntityAggregationComponentComponent
} from '@home/components/calculated-fields/components/entity-aggregation-configuration/entity-aggregation-component.component';
import {
  CalculatedFieldMetricsTableModule
} from '@home/components/calculated-fields/components/metrics/calculated-field-metrics-table.module';

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    CalculatedFieldOutputModule,
    CalculatedFieldArgumentsTableModule,
    CalculatedFieldMetricsTableModule,
  ],
  declarations: [
    EntityAggregationComponentComponent,
  ],
  exports: [
    EntityAggregationComponentComponent,
  ]
})
export class EntityAggregationComponentModule {
}
