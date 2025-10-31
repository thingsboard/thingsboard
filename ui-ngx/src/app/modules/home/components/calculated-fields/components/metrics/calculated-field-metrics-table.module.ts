import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import {
  CalculatedFieldMetricsTableComponent
} from '@home/components/calculated-fields/components/metrics/calculated-field-metrics-table.component';
import {
  CalculatedFieldMetricsPanelComponent
} from '@home/components/calculated-fields/components/metrics/calculated-field-metrics-panel.component';


@NgModule({
  imports: [
    CommonModule,
    SharedModule,
  ],
  declarations: [
    CalculatedFieldMetricsTableComponent,
    CalculatedFieldMetricsPanelComponent
  ],
  exports: [
    CalculatedFieldMetricsTableComponent
  ]
})
export class CalculatedFieldMetricsTableModule {

}
