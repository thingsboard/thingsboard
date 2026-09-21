// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/public-api';
import { RuleChainInputComponent } from './rule-chain-input.component';
import { RuleChainOutputComponent } from './rule-chain-output.component';

@NgModule({
  declarations: [
    RuleChainInputComponent,
    RuleChainOutputComponent
  ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    RuleChainInputComponent,
    RuleChainOutputComponent
  ]
})
export class FlowRuleNodeConfigModule {
}
