// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { EmptyConfigComponent } from './empty-config.component';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { ActionRuleNodeConfigModule } from '@home/components/rule-node/action/action-rule-node-config.module';
import { FilterRuleNodeConfigModule } from '@home/components/rule-node/filter/filter-rule-node-config.module';
import { EnrichmentRuleNodeCoreModule } from '@home/components/rule-node/enrichment/enrichment-rule-node-core.module';
import { ExternalRuleNodeConfigModule } from '@home/components/rule-node/external/external-rule-node-config.module';
import {
  TransformationRuleNodeConfigModule
} from '@home/components/rule-node/transformation/transformation-rule-node-config.module';
import { FlowRuleNodeConfigModule } from '@home/components/rule-node/flow/flow-rule-node-config.module';
import { RuleChainService } from '@core/http/rule-chain.service';

@NgModule({
  declarations: [
    EmptyConfigComponent
  ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    ActionRuleNodeConfigModule,
    FilterRuleNodeConfigModule,
    EnrichmentRuleNodeCoreModule,
    ExternalRuleNodeConfigModule,
    TransformationRuleNodeConfigModule,
    FlowRuleNodeConfigModule,
    EmptyConfigComponent
  ]
})
export class RuleNodeConfigModule {
  constructor(private ruleChainService: RuleChainService) {
    this.ruleChainService.registerSystemRuleNodeConfigModule(this.constructor);
  }
}
