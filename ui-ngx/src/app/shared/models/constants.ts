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

export const httpStatusMessageMap = new Map<number, string>([
  [502, 'Server is temporarily unavailable (Bad Gateway)'],
  [503, 'Server is temporarily unavailable'],
  [504, 'Server did not respond in time (Gateway Timeout)'],
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
  'lt-xmd': 'screen and (max-width: 1599px)',
  'lt-xl': 'screen and (max-width: 1919px)',
  'gt-xs': 'screen and (min-width: 600px)',
  'gt-sm': 'screen and (min-width: 960px)',
  'gt-md': 'screen and (min-width: 1280px)',
  'gt-xmd': 'screen and (min-width: 1600px)',
  'gt-lg': 'screen and (min-width: 1920px)',
  'gt-xxl': 'screen and (min-width: 2448px)',
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

export const HelpLinks = {
  linksMap: {
    outgoingMailSettings: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/mail-settings/`,
    smsProviderSettings: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/sms-provider-settings/`,
    slackSettings: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ui/slack-settings/`,
    securitySettings: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/security/`,
    oauth2Settings: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/security/oauth-2-support/`,
    oauth2Apple: 'https://developer.apple.com/sign-in-with-apple/get-started/',
    oauth2Facebook: 'https://developers.facebook.com/docs/facebook-login/web#logindialog',
    oauth2Github: 'https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/creating-an-oauth-app',
    oauth2Google: 'https://developers.google.com/identity/protocols/oauth2',
    ruleEngine: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine/`,
    ruleNodeCheckRelation: `${helpBaseUrl}/docs/reference/rule-engine/nodes/filter/check-relation-presence/`,
    ruleNodeCheckExistenceFields: `${helpBaseUrl}/docs/reference/rule-engine/nodes/filter/check-fields-presence/`,
    ruleNodeGpsGeofencingFilter: `${helpBaseUrl}/docs/reference/rule-engine/nodes/filter/gps-geofencing-filter/`,
    ruleNodeJsFilter: `${helpBaseUrl}/docs/reference/rule-engine/nodes/filter/script/`,
    ruleNodeJsSwitch: `${helpBaseUrl}/docs/reference/rule-engine/nodes/filter/switch/`,
    ruleNodeAssetProfileSwitch: `${helpBaseUrl}/docs/reference/rule-engine/nodes/filter/asset-profile-switch/`,
    ruleNodeDeviceProfileSwitch: `${helpBaseUrl}/docs/reference/rule-engine/nodes/filter/device-profile-switch/`,
    ruleNodeCheckAlarmStatus: `${helpBaseUrl}/docs/reference/rule-engine/nodes/filter/alarm-status-filter/`,
    ruleNodeMessageTypeFilter: `${helpBaseUrl}/docs/reference/rule-engine/nodes/filter/message-type-filter/`,
    ruleNodeMessageTypeSwitch: `${helpBaseUrl}/docs/reference/rule-engine/nodes/filter/message-type-switch/`,
    ruleNodeOriginatorTypeFilter: `${helpBaseUrl}/docs/reference/rule-engine/nodes/filter/entity-type-filter/`,
    ruleNodeOriginatorTypeSwitch: `${helpBaseUrl}/docs/reference/rule-engine/nodes/filter/entity-type-switch/`,
    ruleNodeOriginatorAttributes: `${helpBaseUrl}/docs/reference/rule-engine/nodes/enrichment/originator-attributes/`,
    ruleNodeOriginatorFields: `${helpBaseUrl}/docs/reference/rule-engine/nodes/enrichment/originator-fields/`,
    ruleNodeOriginatorTelemetry: `${helpBaseUrl}/docs/reference/rule-engine/nodes/enrichment/originator-telemetry/`,
    ruleNodeCustomerAttributes: `${helpBaseUrl}/docs/reference/rule-engine/nodes/enrichment/customer-attributes/`,
    ruleNodeCustomerDetails: `${helpBaseUrl}/docs/reference/rule-engine/nodes/enrichment/customer-details/`,
    ruleNodeFetchDeviceCredentials: `${helpBaseUrl}/docs/reference/rule-engine/nodes/enrichment/fetch-device-credentials/`,
    ruleNodeDeviceAttributes: `${helpBaseUrl}/docs/reference/rule-engine/nodes/enrichment/related-device-attributes/`,
    ruleNodeRelatedAttributes: `${helpBaseUrl}/docs/reference/rule-engine/nodes/enrichment/related-entity-data/`,
    ruleNodeTenantAttributes: `${helpBaseUrl}/docs/reference/rule-engine/nodes/enrichment/tenant-attributes/`,
    ruleNodeTenantDetails: `${helpBaseUrl}/docs/reference/rule-engine/nodes/enrichment/tenant-details/`,
    ruleNodeChangeOriginator: `${helpBaseUrl}/docs/reference/rule-engine/nodes/transformation/change-originator/`,
    ruleNodeCopyKeyValuePairs: `${helpBaseUrl}/docs/reference/rule-engine/nodes/transformation/copy-key-value-pairs/`,
    ruleNodeDeduplication: `${helpBaseUrl}/docs/reference/rule-engine/nodes/transformation/deduplication/`,
    ruleNodeDeleteKeyValuePairs: `${helpBaseUrl}/docs/reference/rule-engine/nodes/transformation/delete-key-value-pairs/`,
    ruleNodeJsonPath: `${helpBaseUrl}/docs/reference/rule-engine/nodes/transformation/json-path/`,
    ruleNodeRenameKeys: `${helpBaseUrl}/docs/reference/rule-engine/nodes/transformation/rename-keys/`,
    ruleNodeTransformMsg: `${helpBaseUrl}/docs/reference/rule-engine/nodes/transformation/script/`,
    ruleNodeSplitArrayMsg: `${helpBaseUrl}/docs/reference/rule-engine/nodes/transformation/split-array-msg/`,
    ruleNodeMsgToEmail: `${helpBaseUrl}/docs/reference/rule-engine/nodes/transformation/to-email/`,
    ruleNodeAssignToCustomer: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/assign-to-customer/`,
    ruleNodeUnassignFromCustomer: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/unassign-from-customer/`,
    ruleNodeCalculatedFields: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/calculated-fields/`,
    ruleNodeClearAlarm: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/clear-alarm/`,
    ruleNodeCreateAlarm: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/create-alarm/`,
    ruleNodeCopyToView: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/copy-to-view/`,
    ruleNodeCreateRelation: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/create-relation/`,
    ruleNodeDeleteRelation: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/delete-relation/`,
    ruleNodeDeviceState: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/device-state/`,
    ruleNodeMessageCount: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/message-count/`,
    ruleNodeMsgDelay: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/delay/`,
    ruleNodeMsgGenerator: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/generator/`,
    ruleNodeGpsGeofencingEvents: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/gps-geofencing-events/`,
    ruleNodeLog: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/log/`,
    ruleNodeRpcCallReply: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/rpc-call-reply/`,
    ruleNodeRpcCallRequest: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/rpc-call-request/`,
    ruleNodeSaveAttributes: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/save-attributes/`,
    ruleNodeDeleteAttributes: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/delete-attributes/`,
    ruleNodeSaveTimeseries: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/save-timeseries/`,
    ruleNodeSaveToCustomTable: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/save-to-custom-table/`,
    ruleNodeRuleChain: `${helpBaseUrl}/docs/reference/rule-engine/nodes/flow/rule-chain/`,
    ruleNodeOutputNode: `${helpBaseUrl}/docs/reference/rule-engine/nodes/flow/output/`,
    ruleNodeAiRequest: `${helpBaseUrl}/docs/reference/rule-engine/nodes/external/ai-request/`,
    ruleNodeAwsLambda: `${helpBaseUrl}/docs/reference/rule-engine/nodes/external/aws-lambda/`,
    ruleNodeAwsSns: `${helpBaseUrl}/docs/reference/rule-engine/nodes/external/aws-sns/`,
    ruleNodeAwsSqs: `${helpBaseUrl}/docs/reference/rule-engine/nodes/external/aws-sqs/`,
    ruleNodeKafka: `${helpBaseUrl}/docs/reference/rule-engine/nodes/external/kafka/`,
    ruleNodeMqtt: `${helpBaseUrl}/docs/reference/rule-engine/nodes/external/mqtt/`,
    ruleNodeAzureIotHub: `${helpBaseUrl}/docs/reference/rule-engine/nodes/external/azure-iot-hub/`,
    ruleNodeGcpPubSub: `${helpBaseUrl}/docs/reference/rule-engine/nodes/external/gcp-pubsub/`,
    ruleNodeRabbitMq: `${helpBaseUrl}/docs/reference/rule-engine/nodes/external/rabbitmq/`,
    ruleNodeRestApiCall: `${helpBaseUrl}/docs/reference/rule-engine/nodes/external/rest-api-call/`,
    ruleNodeSendEmail: `${helpBaseUrl}/docs/reference/rule-engine/nodes/external/send-email/`,
    ruleNodeSendSms: `${helpBaseUrl}/docs/reference/rule-engine/nodes/external/send-sms/`,
    ruleNodeMath: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/math-function/`,
    ruleNodeCalculateDelta: `${helpBaseUrl}/docs/reference/rule-engine/nodes/enrichment/calculate-delta/`,
    ruleNodeRestCallReply: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/rest-call-reply/`,
    ruleNodePushToCloud: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/push-to-cloud/`,
    ruleNodePushToEdge: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/push-to-edge/`,
    ruleNodeDeviceProfile: `${helpBaseUrl}/docs/reference/rule-engine/nodes/action/device-profile/`,
    ruleNodeAcknowledge: `${helpBaseUrl}/docs/reference/rule-engine/nodes/flow/acknowledge/`,
    ruleNodeCheckpoint: `${helpBaseUrl}/docs/reference/rule-engine/nodes/flow/checkpoint/`,
    ruleNodeSendNotification: `${helpBaseUrl}/docs/reference/rule-engine/nodes/external/send-notification/`,
    ruleNodeSendSlack: `${helpBaseUrl}/docs/reference/rule-engine/nodes/external/send-to-slack/`,
    tenants: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/multi-tenancy/`,
    tenantProfiles: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/tenant-profiles/`,
    customers: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/customers/`,
    users: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/users/`,
    devices: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/devices/`,
    deviceProfiles: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/device-profiles/`,
    assetProfiles: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/asset-profiles/`,
    edges: `${helpBaseUrl}/docs/edge/why-thingsboard-edge/`,
    assets: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/assets/`,
    entityViews: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/entity-views/`,
    entitiesImport: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/provisioning/#bulk-provisioning`,
    rulechains: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/rule-engine/`,
    lwm2mResourceLibrary: `${helpBaseUrl}/docs${docPlatformPrefix}/reference/lwm2m-api/getting-started/`,
    dashboards: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/dashboards/`,
    otaUpdates: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ota-updates/`,
    widgetTypes: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/contribution/widgets-development/#widget-types`,
    widgetsBundles: `${helpBaseUrl}/docs${docPlatformPrefix}/reference/widgets/widget-library/`,
    widgetsConfig:  `${helpBaseUrl}/docs${docPlatformPrefix}/reference/widgets/widget-library/`,
    widgetsConfigTimeseries:  `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/contribution/widgets-development/time-series/`,
    widgetsConfigLatest: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/contribution/widgets-development/latest-values/`,
    widgetsConfigRpc: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/contribution/widgets-development/rpc-control/`,
    widgetsConfigAlarm: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/contribution/widgets-development/alarm-widget/`,
    widgetsConfigStatic: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/contribution/widgets-development/static-widget/`,
    queue: `${helpBaseUrl}/docs${docPlatformPrefix}/reference/architecture/queue/`,
    repositorySettings: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/version-control/#git-settings-configuration`,
    autoCommitSettings: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/version-control/#auto-commit`,
    twoFactorAuthentication: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/security/two-factor-authentication/`,
    sentNotification: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/notifications/#send-notification`,
    templateNotifications: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/notifications/#templates`,
    recipientNotifications: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/notifications/#recipients`,
    ruleNotifications: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/notifications/#rules`,
    jwtSecuritySettings: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/security/#jwt-security-settings`,
    gatewayInstall: `${helpBaseUrl}/docs/iot-gateway/installation/docker-installation/`,
    scada: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/scada/`,
    scadaSymbolDev: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/scada-symbol-dev/`,
    scadaSymbolDevAnimation: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/scada-symbol-dev/#scadasymbolanimation`,
    scadaSymbolDevConnectorAnimation: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/scada-symbol-dev/#connectorscadasymbolanimation`,
    domains: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/security/domains/`,
    mobileApplication: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/mobile-app-center/applications/`,
    mobileBundle: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/mobile-app-center/`,
    mobileQrCode: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/mobile-app-center/qr-code-widget/`,
    calculatedField: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/calculated-fields/`,
    aiModels: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/ai-models/`,
    timewindowSettings: `${helpBaseUrl}/docs${docPlatformPrefix}/user-guide/dashboards/#time-window`,
    trendzSettings: `${helpBaseUrl}/docs/trendz/`
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
