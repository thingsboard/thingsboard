///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { NgModule, Type } from '@angular/core';
import { EmptyConfigComponent } from './empty-config.component';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import {
  actionRuleNodeConfigComponentsMap,
  ActionRuleNodeConfigModule
} from '@home/components/rule-node/action/action-rule-node-config.module';
import {
  filterRuleNodeConfigComponentsMap,
  FilterRuleNodeConfigModule
} from '@home/components/rule-node/filter/filter-rule-node-config.module';
import {
  enrichmentRuleNodeConfigComponentsMap,
  EnrichmentRuleNodeCoreModule
} from '@home/components/rule-node/enrichment/enrichment-rule-node-core.module';
import {
  externalRuleNodeConfigComponentsMap,
  ExternalRuleNodeConfigModule
} from '@home/components/rule-node/external/external-rule-node-config.module';
import {
  transformationRuleNodeConfigComponentsMap,
  TransformationRuleNodeConfigModule
} from '@home/components/rule-node/transformation/transformation-rule-node-config.module';
import {
  flowRuleNodeConfigComponentsMap,
  FlowRuleNodeConfigModule
} from '@home/components/rule-node/flow/flow-rule-node-config.module';
import { IRuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

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
export class RuleNodeConfigModule {}

export const ruleNodeConfigComponentsMap: Record<string, Type<IRuleNodeConfigurationComponent>> = {
  ...actionRuleNodeConfigComponentsMap,
  ...enrichmentRuleNodeConfigComponentsMap,
  ...externalRuleNodeConfigComponentsMap,
  ...filterRuleNodeConfigComponentsMap,
  ...flowRuleNodeConfigComponentsMap,
  ...transformationRuleNodeConfigComponentsMap,
  'tbNodeEmptyConfig': EmptyConfigComponent
};
