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
  ruleNodeActionConfigComponentsMap,
  RuleNodeConfigActionModule
} from '@home/components/rule-node/action/rule-node-config-action.module';
import {
  RuleNodeConfigFilterModule,
  ruleNodeFilterConfigComponentsMap
} from '@home/components/rule-node/filter/rule-node-config-filter.module';
import {
  RuleNodeCoreEnrichmentModule,
  ruleNodeEnrichmentConfigComponentsMap
} from '@home/components/rule-node/enrichment/rule-node-core-enrichment.module';
import {
  RuleNodeConfigExternalModule,
  ruleNodeExternalConfigComponentsMap
} from '@home/components/rule-node/external/rule-node-config-external.module';
import {
  RuleNodeConfigTransformModule,
  ruleNodeTransformConfigComponentsMap
} from '@home/components/rule-node/transform/rule-node-config-transform.module';
import {
  RuleNodeConfigFlowModule,
  ruleNodeFlowConfigComponentsMap
} from '@home/components/rule-node/flow/rule-node-config-flow.module';
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
    RuleNodeConfigActionModule,
    RuleNodeConfigFilterModule,
    RuleNodeCoreEnrichmentModule,
    RuleNodeConfigExternalModule,
    RuleNodeConfigTransformModule,
    RuleNodeConfigFlowModule,
    EmptyConfigComponent
  ]
})
export class RuleNodeConfigModule {}

export const ruleNodeConfigComponentsMap: Record<string, Type<IRuleNodeConfigurationComponent>> = {
  ...ruleNodeActionConfigComponentsMap,
  ...ruleNodeEnrichmentConfigComponentsMap,
  ...ruleNodeExternalConfigComponentsMap,
  ...ruleNodeFilterConfigComponentsMap,
  ...ruleNodeFlowConfigComponentsMap,
  ...ruleNodeTransformConfigComponentsMap,
  'tbNodeEmptyConfig': EmptyConfigComponent
};
