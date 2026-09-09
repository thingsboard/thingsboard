// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
  RelatedEntitiesAggregationComponentComponent
} from '@home/components/calculated-fields/components/related-entities-aggregation-configuration/related-entities-aggregation-component.component';
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
    RelatedEntitiesAggregationComponentComponent,
  ],
  exports: [
    RelatedEntitiesAggregationComponentComponent,
  ]
})
export class RelatedEntitiesAggregationComponentModule {
}
