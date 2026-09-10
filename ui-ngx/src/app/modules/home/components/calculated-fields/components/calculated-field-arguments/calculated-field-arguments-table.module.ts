// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
