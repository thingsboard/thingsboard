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
  CalculatedFieldArgumentPanelComponent
} from '@home/components/calculated-fields/components/calculated-field-arguments/calculated-field-argument-panel.component';
import {
  CalculatedFieldArgumentsTableComponent
} from '@home/components/calculated-fields/components/calculated-field-arguments/calculated-field-arguments-table.component';
import {
  PropagateArgumentsTableComponent
} from '@home/components/calculated-fields/components/calculated-field-arguments/propagate-arguments-table.component';
import {
  RelatedAggregationArgumentsTableComponent
} from '@home/components/calculated-fields/components/calculated-field-arguments/related-aggregation-arguments-table.component';
import {
  EntityAggregationArgumentsTableComponent
} from '@home/components/calculated-fields/components/calculated-field-arguments/entity-aggregation-arguments-table.component';

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
  ],
  declarations: [
    CalculatedFieldArgumentPanelComponent,
    CalculatedFieldArgumentsTableComponent,
    PropagateArgumentsTableComponent,
    RelatedAggregationArgumentsTableComponent,
    EntityAggregationArgumentsTableComponent,
  ],
  exports: [
    CalculatedFieldArgumentsTableComponent,
    PropagateArgumentsTableComponent,
    RelatedAggregationArgumentsTableComponent,
    EntityAggregationArgumentsTableComponent,
  ]
})
export class CalculatedFieldArgumentsTableModule {}
