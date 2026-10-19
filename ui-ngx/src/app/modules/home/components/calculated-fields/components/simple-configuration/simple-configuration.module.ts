// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import {
  SimpleConfigurationComponent
} from '@home/components/calculated-fields/components/simple-configuration/simple-configuration.component';
import {
  CalculatedFieldOutputModule
} from '@home/components/calculated-fields/components/output/calculated-field-output.module';
import {
  CalculatedFieldArgumentsTableModule
} from '@home/components/calculated-fields/components/calculated-field-arguments/calculated-field-arguments-table.module';

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    CalculatedFieldOutputModule,
    CalculatedFieldArgumentsTableModule,
  ],
  declarations: [
    SimpleConfigurationComponent,
  ],
  exports: [
    SimpleConfigurationComponent
  ]
})
export class SimpleConfigurationModule {}
