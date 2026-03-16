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

import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { RuleChainId } from '@shared/models/id/rule-chain-id';
import { RuleNodeId } from '@shared/models/id/rule-node-id';
import { RuleNode, RuleNodeComponentDescriptor, RuleNodeType } from '@shared/models/rule-node.models';
import { ComponentClusteringMode, ComponentType } from '@shared/models/component-descriptor.models';
import { HasTenantId, HasVersion } from '@shared/models/entity.models';

export interface RuleChain extends BaseData<RuleChainId>, HasTenantId, HasVersion, ExportableEntity<RuleChainId> {
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

export interface RuleChainMetaData extends HasVersion {
  ruleChainId: RuleChainId;
  firstNodeIndex?: number;
  nodes: Array<RuleNode>;
  connections: Array<NodeConnectionInfo>;
}

export interface RuleChainImport {
  ruleChain: RuleChain;
  metadata: RuleChainMetaData;
}

export interface NodeConnectionInfo {
  fromIndex: number;
  toIndex: number;
  type: string;
}

export const ruleNodeTypeComponentTypes: ComponentType[] =
  [
    ComponentType.FILTER,
    ComponentType.ENRICHMENT,
    ComponentType.TRANSFORMATION,
    ComponentType.ACTION,
    ComponentType.EXTERNAL,
    ComponentType.FLOW
  ];

export const unknownNodeComponent: RuleNodeComponentDescriptor = {
  type: RuleNodeType.UNKNOWN,
  name: 'unknown',
  clusteringMode: ComponentClusteringMode.ENABLED,
  configurationVersion: 0,
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
  configurationVersion: 0,
  clusteringMode: ComponentClusteringMode.ENABLED,
  name: 'Input',
  clazz: 'tb.internal.Input'
};

export enum RuleChainType {
  CORE = 'CORE',
  EDGE = 'EDGE'
}
