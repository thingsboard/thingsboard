// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import {
  CalculatedFieldOutputComponent
} from '@home/components/calculated-fields/components/output/calculated-field-output.component';

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
  ],
  declarations: [
    CalculatedFieldOutputComponent
  ],
  exports: [
    CalculatedFieldOutputComponent
  ]
})
export class CalculatedFieldOutputModule { }
