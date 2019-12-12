///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import { FcNode, FcEdge, FcModel } from 'ngx-flowchart/dist/ngx-flowchart';
import { RuleNodeComponentDescriptor, RuleNodeConfiguration } from '@shared/models/rule-node.models';
import { RuleNodeId } from '@app/shared/models/id/rule-node-id';
import { RuleChainId } from '@shared/models/id/rule-chain-id';

export interface FcRuleNodeType extends FcNode {
  component: RuleNodeComponentDescriptor;
  nodeClass: string;
  icon: string;
  iconUrl?: string;
}

export interface FcRuleNodeTypeModel extends FcModel {
  nodes: Array<FcRuleNodeType>;
}

export interface FcRuleNode extends FcRuleNodeType {
  ruleNodeId?: RuleNodeId;
  additionalInfo?: any;
  configuration?: RuleNodeConfiguration;
  debugMode?: boolean;
  targetRuleChainId?: string;
  error?: string;
  highlighted?: boolean;
}

export interface FcRuleEdge extends FcEdge {
  labels?: string[];
}

export interface FcRuleNodeModel extends FcModel {
  nodes: Array<FcRuleNode>;
  edges: Array<FcRuleEdge>;
}
