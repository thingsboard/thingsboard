// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { AiModelRoutingModule } from '@home/pages/ai-model/ai-model-routing.module';
import { AiModelTableHeaderComponent } from '@home/pages/ai-model/ai-model-table-header.component';

@NgModule({
  declarations: [
    AiModelTableHeaderComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    AiModelRoutingModule
  ]
})
export class AiModelModule { }
