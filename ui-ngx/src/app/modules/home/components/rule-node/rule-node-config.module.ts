///
/// Copyright © 2016-2026 The Thingsboard Authors
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
