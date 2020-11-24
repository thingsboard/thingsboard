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

import { InjectionToken } from '@angular/core';

export const Constants = {
  serverErrorCode: {
    general: 2,
    authentication: 10,
    jwtTokenExpired: 11,
    tenantTrialExpired: 12,
    credentialsExpired: 15,
    permissionDenied: 20,
    invalidArguments: 30,
    badRequestParams: 31,
    itemNotFound: 32,
    tooManyRequests: 33,
    tooManyUpdates: 34
  },
  entryPoints: {
    login: '/api/auth/login',
    tokenRefresh: '/api/auth/token',
    nonTokenBased: '/api/noauth'
  }
};

export const MediaBreakpoints = {
  xs: 'screen and (max-width: 599px)',
  sm: 'screen and (min-width: 600px) and (max-width: 959px)',
  md: 'screen and (min-width: 960px) and (max-width: 1279px)',
  lg: 'screen and (min-width: 1280px) and (max-width: 1919px)',
  xl: 'screen and (min-width: 1920px) and (max-width: 5000px)',
  'lt-sm': 'screen and (max-width: 599px)',
  'lt-md': 'screen and (max-width: 959px)',
  'lt-lg': 'screen and (max-width: 1279px)',
  'lt-xl': 'screen and (max-width: 1919px)',
  'gt-xs': 'screen and (min-width: 600px)',
  'gt-sm': 'screen and (min-width: 960px)',
  'gt-md': 'screen and (min-width: 1280px)',
  'gt-lg': 'screen and (min-width: 1920px)',
  'gt-xl': 'screen and (min-width: 5001px)'
};

const helpBaseUrl = 'https://thingsboard.io';

export const HelpLinks = {
  linksMap: {
    outgoingMailSettings: helpBaseUrl + '/docs/user-guide/ui/mail-settings',
    smsProviderSettings: helpBaseUrl + '/docs/user-guide/ui/sms-provider-settings',
    securitySettings: helpBaseUrl + '/docs/user-guide/ui/security-settings',
    oauth2Settings: helpBaseUrl + '/docs/user-guide/oauth-2-support/',
    ruleEngine: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/overview/',
    ruleNodeCheckRelation: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/filter-nodes/#check-relation-filter-node',
    ruleNodeCheckExistenceFields: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/filter-nodes/#check-existence-fields-node',
    ruleNodeGpsGeofencingFilter: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/filter-nodes/#gps-geofencing-filter-node',
    ruleNodeJsFilter: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/filter-nodes/#script-filter-node',
    ruleNodeJsSwitch: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/filter-nodes/#switch-node',
    ruleNodeMessageTypeFilter: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/filter-nodes/#message-type-filter-node',
    ruleNodeMessageTypeSwitch: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/filter-nodes/#message-type-switch-node',
    ruleNodeOriginatorTypeFilter: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/filter-nodes/#originator-type-filter-node',
    ruleNodeOriginatorTypeSwitch: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/filter-nodes/#originator-type-switch-node',
    ruleNodeOriginatorAttributes: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/enrichment-nodes/#originator-attributes',
    ruleNodeOriginatorFields: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/enrichment-nodes/#originator-fields',
    ruleNodeOriginatorTelemetry: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/enrichment-nodes/#originator-telemetry',
    ruleNodeCustomerAttributes: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/enrichment-nodes/#customer-attributes',
    ruleNodeCustomerDetails: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/enrichment-nodes/#customer-details',
    ruleNodeDeviceAttributes: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/enrichment-nodes/#device-attributes',
    ruleNodeRelatedAttributes: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/enrichment-nodes/#related-attributes',
    ruleNodeTenantAttributes: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/enrichment-nodes/#tenant-attributes',
    ruleNodeTenantDetails: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/enrichment-nodes/#tenant-details',
    ruleNodeChangeOriginator: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/transformation-nodes/#change-originator',
    ruleNodeTransformMsg: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/transformation-nodes/#script-transformation-node',
    ruleNodeMsgToEmail: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/transformation-nodes/#to-email-node',
    ruleNodeAssignToCustomer: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/transformation-nodes/#assign-to-customer-node',
    ruleNodeUnassignFromCustomer: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/transformation-nodes/#unassign-from-customer-node',
    ruleNodeClearAlarm: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/action-nodes/#clear-alarm-node',
    ruleNodeCreateAlarm: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/action-nodes/#create-alarm-node',
    ruleNodeCreateRelation: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/action-nodes/#create-relation-node',
    ruleNodeDeleteRelation: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/action-nodes/#delete-relation-node',
    ruleNodeMsgDelay: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/action-nodes/#delay-node',
    ruleNodeMsgGenerator: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/action-nodes/#generator-node',
    ruleNodeGpsGeofencingEvents: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/action-nodes/#gps-geofencing-events-node',
    ruleNodeLog: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/action-nodes/#log-node',
    ruleNodeRpcCallReply: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/action-nodes/#rpc-call-reply-node',
    ruleNodeRpcCallRequest: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/action-nodes/#rpc-call-request-node',
    ruleNodeSaveAttributes: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/action-nodes/#save-attributes-node',
    ruleNodeSaveTimeseries: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/action-nodes/#save-timeseries-node',
    ruleNodeSaveToCustomTable: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/action-nodes/#save-to-custom-table',
    ruleNodeRuleChain: helpBaseUrl + '/docs/user-guide/ui/rule-chains/',
    ruleNodeAwsSns: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/external-nodes/#aws-sns-node',
    ruleNodeAwsSqs: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/external-nodes/#aws-sqs-node',
    ruleNodeKafka: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/external-nodes/#kafka-node',
    ruleNodeMqtt: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/external-nodes/#mqtt-node',
    ruleNodeAzureIotHub: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/external-nodes/#azure-iot-hub-node',
    ruleNodeRabbitMq: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/external-nodes/#rabbitmq-node',
    ruleNodeRestApiCall: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/external-nodes/#rest-api-call-node',
    ruleNodeSendEmail: helpBaseUrl + '/docs/user-guide/rule-engine-2-0/external-nodes/#send-email-node',
    tenants: helpBaseUrl + '/docs/user-guide/ui/tenants',
    tenantProfiles: helpBaseUrl + '/docs/user-guide/ui/tenant-profiles',
    customers: helpBaseUrl + '/docs/user-guide/ui/customers',
    users: helpBaseUrl + '/docs/user-guide/ui/users',
    devices: helpBaseUrl + '/docs/user-guide/ui/devices',
    deviceProfiles: helpBaseUrl + '/docs/user-guide/ui/device-profiles',
    assets: helpBaseUrl + '/docs/user-guide/ui/assets',
    entityViews: helpBaseUrl + '/docs/user-guide/ui/entity-views',
    entitiesImport: helpBaseUrl + '/docs/user-guide/bulk-provisioning',
    rulechains: helpBaseUrl + '/docs/user-guide/ui/rule-chains',
    dashboards: helpBaseUrl + '/docs/user-guide/ui/dashboards',
    widgetsBundles: helpBaseUrl + '/docs/user-guide/ui/widget-library#bundles',
    widgetsConfig:  helpBaseUrl + '/docs/user-guide/ui/dashboards#widget-configuration',
    widgetsConfigTimeseries:  helpBaseUrl + '/docs/user-guide/ui/dashboards#timeseries',
    widgetsConfigLatest: helpBaseUrl +  '/docs/user-guide/ui/dashboards#latest',
    widgetsConfigRpc: helpBaseUrl +  '/docs/user-guide/ui/dashboards#rpc',
    widgetsConfigAlarm: helpBaseUrl +  '/docs/user-guide/ui/dashboards#alarm',
    widgetsConfigStatic: helpBaseUrl +  '/docs/user-guide/ui/dashboards#static',
  }
};

export interface ValueTypeData {
  name: string;
  icon: string;
}

export enum ValueType {
  STRING = 'STRING',
  INTEGER = 'INTEGER',
  DOUBLE = 'DOUBLE',
  BOOLEAN = 'BOOLEAN',
  JSON = 'JSON'
}

export const valueTypesMap = new Map<ValueType, ValueTypeData>(
  [
    [
      ValueType.STRING,
      {
        name: 'value.string',
        icon: 'mdi:format-text'
      }
    ],
    [
      ValueType.INTEGER,
      {
        name: 'value.integer',
        icon: 'mdi:numeric'
      }
    ],
    [
      ValueType.DOUBLE,
      {
        name: 'value.double',
        icon: 'mdi:numeric'
      }
    ],
    [
      ValueType.BOOLEAN,
      {
        name: 'value.boolean',
        icon: 'mdi:checkbox-marked-outline'
      }
    ],
    [
      ValueType.JSON,
      {
        name: 'value.json',
        icon: 'mdi:json'
      }
    ]
  ]
);

export interface ContentTypeData {
  name: string;
  code: string;
}

export enum ContentType {
  JSON = 'JSON',
  TEXT = 'TEXT',
  BINARY = 'BINARY'
}

export const contentTypesMap = new Map<ContentType, ContentTypeData>(
  [
    [
      ContentType.JSON,
      {
        name: 'content-type.json',
        code: 'json'
      }
    ],
    [
      ContentType.TEXT,
      {
        name: 'content-type.text',
        code: 'text'
      }
    ],
    [
      ContentType.BINARY,
      {
        name: 'content-type.binary',
        code: 'text'
      }
    ]
  ]
);

export const customTranslationsPrefix = 'custom.';

export const MODULES_MAP = new InjectionToken<{[key: string]: any}>('ModulesMap');
