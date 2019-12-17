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

import {BaseData} from '@shared/models/base-data';
import {AssetId} from '@shared/models/id/asset-id';
import {TenantId} from '@shared/models/id/tenant-id';
import {CustomerId} from '@shared/models/id/customer-id';
import {RuleChainId} from '@shared/models/id/rule-chain-id';
import {RuleNodeId} from '@shared/models/id/rule-node-id';
import { ComponentDescriptor, ComponentType } from '@shared/models/component-descriptor.models';
import { EntityType, EntityTypeResource } from '@shared/models/entity-type.models';

export enum MsgDataType {
  JSON = 'JSON',
  TEXT = 'TEXT',
  BINARY = 'BINARY'
}

export interface RuleNodeConfiguration {
  [key: string]: any;
  // TODO:
}

export interface RuleNode extends BaseData<RuleNodeId> {
  ruleChainId: RuleChainId;
  type: string;
  name: string;
  debugMode: boolean;
  configuration: RuleNodeConfiguration;
  additionalInfo?: any;
}

export interface LinkLabel {
  name: string;
  value: string;
}

export interface RuleNodeConfigurationDescriptor {
  nodeDefinition: {
    description: string;
    details: string;
    inEnabled: boolean;
    outEnabled: boolean;
    relationTypes: string[];
    customRelations: boolean;
    defaultConfiguration: any;
    icon?: string;
    iconUrl?: string;
    docUrl?: string;
    uiResources?: string[];
    uiResourceLoadError?: string;
  };
}

export enum RuleNodeType {
  FILTER = 'FILTER',
  ENRICHMENT = 'ENRICHMENT',
  TRANSFORMATION = 'TRANSFORMATION',
  ACTION = 'ACTION',
  EXTERNAL = 'EXTERNAL',
  RULE_CHAIN = 'RULE_CHAIN',
  UNKNOWN = 'UNKNOWN',
  INPUT = 'INPUT'
}

export const ruleNodeTypesLibrary = [
  RuleNodeType.FILTER,
  RuleNodeType.ENRICHMENT,
  RuleNodeType.TRANSFORMATION,
  RuleNodeType.ACTION,
  RuleNodeType.EXTERNAL,
  RuleNodeType.RULE_CHAIN,
];

export interface RuleNodeTypeDescriptor {
  value: RuleNodeType;
  name: string;
  details: string;
  nodeClass: string;
  icon: string;
  special?: boolean;
}

export const ruleNodeTypeDescriptors = new Map<RuleNodeType, RuleNodeTypeDescriptor>(
  [
    [
      RuleNodeType.FILTER,
      {
        value: RuleNodeType.FILTER,
        name: 'rulenode.type-filter',
        details: 'rulenode.type-filter-details',
        nodeClass: 'tb-filter-type',
        icon: 'filter_list'
      }
    ],
    [
      RuleNodeType.ENRICHMENT,
      {
        value: RuleNodeType.ENRICHMENT,
        name: 'rulenode.type-enrichment',
        details: 'rulenode.type-enrichment-details',
        nodeClass: 'tb-enrichment-type',
        icon: 'playlist_add'
      }
    ],
    [
      RuleNodeType.TRANSFORMATION,
      {
        value: RuleNodeType.TRANSFORMATION,
        name: 'rulenode.type-transformation',
        details: 'rulenode.type-transformation-details',
        nodeClass: 'tb-transformation-type',
        icon: 'transform'
      }
    ],
    [
      RuleNodeType.ACTION,
      {
        value: RuleNodeType.ACTION,
        name: 'rulenode.type-action',
        details: 'rulenode.type-action-details',
        nodeClass: 'tb-action-type',
        icon: 'flash_on'
      }
    ],
    [
      RuleNodeType.EXTERNAL,
      {
        value: RuleNodeType.EXTERNAL,
        name: 'rulenode.type-external',
        details: 'rulenode.type-external-details',
        nodeClass: 'tb-external-type',
        icon: 'cloud_upload'
      }
    ],
    [
      RuleNodeType.RULE_CHAIN,
      {
        value: RuleNodeType.RULE_CHAIN,
        name: 'rulenode.type-rule-chain',
        details: 'rulenode.type-rule-chain-details',
        nodeClass: 'tb-rule-chain-type',
        icon: 'settings_ethernet'
      }
    ],
    [
      RuleNodeType.INPUT,
      {
        value: RuleNodeType.INPUT,
        name: 'rulenode.type-input',
        details: 'rulenode.type-input-details',
        nodeClass: 'tb-input-type',
        icon: 'input',
        special: true
      }
    ],
    [
      RuleNodeType.UNKNOWN,
      {
        value: RuleNodeType.UNKNOWN,
        name: 'rulenode.type-unknown',
        details: 'rulenode.type-unknown-details',
        nodeClass: 'tb-unknown-type',
        icon: 'help_outline'
      }
    ]
  ]
);

export interface RuleNodeComponentDescriptor extends ComponentDescriptor {
  type: RuleNodeType;
  configurationDescriptor?: RuleNodeConfigurationDescriptor;
}

const ruleNodeClazzHelpLinkMap = {
  'org.thingsboard.rule.engine.filter.TbCheckRelationNode': 'ruleNodeCheckRelation',
  'org.thingsboard.rule.engine.filter.TbJsFilterNode': 'ruleNodeJsFilter',
  'org.thingsboard.rule.engine.filter.TbJsSwitchNode': 'ruleNodeJsSwitch',
  'org.thingsboard.rule.engine.filter.TbMsgTypeFilterNode': 'ruleNodeMessageTypeFilter',
  'org.thingsboard.rule.engine.filter.TbMsgTypeSwitchNode': 'ruleNodeMessageTypeSwitch',
  'org.thingsboard.rule.engine.filter.TbOriginatorTypeFilterNode': 'ruleNodeOriginatorTypeFilter',
  'org.thingsboard.rule.engine.filter.TbOriginatorTypeSwitchNode': 'ruleNodeOriginatorTypeSwitch',
  'org.thingsboard.rule.engine.metadata.TbGetAttributesNode': 'ruleNodeOriginatorAttributes',
  'org.thingsboard.rule.engine.metadata.TbGetOriginatorFieldsNode': 'ruleNodeOriginatorFields',
  'org.thingsboard.rule.engine.metadata.TbGetCustomerAttributeNode': 'ruleNodeCustomerAttributes',
  'org.thingsboard.rule.engine.metadata.TbGetDeviceAttrNode': 'ruleNodeDeviceAttributes',
  'org.thingsboard.rule.engine.metadata.TbGetRelatedAttributeNode': 'ruleNodeRelatedAttributes',
  'org.thingsboard.rule.engine.metadata.TbGetTenantAttributeNode': 'ruleNodeTenantAttributes',
  'org.thingsboard.rule.engine.transform.TbChangeOriginatorNode': 'ruleNodeChangeOriginator',
  'org.thingsboard.rule.engine.transform.TbTransformMsgNode': 'ruleNodeTransformMsg',
  'org.thingsboard.rule.engine.mail.TbMsgToEmailNode': 'ruleNodeMsgToEmail',
  'org.thingsboard.rule.engine.action.TbClearAlarmNode': 'ruleNodeClearAlarm',
  'org.thingsboard.rule.engine.action.TbCreateAlarmNode': 'ruleNodeCreateAlarm',
  'org.thingsboard.rule.engine.delay.TbMsgDelayNode': 'ruleNodeMsgDelay',
  'org.thingsboard.rule.engine.debug.TbMsgGeneratorNode': 'ruleNodeMsgGenerator',
  'org.thingsboard.rule.engine.action.TbLogNode': 'ruleNodeLog',
  'org.thingsboard.rule.engine.rpc.TbSendRPCReplyNode': 'ruleNodeRpcCallReply',
  'org.thingsboard.rule.engine.rpc.TbSendRPCRequestNode': 'ruleNodeRpcCallRequest',
  'org.thingsboard.rule.engine.telemetry.TbMsgAttributesNode': 'ruleNodeSaveAttributes',
  'org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode': 'ruleNodeSaveTimeseries',
  'tb.internal.RuleChain': 'ruleNodeRuleChain',
  'org.thingsboard.rule.engine.aws.sns.TbSnsNode': 'ruleNodeAwsSns',
  'org.thingsboard.rule.engine.aws.sqs.TbSqsNode': 'ruleNodeAwsSqs',
  'org.thingsboard.rule.engine.kafka.TbKafkaNode': 'ruleNodeKafka',
  'org.thingsboard.rule.engine.mqtt.TbMqttNode': 'ruleNodeMqtt',
  'org.thingsboard.rule.engine.rabbitmq.TbRabbitMqNode': 'ruleNodeRabbitMq',
  'org.thingsboard.rule.engine.rest.TbRestApiCallNode': 'ruleNodeRestApiCall',
  'org.thingsboard.rule.engine.mail.TbSendEmailNode': 'ruleNodeSendEmail'
};

export function getRuleNodeHelpLink(component: RuleNodeComponentDescriptor): string {
  if (component) {
    if (component.configurationDescriptor &&
      component.configurationDescriptor.nodeDefinition &&
      component.configurationDescriptor.nodeDefinition.docUrl) {
      return component.configurationDescriptor.nodeDefinition.docUrl;
    } else if (component.clazz) {
      if (ruleNodeClazzHelpLinkMap[component.clazz]) {
        return ruleNodeClazzHelpLinkMap[component.clazz];
      }
    }
  }
  return 'ruleEngine';
}
