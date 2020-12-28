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
import { RuleChainId } from '@shared/models/id/rule-chain-id';
import { RuleNodeId } from '@shared/models/id/rule-node-id';
import { ComponentDescriptor } from '@shared/models/component-descriptor.models';
import { FcEdge, FcNode } from 'ngx-flowchart/dist/ngx-flowchart';
import { Observable } from 'rxjs';
import { PageComponent } from '@shared/components/page.component';
import { AfterViewInit, EventEmitter, Inject, OnInit, Directive } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AbstractControl, FormGroup } from '@angular/forms';

export interface RuleNodeConfiguration {
  [key: string]: any;
}

export interface RuleNode extends BaseData<RuleNodeId> {
  ruleChainId?: RuleChainId;
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

export interface RuleNodeDefinition {
  description: string;
  details: string;
  inEnabled: boolean;
  outEnabled: boolean;
  relationTypes: string[];
  customRelations: boolean;
  defaultConfiguration: RuleNodeConfiguration;
  icon?: string;
  iconUrl?: string;
  docUrl?: string;
  uiResources?: string[];
  uiResourceLoadError?: string;
  configDirective?: string;
}

export interface RuleNodeConfigurationDescriptor {
  nodeDefinition: RuleNodeDefinition;
}

export interface IRuleNodeConfigurationComponent {
  ruleNodeId: string;
  configuration: RuleNodeConfiguration;
  configurationChanged: Observable<RuleNodeConfiguration>;
  validate();
  [key: string]: any;
}

@Directive()
export abstract class RuleNodeConfigurationComponent extends PageComponent implements
  IRuleNodeConfigurationComponent, OnInit, AfterViewInit {

  ruleNodeId: string;

  configurationValue: RuleNodeConfiguration;

  private configurationSet = false;

  set configuration(value: RuleNodeConfiguration) {
    this.configurationValue = value;
    if (!this.configurationSet) {
      this.configurationSet = true;
      this.setupConfiguration(value);
    } else {
      this.updateConfiguration(value);
    }
  }

  get configuration(): RuleNodeConfiguration {
    return this.configurationValue;
  }

  configurationChangedEmiter = new EventEmitter<RuleNodeConfiguration>();
  configurationChanged = this.configurationChangedEmiter.asObservable();

  protected constructor(@Inject(Store) protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {}

  ngAfterViewInit(): void {
    setTimeout(() => {
      if (!this.validateConfig()) {
        this.configurationChangedEmiter.emit(null);
      }
    }, 0);
  }

  validate() {
    this.onValidate();
  }

  protected setupConfiguration(configuration: RuleNodeConfiguration) {
    this.onConfigurationSet(this.prepareInputConfig(configuration));
    this.updateValidators(false);
    for (const trigger of this.validatorTriggers()) {
      const path = trigger.split('.');
      let control: AbstractControl = this.configForm();
      for (const part of path) {
        control = control.get(part);
      }
      control.valueChanges.subscribe(() => {
        this.updateValidators(true, trigger);
      });
    }
    this.configForm().valueChanges.subscribe((updated: RuleNodeConfiguration) => {
      this.onConfigurationChanged(updated);
    });
  }

  protected updateConfiguration(configuration: RuleNodeConfiguration) {
    this.configForm().reset(this.prepareInputConfig(configuration), {emitEvent: false});
    this.updateValidators(false);
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
  }

  protected validatorTriggers(): string[] {
    return [];
  }

  protected onConfigurationChanged(updated: RuleNodeConfiguration) {
    this.configurationValue = updated;
    if (this.validateConfig()) {
      this.configurationChangedEmiter.emit(this.prepareOutputConfig(updated));
    } else {
      this.configurationChangedEmiter.emit(null);
    }
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return configuration;
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return configuration;
  }

  protected validateConfig(): boolean {
    return this.configForm().valid;
  }

  protected onValidate() {}

  protected abstract configForm(): FormGroup;

  protected abstract onConfigurationSet(configuration: RuleNodeConfiguration);

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

export interface FcRuleNodeType extends FcNode {
  component?: RuleNodeComponentDescriptor;
  nodeClass?: string;
  icon?: string;
  iconUrl?: string;
}

export interface FcRuleNode extends FcRuleNodeType {
  ruleNodeId?: RuleNodeId;
  additionalInfo?: any;
  configuration?: RuleNodeConfiguration;
  debugMode?: boolean;
  targetRuleChainId?: string;
  error?: string;
  highlighted?: boolean;
  componentClazz?: string;
}

export interface FcRuleEdge extends FcEdge {
  labels?: string[];
}

export interface TestScriptInputParams {
  script: string;
  scriptType: string;
  argNames: string[];
  msg: string;
  metadata: {[key: string]: string};
  msgType: string;
}

export interface TestScriptResult {
  output: string;
  error: string;
}

export enum MessageType {
  POST_ATTRIBUTES_REQUEST = 'POST_ATTRIBUTES_REQUEST',
  POST_TELEMETRY_REQUEST = 'POST_TELEMETRY_REQUEST',
  TO_SERVER_RPC_REQUEST = 'TO_SERVER_RPC_REQUEST',
  RPC_CALL_FROM_SERVER_TO_DEVICE = 'RPC_CALL_FROM_SERVER_TO_DEVICE',
  ACTIVITY_EVENT = 'ACTIVITY_EVENT',
  INACTIVITY_EVENT = 'INACTIVITY_EVENT',
  CONNECT_EVENT = 'CONNECT_EVENT',
  DISCONNECT_EVENT = 'DISCONNECT_EVENT',
  ENTITY_CREATED = 'ENTITY_CREATED',
  ENTITY_UPDATED = 'ENTITY_UPDATED',
  ENTITY_DELETED = 'ENTITY_DELETED',
  ENTITY_ASSIGNED = 'ENTITY_ASSIGNED',
  ENTITY_UNASSIGNED = 'ENTITY_UNASSIGNED',
  ATTRIBUTES_UPDATED = 'ATTRIBUTES_UPDATED',
  ATTRIBUTES_DELETED = 'ATTRIBUTES_DELETED',
  TIMESERIES_UPDATED = 'TIMESERIES_UPDATED'
}

export const messageTypeNames = new Map<MessageType, string>(
  [
    [MessageType.POST_ATTRIBUTES_REQUEST, 'Post attributes'],
    [MessageType.POST_TELEMETRY_REQUEST, 'Post telemetry'],
    [MessageType.TO_SERVER_RPC_REQUEST, 'RPC Request from Device'],
    [MessageType.RPC_CALL_FROM_SERVER_TO_DEVICE, 'RPC Request to Device'],
    [MessageType.ACTIVITY_EVENT, 'Activity Event'],
    [MessageType.INACTIVITY_EVENT, 'Inactivity Event'],
    [MessageType.CONNECT_EVENT, 'Connect Event'],
    [MessageType.DISCONNECT_EVENT, 'Disconnect Event'],
    [MessageType.ENTITY_CREATED, 'Entity Created'],
    [MessageType.ENTITY_UPDATED, 'Entity Updated'],
    [MessageType.ENTITY_DELETED, 'Entity Deleted'],
    [MessageType.ENTITY_ASSIGNED, 'Entity Assigned'],
    [MessageType.ENTITY_UNASSIGNED, 'Entity Unassigned'],
    [MessageType.ATTRIBUTES_UPDATED, 'Attributes Updated'],
    [MessageType.ATTRIBUTES_DELETED, 'Attributes Deleted'],
    [MessageType.TIMESERIES_UPDATED, 'Timeseries Updated']
  ]
);

const ruleNodeClazzHelpLinkMap = {
  'org.thingsboard.rule.engine.filter.TbCheckRelationNode': 'ruleNodeCheckRelation',
  'org.thingsboard.rule.engine.filter.TbCheckMessageNode': 'ruleNodeCheckExistenceFields',
  'org.thingsboard.rule.engine.geo.TbGpsGeofencingFilterNode': 'ruleNodeGpsGeofencingFilter',
  'org.thingsboard.rule.engine.filter.TbJsFilterNode': 'ruleNodeJsFilter',
  'org.thingsboard.rule.engine.filter.TbJsSwitchNode': 'ruleNodeJsSwitch',
  'org.thingsboard.rule.engine.filter.TbMsgTypeFilterNode': 'ruleNodeMessageTypeFilter',
  'org.thingsboard.rule.engine.filter.TbMsgTypeSwitchNode': 'ruleNodeMessageTypeSwitch',
  'org.thingsboard.rule.engine.filter.TbOriginatorTypeFilterNode': 'ruleNodeOriginatorTypeFilter',
  'org.thingsboard.rule.engine.filter.TbOriginatorTypeSwitchNode': 'ruleNodeOriginatorTypeSwitch',
  'org.thingsboard.rule.engine.metadata.TbGetAttributesNode': 'ruleNodeOriginatorAttributes',
  'org.thingsboard.rule.engine.metadata.TbGetOriginatorFieldsNode': 'ruleNodeOriginatorFields',
  'org.thingsboard.rule.engine.metadata.TbGetTelemetryNode': 'ruleNodeOriginatorTelemetry',
  'org.thingsboard.rule.engine.metadata.TbGetCustomerAttributeNode': 'ruleNodeCustomerAttributes',
  'org.thingsboard.rule.engine.metadata.TbGetCustomerDetailsNode': 'ruleNodeCustomerDetails',
  'org.thingsboard.rule.engine.metadata.TbGetDeviceAttrNode': 'ruleNodeDeviceAttributes',
  'org.thingsboard.rule.engine.metadata.TbGetRelatedAttributeNode': 'ruleNodeRelatedAttributes',
  'org.thingsboard.rule.engine.metadata.TbGetTenantAttributeNode': 'ruleNodeTenantAttributes',
  'org.thingsboard.rule.engine.metadata.TbGetTenantDetailsNode': 'ruleNodeTenantDetails',
  'org.thingsboard.rule.engine.transform.TbChangeOriginatorNode': 'ruleNodeChangeOriginator',
  'org.thingsboard.rule.engine.transform.TbTransformMsgNode': 'ruleNodeTransformMsg',
  'org.thingsboard.rule.engine.mail.TbMsgToEmailNode': 'ruleNodeMsgToEmail',
  'org.thingsboard.rule.engine.action.TbAssignToCustomerNode': 'ruleNodeAssignToCustomer',
  'org.thingsboard.rule.engine.action.TbUnassignFromCustomerNode': 'ruleNodeUnassignFromCustomer',
  'org.thingsboard.rule.engine.action.TbClearAlarmNode': 'ruleNodeClearAlarm',
  'org.thingsboard.rule.engine.action.TbCreateAlarmNode': 'ruleNodeCreateAlarm',
  'org.thingsboard.rule.engine.action.TbCreateRelationNode': 'ruleNodeCreateRelation',
  'org.thingsboard.rule.engine.action.TbDeleteRelationNode': 'ruleNodeDeleteRelation',
  'org.thingsboard.rule.engine.delay.TbMsgDelayNode': 'ruleNodeMsgDelay',
  'org.thingsboard.rule.engine.debug.TbMsgGeneratorNode': 'ruleNodeMsgGenerator',
  'org.thingsboard.rule.engine.geo.TbGpsGeofencingActionNode': 'ruleNodeGpsGeofencingEvents',
  'org.thingsboard.rule.engine.action.TbLogNode': 'ruleNodeLog',
  'org.thingsboard.rule.engine.rpc.TbSendRPCReplyNode': 'ruleNodeRpcCallReply',
  'org.thingsboard.rule.engine.rpc.TbSendRPCRequestNode': 'ruleNodeRpcCallRequest',
  'org.thingsboard.rule.engine.telemetry.TbMsgAttributesNode': 'ruleNodeSaveAttributes',
  'org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode': 'ruleNodeSaveTimeseries',
  'org.thingsboard.rule.engine.action.TbSaveToCustomCassandraTableNode': 'ruleNodeSaveToCustomTable',
  'tb.internal.RuleChain': 'ruleNodeRuleChain',
  'org.thingsboard.rule.engine.aws.sns.TbSnsNode': 'ruleNodeAwsSns',
  'org.thingsboard.rule.engine.aws.sqs.TbSqsNode': 'ruleNodeAwsSqs',
  'org.thingsboard.rule.engine.kafka.TbKafkaNode': 'ruleNodeKafka',
  'org.thingsboard.rule.engine.mqtt.TbMqttNode': 'ruleNodeMqtt',
  'org.thingsboard.rule.engine.mqtt.azure.TbAzureIotHubNode': 'ruleNodeAzureIotHub',
  'org.thingsboard.rule.engine.rabbitmq.TbRabbitMqNode': 'ruleNodeRabbitMq',
  'org.thingsboard.rule.engine.rest.TbRestApiCallNode': 'ruleNodeRestApiCall',
  'org.thingsboard.rule.engine.mail.TbSendEmailNode': 'ruleNodeSendEmail',
  'org.thingsboard.rule.engine.sms.TbSendSmsNode': 'ruleNodeSendSms'
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
