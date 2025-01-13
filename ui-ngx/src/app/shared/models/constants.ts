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

import { InjectionToken } from '@angular/core';
import { IModulesMap } from '@modules/common/modules-map.models';
import { EntityType } from '@shared/models/entity-type.models';

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
    tooManyUpdates: 34,
    passwordViolation: 45
  },
  entryPoints: {
    login: '/api/auth/login',
    tokenRefresh: '/api/auth/token',
    nonTokenBased: '/api/noauth'
  }
};

export const serverErrorCodesTranslations = new Map<number, string>([
  [Constants.serverErrorCode.general, 'server-error.general'],
  [Constants.serverErrorCode.authentication, 'server-error.authentication'],
  [Constants.serverErrorCode.jwtTokenExpired, 'server-error.jwt-token-expired'],
  [Constants.serverErrorCode.tenantTrialExpired, 'server-error.tenant-trial-expired'],
  [Constants.serverErrorCode.credentialsExpired, 'server-error.credentials-expired'],
  [Constants.serverErrorCode.permissionDenied, 'server-error.permission-denied'],
  [Constants.serverErrorCode.invalidArguments, 'server-error.invalid-arguments'],
  [Constants.serverErrorCode.badRequestParams, 'server-error.bad-request-params'],
  [Constants.serverErrorCode.itemNotFound, 'server-error.item-not-found'],
  [Constants.serverErrorCode.tooManyRequests, 'server-error.too-many-requests'],
  [Constants.serverErrorCode.tooManyUpdates, 'server-error.too-many-updates'],
]);

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
  'gt-xl': 'screen and (min-width: 5001px)',
  'md-lg': 'screen and (min-width: 960px) and (max-width: 1819px)'
};

export const resolveBreakpoint = (breakpoint: string): string => {
  if (MediaBreakpoints[breakpoint]) {
    return MediaBreakpoints[breakpoint];
  }
  return breakpoint;
};

export const helpBaseUrl = 'https://thingsboard.io';

export const docPlatformPrefix = '';

/* eslint-disable max-len */
export const HelpLinks = {
  linksMap: {
    outgoingMailSettings: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/mail-settings`,
    smsProviderSettings: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/sms-provider-settings`,
    slackSettings: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/slack-settings`,
    securitySettings: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/security-settings`,
    oauth2Settings: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/oauth-2-support/`,
    oauth2Apple: 'https://developer.apple.com/sign-in-with-apple/get-started/',
    oauth2Facebook: 'https://developers.facebook.com/docs/facebook-login/web#logindialog',
    oauth2Github: 'https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/creating-an-oauth-app',
    oauth2Google: 'https://developers.google.com/google-ads/api/docs/start',
    ruleEngine: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/overview/`,
    ruleNodeCheckRelation: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/filter-nodes/#check-relation-filter-node`,
    ruleNodeCheckExistenceFields: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/filter-nodes/#check-existence-fields-node`,
    ruleNodeGpsGeofencingFilter: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/filter-nodes/#gps-geofencing-filter-node`,
    ruleNodeJsFilter: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/filter-nodes/#script-filter-node`,
    ruleNodeJsSwitch: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/filter-nodes/#switch-node`,
    ruleNodeAssetProfileSwitch: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/filter-nodes/#asset-profile-switch`,
    ruleNodeDeviceProfileSwitch: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/filter-nodes/#device-profile-switch`,
    ruleNodeCheckAlarmStatus: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/filter-nodes/#check-alarm-status`,
    ruleNodeMessageTypeFilter: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/filter-nodes/#message-type-filter-node`,
    ruleNodeMessageTypeSwitch: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/filter-nodes/#message-type-switch-node`,
    ruleNodeOriginatorTypeFilter: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/filter-nodes/#originator-type-filter-node`,
    ruleNodeOriginatorTypeSwitch: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/filter-nodes/#originator-type-switch-node`,
    ruleNodeOriginatorAttributes: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/enrichment-nodes/#originator-attributes`,
    ruleNodeOriginatorFields: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/enrichment-nodes/#originator-fields`,
    ruleNodeOriginatorTelemetry: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/enrichment-nodes/#originator-telemetry`,
    ruleNodeCustomerAttributes: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/enrichment-nodes/#customer-attributes`,
    ruleNodeCustomerDetails: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/enrichment-nodes/#customer-details`,
    ruleNodeDeviceAttributes: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/enrichment-nodes/#device-attributes`,
    ruleNodeRelatedAttributes: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/enrichment-nodes/#related-attributes`,
    ruleNodeTenantAttributes: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/enrichment-nodes/#tenant-attributes`,
    ruleNodeTenantDetails: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/enrichment-nodes/#tenant-details`,
    ruleNodeChangeOriginator: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/transformation-nodes/#change-originator`,
    ruleNodeTransformMsg: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/transformation-nodes/#script-transformation-node`,
    ruleNodeMsgToEmail: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/transformation-nodes/#to-email-node`,
    ruleNodeAssignToCustomer: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/transformation-nodes/#assign-to-customer-node`,
    ruleNodeUnassignFromCustomer: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/transformation-nodes/#unassign-from-customer-node`,
    ruleNodeClearAlarm: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/action-nodes/#clear-alarm-node`,
    ruleNodeCreateAlarm: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/action-nodes/#create-alarm-node`,
    ruleNodeCreateRelation: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/action-nodes/#create-relation-node`,
    ruleNodeDeleteRelation: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/action-nodes/#delete-relation-node`,
    ruleNodeMsgDelay: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/action-nodes/#delay-node`,
    ruleNodeMsgGenerator: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/action-nodes/#generator-node`,
    ruleNodeGpsGeofencingEvents: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/action-nodes/#gps-geofencing-events-node`,
    ruleNodeLog: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/action-nodes/#log-node`,
    ruleNodeRpcCallReply: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/action-nodes/#rpc-call-reply-node`,
    ruleNodeRpcCallRequest: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/action-nodes/#rpc-call-request-node`,
    ruleNodeSaveAttributes: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/action-nodes/#save-attributes-node`,
    ruleNodeSaveTimeseries: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/action-nodes/#save-timeseries-node`,
    ruleNodeSaveToCustomTable: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/action-nodes/#save-to-custom-table`,
    ruleNodeRuleChain: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/flow-nodes/#rule-chain-node`,
    ruleNodeOutputNode: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/flow-nodes/#output-node`,
    ruleNodeAwsLambda: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/external-nodes/#aws-lambda-node`,
    ruleNodeAwsSns: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/external-nodes/#aws-sns-node`,
    ruleNodeAwsSqs: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/external-nodes/#aws-sqs-node`,
    ruleNodeKafka: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/external-nodes/#kafka-node`,
    ruleNodeMqtt: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/external-nodes/#mqtt-node`,
    ruleNodeAzureIotHub: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/external-nodes/#azure-iot-hub-node`,
    ruleNodeRabbitMq: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/external-nodes/#rabbitmq-node`,
    ruleNodeRestApiCall: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/external-nodes/#rest-api-call-node`,
    ruleNodeSendEmail: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/external-nodes/#send-email-node`,
    ruleNodeSendSms: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/external-nodes/#send-sms-node`,
    ruleNodeMath: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/action-nodes/#math-function-node`,
    ruleNodeCalculateDelta: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/enrichment-nodes/#calculate-delta`,
    ruleNodeRestCallReply: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/action-nodes/#rest-call-reply-node`,
    ruleNodePushToCloud: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/action-nodes/#push-to-cloud`,
    ruleNodePushToEdge: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/action-nodes/#push-to-edge`,
    ruleNodeAcknowledge: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/flow-nodes/#acknowledge-node`,
    ruleNodeCheckpoint: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/flow-nodes/#checkpoint-node`,
    ruleNodeSendNotification: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/external-nodes/#send-notification-node`,
    ruleNodeSendSlack: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/external-nodes/#send-to-slack-node`,
    tenants: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/tenants`,
    tenantProfiles: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/tenant-profiles`,
    customers: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/customers`,
    users: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/users`,
    devices: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/devices`,
    deviceProfiles: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/device-profiles`,
    assetProfiles: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/asset-profiles`,
    edges: `${helpBaseUrl}/docs/edge/getting-started-guides/what-is-edge`,
    assets: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/assets`,
    entityViews: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/entity-views`,
    entitiesImport: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/bulk-provisioning`,
    rulechains: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/rule-chains`,
    lwm2mResourceLibrary: `${helpBaseUrl}/docs${docPlatformPrefix}/reference/lwm2m-api`,
    dashboards: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/dashboards`,
    otaUpdates: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ota-updates`,
    widgetTypes: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/widget-library/#widget-types`,
    widgetsBundles: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/widget-library/#widgets-library-bundles`,
    widgetsConfig:  `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/widget-library`,
    widgetsConfigTimeseries:  `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/widget-library#time-series`,
    widgetsConfigLatest: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/widget-library#latest-values`,
    widgetsConfigRpc: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/widget-library#control-widget`,
    widgetsConfigAlarm: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/widget-library#alarm-widget`,
    widgetsConfigStatic: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/widget-library#static`,
    queue: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/queue`,
    repositorySettings: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/version-control/#git-settings-configuration`,
    autoCommitSettings: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/version-control/#auto-commit`,
    twoFactorAuthentication: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/two-factor-authentication`,
    sentNotification: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/notifications/#send-notification`,
    templateNotifications: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/notifications/#templates`,
    recipientNotifications: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/notifications/#recipients`,
    ruleNotifications: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/notifications/#rules`,
    jwtSecuritySettings: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/jwt-security-settings`,
    gatewayInstall: `${helpBaseUrl}/docs/iot-gateway/install/docker-installation`,
    scada: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/scada`,
    scadaSymbolDev: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/scada/scada-symbols-dev-guide/`,
    scadaSymbolDevAnimation: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/scada/scada-symbols-dev-guide/#scadasymbolanimation`,
    domains: `${helpBaseUrl}/docs${docPlatformPrefix}/domains`,
    mobileApplication: `${helpBaseUrl}/docs${docPlatformPrefix}/mobile-center/applications/`,
    mobileBundle: `${helpBaseUrl}/docs${docPlatformPrefix}/mobile-center/mobile-center/`,
    mobileQrCode: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/mobile-qr-code/`,
  }
};
/* eslint-enable max-len */

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

export enum DataType {
  STRING = 'STRING',
  LONG = 'LONG',
  BOOLEAN = 'BOOLEAN',
  DOUBLE = 'DOUBLE',
  JSON = 'JSON'
}

export const DataTypeTranslationMap = new Map([
  [DataType.STRING, 'value.string'],
  [DataType.LONG, 'value.integer'],
  [DataType.BOOLEAN, 'value.boolean'],
  [DataType.DOUBLE, 'value.double'],
  [DataType.JSON, 'value.json']
]);

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
        icon: 'mdi:code-json'
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

export const hidePageSizePixelValue = 550;
export const customTranslationsPrefix = 'custom.';
export const i18nPrefix = 'i18n';

export const MODULES_MAP = new InjectionToken<IModulesMap>('ModulesMap');
