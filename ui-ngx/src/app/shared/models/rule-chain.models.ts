///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { BaseData } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { RuleChainId } from '@shared/models/id/rule-chain-id';
import { RuleNodeId } from '@shared/models/id/rule-node-id';
import { RuleNode, RuleNodeComponentDescriptor, RuleNodeType } from '@shared/models/rule-node.models';
import { ComponentType } from '@shared/models/component-descriptor.models';

export interface RuleChain extends BaseData<RuleChainId> {
  tenantId: TenantId;
  name: string;
  firstRuleNodeId: RuleNodeId;
  root: boolean;
  debugMode: boolean;
  type: string;
  configuration?: any;
  additionalInfo?: any;
  isDefault?: boolean;
}

export interface RuleChainMetaData {
  ruleChainId: RuleChainId;
  firstNodeIndex?: number;
  nodes: Array<RuleNode>;
  connections: Array<NodeConnectionInfo>;
  ruleChainConnections: Array<RuleChainConnectionInfo>;
}

export interface ResolvedRuleChainMetaData extends RuleChainMetaData {
  targetRuleChainsMap: {[ruleChainId: string]: RuleChain};
}

export interface RuleChainImport {
  ruleChain: RuleChain;
  metadata: RuleChainMetaData;
  resolvedMetadata?: ResolvedRuleChainMetaData;
}

export interface NodeConnectionInfo {
  fromIndex: number;
  toIndex: number;
  type: string;
}

export interface RuleChainConnectionInfo {
  fromIndex: number;
  targetRuleChainId: RuleChainId;
  additionalInfo: any;
  type: string;
}

export const ruleNodeTypeComponentTypes: ComponentType[] =
  [
    ComponentType.FILTER,
    ComponentType.ENRICHMENT,
    ComponentType.TRANSFORMATION,
    ComponentType.ACTION,
    ComponentType.EXTERNAL
  ];

export const ruleChainNodeComponent: RuleNodeComponentDescriptor = {
  type: RuleNodeType.RULE_CHAIN,
  name: 'rule chain',
  clazz: 'tb.internal.RuleChain',
  configurationDescriptor: {
    nodeDefinition: {
      description: '',
      details: 'Forwards incoming messages to specified Rule Chain',
      inEnabled: true,
      outEnabled: false,
      relationTypes: [],
      customRelations: false,
      defaultConfiguration: {}
    }
  }
};

export const unknownNodeComponent: RuleNodeComponentDescriptor = {
  type: RuleNodeType.UNKNOWN,
  name: 'unknown',
  clazz: 'tb.internal.Unknown',
  configurationDescriptor: {
    nodeDefinition: {
      description: '',
      details: '',
      inEnabled: true,
      outEnabled: true,
      relationTypes: [],
      customRelations: false,
      defaultConfiguration: {}
    }
  }
};

export const inputNodeComponent: RuleNodeComponentDescriptor = {
  type: RuleNodeType.INPUT,
  name: 'Input',
  clazz: 'tb.internal.Input'
};

export enum RuleChainType {
  CORE = 'CORE',
  EDGE = 'EDGE'
}
