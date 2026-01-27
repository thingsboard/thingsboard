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
    entitiesLimitExceeded: 41,
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
  [Constants.serverErrorCode.entitiesLimitExceeded, 'server-error.entities-limit-exceeded'],
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
    oauth2Google: 'https://developers.google.com/identity/protocols/oauth2',
    ruleEngine: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine-2-0/overview/`,
    ruleNodeCheckRelation: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/filter/check-relation-presence/`,
    ruleNodeCheckExistenceFields: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/filter/check-fields-presence/`,
    ruleNodeGpsGeofencingFilter: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/filter/gps-geofencing-filter/`,
    ruleNodeJsFilter: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/filter/script/`,
    ruleNodeJsSwitch: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/filter/switch/`,
    ruleNodeAssetProfileSwitch: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/filter/asset-profile-switch/`,
    ruleNodeDeviceProfileSwitch: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/filter/device-profile-switch/`,
    ruleNodeCheckAlarmStatus: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/filter/alarm-status-filter/`,
    ruleNodeMessageTypeFilter: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/filter/message-type-filter/`,
    ruleNodeMessageTypeSwitch: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/filter/message-type-switch/`,
    ruleNodeOriginatorTypeFilter: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/filter/entity-type-filter/`,
    ruleNodeOriginatorTypeSwitch: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/filter/entity-type-switch/`,
    ruleNodeOriginatorAttributes: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/enrichment/originator-attributes/`,
    ruleNodeOriginatorFields: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/enrichment/originator-fields/`,
    ruleNodeOriginatorTelemetry: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/enrichment/originator-telemetry/`,
    ruleNodeCustomerAttributes: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/enrichment/customer-attributes/`,
    ruleNodeCustomerDetails: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/enrichment/customer-details/`,
    ruleNodeFetchDeviceCredentials: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/enrichment/fetch-device-credentials/`,
    ruleNodeDeviceAttributes: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/enrichment/related-device-attributes/`,
    ruleNodeRelatedAttributes: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/enrichment/related-entity-data/`,
    ruleNodeTenantAttributes: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/enrichment/tenant-attributes/`,
    ruleNodeTenantDetails: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/enrichment/tenant-details/`,
    ruleNodeChangeOriginator: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/transformation/change-originator/`,
    ruleNodeCopyKeyValuePairs: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/transformation/copy-key-value-pairs/`,
    ruleNodeDeduplication: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/transformation/deduplication/`,
    ruleNodeDeleteKeyValuePairs: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/transformation/delete-key-value-pairs/`,
    ruleNodeJsonPath: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/transformation/json-path/`,
    ruleNodeRenameKeys: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/transformation/rename-keys/`,
    ruleNodeTransformMsg: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/transformation/script/`,
    ruleNodeSplitArrayMsg: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/transformation/split-array-msg/`,
    ruleNodeMsgToEmail: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/transformation/to-email/`,
    ruleNodeAssignToCustomer: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/assign-to-customer/`,
    ruleNodeUnassignFromCustomer: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/unassign-from-customer/`,
    ruleNodeCalculatedFields: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/calculated-fields/`,
    ruleNodeClearAlarm: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/clear-alarm/`,
    ruleNodeCreateAlarm: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/create-alarm/`,
    ruleNodeCopyToView: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/copy-to-view/`,
    ruleNodeCreateRelation: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/create-relation/`,
    ruleNodeDeleteRelation: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/delete-relation/`,
    ruleNodeDeviceState: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/device-state/`,
    ruleNodeMessageCount: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/message-count/`,
    ruleNodeMsgDelay: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/delay/`,
    ruleNodeMsgGenerator: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/generator/`,
    ruleNodeGpsGeofencingEvents: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/gps-geofencing-events/`,
    ruleNodeLog: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/log/`,
    ruleNodeRpcCallReply: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/rpc-call-reply/`,
    ruleNodeRpcCallRequest: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/rpc-call-request/`,
    ruleNodeSaveAttributes: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/save-attributes/`,
    ruleNodeDeleteAttributes: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/delete-attributes/`,
    ruleNodeSaveTimeseries: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/save-timeseries/`,
    ruleNodeSaveToCustomTable: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/save-to-custom-table/`,
    ruleNodeRuleChain: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/flow/rule-chain/`,
    ruleNodeOutputNode: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/flow/output/`,
    ruleNodeAiRequest: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/external/ai-request/`,
    ruleNodeAwsLambda: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/external/aws-lambda/`,
    ruleNodeAwsSns: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/external/aws-sns/`,
    ruleNodeAwsSqs: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/external/aws-sqs/`,
    ruleNodeKafka: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/external/kafka/`,
    ruleNodeMqtt: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/external/mqtt/`,
    ruleNodeAzureIotHub: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/external/azure-iot-hub/`,
    ruleNodeGcpPubSub: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/external/gcp-pubsub/`,
    ruleNodeRabbitMq: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/external/rabbitmq/`,
    ruleNodeRestApiCall: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/external/rest-api-call/`,
    ruleNodeSendEmail: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/external/send-email/`,
    ruleNodeSendSms: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/external/send-sms/`,
    ruleNodeMath: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/math-function/`,
    ruleNodeCalculateDelta: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/enrichment/calculate-delta/`,
    ruleNodeRestCallReply: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/rest-call-reply/`,
    ruleNodePushToCloud: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/push-to-cloud/`,
    ruleNodePushToEdge: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/push-to-edge/`,
    ruleNodeDeviceProfile: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/action/device-profile/`,
    ruleNodeAcknowledge: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/flow/acknowledge/`,
    ruleNodeCheckpoint: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/flow/checkpoint/`,
    ruleNodeSendNotification: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/external/send-notification/`,
    ruleNodeSendSlack: `${helpBaseUrl}/docs/user-guide/rule-engine-2-0/nodes/external/send-to-slack/`,
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
    jsExtension: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/contribution/ui/advanced-development`,
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
    scadaSymbolDevConnectorAnimation: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/scada/scada-symbols-dev-guide/#connectorscadasymbolanimation`,
    domains: `${helpBaseUrl}/docs${docPlatformPrefix}/domains`,
    mobileApplication: `${helpBaseUrl}/docs${docPlatformPrefix}/mobile-center/applications/`,
    mobileBundle: `${helpBaseUrl}/docs${docPlatformPrefix}/mobile-center/mobile-center/`,
    mobileQrCode: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/mobile-qr-code/`,
    calculatedField: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/calculated-fields/`,
    aiModels: `${helpBaseUrl}/docs${docPlatformPrefix}/samples/analytics/ai-models/`,
    apiKeys: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/security/api-keys/`,
    timewindowSettings: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/dashboards/#time-window`,
    trendzSettings: `${helpBaseUrl}/docs/trendz/`,
    alarmRules: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/alarm-rules/`,
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
