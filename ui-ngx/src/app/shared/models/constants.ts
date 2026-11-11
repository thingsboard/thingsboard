// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { InjectionToken } from '@angular/core';
import { IModulesMap } from '@modules/common/modules-map.models';

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
    subscriptionViolation: 40,
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

export const docPlatformPrefix = '/pe';
const docIntegrationPrefix = docPlatformPrefix;

const docsBase = `${helpBaseUrl}/docs${docPlatformPrefix}`;
const ruleNodesBase = `${docsBase}/reference/rule-engine/nodes`;
const integrationsBase = `${helpBaseUrl}/docs${docIntegrationPrefix}/user-guide/integrations`;

const ruleNodeAggregateLatestDoc = `${ruleNodesBase}/analytics/aggregate-latest/`;
const ruleNodeAlarmsCountDoc = `${ruleNodesBase}/analytics/alarms-count/`;

export const HelpLinks = {
  linksMap: {
    docs: `${docsBase}`,
    outgoingMailSettings: `${docsBase}/user-guide/ui/mail-settings/`,
    mailTemplates: `${docsBase}/user-guide/white-labeling-mail/`,
    smsProviderSettings: `${docsBase}/user-guide/ui/sms-provider-settings/`,
    slackSettings: `${docsBase}/user-guide/ui/slack-settings/`,
    securitySettings: `${docsBase}/user-guide/security/`,
    oauth2Settings: `${docsBase}/user-guide/security/oauth-2-support/`,
    oauth2Apple: 'https://developer.apple.com/sign-in-with-apple/get-started/',
    oauth2Facebook: 'https://developers.facebook.com/docs/facebook-login/web#logindialog',
    oauth2Github: 'https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/creating-an-oauth-app',
    oauth2Google: 'https://developers.google.com/identity/protocols/oauth2',
    ruleEngine: `${docsBase}/user-guide/rule-engine/`,
    ruleNodeCheckRelation: `${ruleNodesBase}/filter/check-relation-presence/`,
    ruleNodeCheckExistenceFields: `${ruleNodesBase}/filter/check-fields-presence/`,
    ruleNodeGpsGeofencingFilter: `${ruleNodesBase}/filter/gps-geofencing-filter/`,
    ruleNodeJsFilter: `${ruleNodesBase}/filter/script/`,
    ruleNodeJsSwitch: `${ruleNodesBase}/filter/switch/`,
    ruleNodeAssetProfileSwitch: `${ruleNodesBase}/filter/asset-profile-switch/`,
    ruleNodeDeviceProfileSwitch: `${ruleNodesBase}/filter/device-profile-switch/`,
    ruleNodeCheckAlarmStatus: `${ruleNodesBase}/filter/alarm-status-filter/`,
    ruleNodeMessageTypeFilter: `${ruleNodesBase}/filter/message-type-filter/`,
    ruleNodeMessageTypeSwitch: `${ruleNodesBase}/filter/message-type-switch/`,
    ruleNodeOriginatorTypeFilter: `${ruleNodesBase}/filter/entity-type-filter/`,
    ruleNodeOriginatorTypeSwitch: `${ruleNodesBase}/filter/entity-type-switch/`,
    ruleNodeOriginatorAttributes: `${ruleNodesBase}/enrichment/originator-attributes/`,
    ruleNodeOriginatorFields: `${ruleNodesBase}/enrichment/originator-fields/`,
    ruleNodeOriginatorTelemetry: `${ruleNodesBase}/enrichment/originator-telemetry/`,
    ruleNodeCustomerAttributes: `${ruleNodesBase}/enrichment/customer-attributes/`,
    ruleNodeCustomerDetails: `${ruleNodesBase}/enrichment/customer-details/`,
    ruleNodeFetchDeviceCredentials: `${ruleNodesBase}/enrichment/fetch-device-credentials/`,
    ruleNodeDeviceAttributes: `${ruleNodesBase}/enrichment/related-device-attributes/`,
    ruleNodeRelatedAttributes: `${ruleNodesBase}/enrichment/related-entity-data/`,
    ruleNodeTenantAttributes: `${ruleNodesBase}/enrichment/tenant-attributes/`,
    ruleNodeTenantDetails: `${ruleNodesBase}/enrichment/tenant-details/`,
    ruleNodeChangeOriginator: `${ruleNodesBase}/transformation/change-originator/`,
    ruleNodeCopyKeyValuePairs: `${ruleNodesBase}/transformation/copy-key-value-pairs/`,
    ruleNodeDeduplication: `${ruleNodesBase}/transformation/deduplication/`,
    ruleNodeDeleteKeyValuePairs: `${ruleNodesBase}/transformation/delete-key-value-pairs/`,
    ruleNodeJsonPath: `${ruleNodesBase}/transformation/json-path/`,
    ruleNodeRenameKeys: `${ruleNodesBase}/transformation/rename-keys/`,
    ruleNodeTransformMsg: `${ruleNodesBase}/transformation/script/`,
    ruleNodeSplitArrayMsg: `${ruleNodesBase}/transformation/split-array-msg/`,
    ruleNodeMsgToEmail: `${ruleNodesBase}/transformation/to-email/`,
    ruleNodeAssignToCustomer: `${ruleNodesBase}/action/assign-to-customer/`,
    ruleNodeUnassignFromCustomer: `${ruleNodesBase}/action/unassign-from-customer/`,
    ruleNodeCalculatedFields: `${ruleNodesBase}/action/calculated-fields/`,
    ruleNodeClearAlarm: `${ruleNodesBase}/action/clear-alarm/`,
    ruleNodeCreateAlarm: `${ruleNodesBase}/action/create-alarm/`,
    ruleNodeCopyToView: `${ruleNodesBase}/action/copy-to-view/`,
    ruleNodeCreateRelation: `${ruleNodesBase}/action/create-relation/`,
    ruleNodeDeleteRelation: `${ruleNodesBase}/action/delete-relation/`,
    ruleNodeDeviceState: `${ruleNodesBase}/action/device-state/`,
    ruleNodeMessageCount: `${ruleNodesBase}/action/message-count/`,
    ruleNodeMsgDelay: `${ruleNodesBase}/action/delay/`,
    ruleNodeMsgGenerator: `${ruleNodesBase}/action/generator/`,
    ruleNodeGpsGeofencingEvents: `${ruleNodesBase}/action/gps-geofencing-events/`,
    ruleNodeLog: `${ruleNodesBase}/action/log/`,
    ruleNodeRpcCallReply: `${ruleNodesBase}/action/rpc-call-reply/`,
    ruleNodeRpcCallRequest: `${ruleNodesBase}/action/rpc-call-request/`,
    ruleNodeSaveAttributes: `${ruleNodesBase}/action/save-attributes/`,
    ruleNodeDeleteAttributes: `${ruleNodesBase}/action/delete-attributes/`,
    ruleNodeSaveTimeseries: `${ruleNodesBase}/action/save-timeseries/`,
    ruleNodeSaveToCustomTable: `${ruleNodesBase}/action/save-to-custom-table/`,
    ruleNodeRuleChain: `${ruleNodesBase}/flow/rule-chain/`,
    ruleNodeOutputNode: `${ruleNodesBase}/flow/output/`,
    ruleNodeAiRequest: `${ruleNodesBase}/external/ai-request/`,
    ruleNodeAwsLambda: `${ruleNodesBase}/external/aws-lambda/`,
    ruleNodeAwsSns: `${ruleNodesBase}/external/aws-sns/`,
    ruleNodeAwsSqs: `${ruleNodesBase}/external/aws-sqs/`,
    ruleNodeKafka: `${ruleNodesBase}/external/kafka/`,
    ruleNodeMqtt: `${ruleNodesBase}/external/mqtt/`,
    ruleNodeAzureIotHub: `${ruleNodesBase}/external/azure-iot-hub/`,
    ruleNodeGcpPubSub: `${ruleNodesBase}/external/gcp-pubsub/`,
    ruleNodeRabbitMq: `${ruleNodesBase}/external/rabbitmq/`,
    ruleNodeRestApiCall: `${ruleNodesBase}/external/rest-api-call/`,
    ruleNodeSendEmail: `${ruleNodesBase}/external/send-email/`,
    ruleNodeSendSms: `${ruleNodesBase}/external/send-sms/`,
    ruleNodeMath: `${ruleNodesBase}/action/math-function/`,
    ruleNodeCalculateDelta: `${ruleNodesBase}/enrichment/calculate-delta/`,
    ruleNodeRestCallReply: `${ruleNodesBase}/action/rest-call-reply/`,
    ruleNodePushToCloud: `${ruleNodesBase}/action/push-to-cloud/`,
    ruleNodePushToEdge: `${ruleNodesBase}/action/push-to-edge/`,
    ruleNodeDeviceProfile: `${ruleNodesBase}/action/device-profile/`, //CE END
    ruleNodeIntegrationDownlink: `${ruleNodesBase}/action/integration-downlink/`,
    ruleNodeAddToGroup: `${ruleNodesBase}/action/add-to-group/`,
    ruleNodeRemoveFromGroup: `${ruleNodesBase}/action/remove-from-group/`,
    ruleNodeDuplicateToGroup: `${ruleNodesBase}/transformation/duplicate-to-group/`,
    ruleNodeDuplicateToGroupByName: `${ruleNodesBase}/transformation/duplicate-to-group-by-name/`,
    ruleNodeDuplicateToRelated: `${ruleNodesBase}/transformation/duplicate-to-related/`,
    ruleNodeChangeOwner: `${ruleNodesBase}/action/change-owner/`,
    ruleNodeGenerateReport: `${ruleNodesBase}/action/generate-report/`,
    ruleNodeGenerateDashboardReport: `${ruleNodesBase}/action/generate-dashboard-report/`,
    ruleNodeAggregateLatest: ruleNodeAggregateLatestDoc,
    ruleNodeAggregateLatestDeprecated: ruleNodeAggregateLatestDoc,
    ruleNodeAggregateStream: `${ruleNodesBase}/analytics/aggregate-stream/`,
    ruleNodeAlarmsCount: ruleNodeAlarmsCountDoc,
    ruleNodeAlarmsCountDeprecated: ruleNodeAlarmsCountDoc,
    ruleNodeAcknowledge: `${ruleNodesBase}/flow/acknowledge/`,
    ruleNodeCheckpoint: `${ruleNodesBase}/flow/checkpoint/`,
    ruleNodeSendNotification: `${ruleNodesBase}/external/send-notification/`,
    ruleNodeSendSlack: `${ruleNodesBase}/external/send-to-slack/`,
    ruleNodeTwilioSms: `${ruleNodesBase}/external/twilio-sms/`,
    ruleNodeTwilioVoice: `${ruleNodesBase}/external/twilio-voice/`,
    tenants: `${docsBase}/user-guide/multi-tenancy/`,
    tenantProfiles: `${docsBase}/user-guide/tenant-profiles/`,
    customers: `${docsBase}/user-guide/customers/`,
    users: `${docsBase}/user-guide/users/`,
    devices: `${docsBase}/user-guide/devices/`,
    deviceProfiles: `${docsBase}/user-guide/device-profiles/`,
    assetProfiles: `${docsBase}/user-guide/asset-profiles/`,
    edges: `${helpBaseUrl}/docs/edge${docPlatformPrefix}/why-thingsboard-edge/`,
    agents: `${docsBase}/user-guide/agents/`,
    agentInstall: `${docsBase}/user-guide/agents/installation/`,
    agentSelfUpgrade: `${docsBase}/user-guide/agents/self-upgrade/`,
    agentProfiles: `${docsBase}/user-guide/agents/agent-profiles/`,
    agentProfileAutoProvision: `${docsBase}/user-guide/agents/agent-profiles/#auto-provision-an-agent`,
    agentProfileAssignAppProfiles: `${docsBase}/user-guide/agents/agent-profiles/#assign-application-profiles`,
    agentProfileProvisionCommand: `${docsBase}/user-guide/agents/agent-profiles/#provisioning-command-and-profile-details`,
    agentApplications: `${docsBase}/user-guide/agents/applications/`,
    agentAppInstall: `${docsBase}/user-guide/agents/applications/#deploy-an-application`,
    agentAppEvents: `${docsBase}/user-guide/agents/applications/#events`,
    agentAppLogs: `${docsBase}/user-guide/agents/applications/#view-container-logs`,
    agentAppProfiles: `${docsBase}/user-guide/agents/application-profiles/`,
    agentAppProfileUse: `${docsBase}/user-guide/agents/application-profiles/#use-a-profile`,
    agentAppProfileVersions: `${docsBase}/user-guide/agents/application-profiles/#profile-versions-and-upgrades`,
    agentAppUpdate: `${docsBase}/user-guide/agents/application-actions/#update-an-application`,
    agentAppUpgrade: `${docsBase}/user-guide/agents/application-actions/#upgrade-an-application`,
    agentAppDelete: `${docsBase}/user-guide/agents/application-actions/#delete-an-application`,
    agentBulkActions: `${docsBase}/user-guide/agents/bulk-actions/`,
    agentBulkActionExecutions: `${docsBase}/user-guide/agents/bulk-actions/#monitor-the-executions`,
    agentEdgeInstall: `${helpBaseUrl}/docs/edge${docPlatformPrefix}/installation/agent/`,
    agentGatewayInstall: `${helpBaseUrl}/docs/iot-gateway/installation/agent-installation/`,
    assets: `${docsBase}/user-guide/assets/`,
    entityViews: `${docsBase}/user-guide/entity-views/`,
    entitiesImport: `${docsBase}/user-guide/provisioning/#bulk-provisioning`,
    rulechains: `${docsBase}/user-guide/rule-engine/`,
    lwm2mResourceLibrary: `${docsBase}/reference/lwm2m-api/getting-started/`,
    jsExtension: `${docsBase}/user-guide/contribution/ui/advanced-development`,
    dashboards: `${docsBase}/user-guide/dashboards/`,
    otaUpdates: `${docsBase}/user-guide/ota-updates/`,
    widgetTypes: `${docsBase}/user-guide/contribution/widgets-development/#widget-types`,
    widgetsBundles: `${docsBase}/reference/widgets/widget-library/`,
    widgetsConfig:  `${docsBase}/reference/widgets/widget-library/`,
    widgetsConfigTimeseries:  `${docsBase}/user-guide/contribution/widgets-development/time-series/`,
    widgetsConfigLatest: `${docsBase}/user-guide/contribution/widgets-development/latest-values/`,
    widgetsConfigRpc: `${docsBase}/user-guide/contribution/widgets-development/rpc-control/`,
    widgetsConfigAlarm: `${docsBase}/user-guide/contribution/widgets-development/alarm-widget/`,
    widgetsConfigStatic: `${docsBase}/user-guide/contribution/widgets-development/static-widget/`,
    queue: `${docsBase}/reference/architecture/queue/`,
    repositorySettings: `${docsBase}/user-guide/version-control/#git-settings-configuration`,
    autoCommitSettings: `${docsBase}/user-guide/version-control/#auto-commit`,
    twoFactorAuthentication: `${docsBase}/user-guide/security/two-factor-authentication/`,
    sentNotification: `${docsBase}/user-guide/notifications/#send-notification`,
    templateNotifications: `${docsBase}/user-guide/notifications/#templates`,
    recipientNotifications: `${docsBase}/user-guide/notifications/#recipients`,
    ruleNotifications: `${docsBase}/user-guide/notifications/#rules`,
    jwtSecuritySettings: `${docsBase}/user-guide/security/#jwt-security-settings`,
    gatewayInstall: `${helpBaseUrl}/docs/iot-gateway/installation/docker-installation/`,
    scada: `${docsBase}/user-guide/scada/`,
    scadaSymbolDev: `${docsBase}/user-guide/scada-symbol-dev/`,
    scadaSymbolDevAnimation: `${docsBase}/user-guide/scada-symbol-dev/#scadasymbolanimation`,
    scadaSymbolDevConnectorAnimation: `${docsBase}/user-guide/scada-symbol-dev/#connectorscadasymbolanimation`,
    domains: `${docsBase}/user-guide/security/domains/`,
    mobileApplication: `${docsBase}/user-guide/mobile-app-center/applications/`,
    mobileBundle: `${docsBase}/user-guide/mobile-app-center/`,
    mobileQrCode: `${docsBase}/user-guide/mobile-app-center/qr-code-widget/`,
    calculatedField: `${docsBase}/user-guide/calculated-fields/`,
    aiModels: `${docsBase}/user-guide/ai-models/`,
    apiKeys: `${docsBase}/user-guide/security/api-keys/`,
    timewindowSettings: `${docsBase}/user-guide/dashboards/#time-window`,
    converters: `${integrationsBase}/`,
    uplinkConverters: `${integrationsBase}/uplink-data-converter`,
    downlinkConverters: `${integrationsBase}/downlink-data-converter`,
    integrations: `${integrationsBase}`,
    integrationHttp: `${integrationsBase}/http`,
    integrationOceanConnect: `${integrationsBase}/integration-types/`,
    integrationSigFox: `${integrationsBase}/sigfox`,
    integrationThingPark: `${integrationsBase}/thingpark`,
    integrationThingParkEnterprise: `${integrationsBase}/thingparkenterprise/`,
    integrationTMobileIotCdp: `${integrationsBase}/iotcreators/`,
    integrationLoriot: `${integrationsBase}/loriot`,
    integrationParticle: `${integrationsBase}/particle`,
    integrationMqtt: `${integrationsBase}/mqtt`,
    integrationAwsIoT: `${integrationsBase}/aws-iot`,
    integrationAwsSQS: `${integrationsBase}/aws-sqs`,
    integrationAwsKinesis:  `${integrationsBase}/aws-kinesis`,
    integrationTheThingsNetwork: `${integrationsBase}/ttn`,
    integrationTheThingsIndustries: `${integrationsBase}/tti`,
    integrationChirpStack: `${integrationsBase}/chirpstack`,
    integrationAzureEventHub: `${integrationsBase}/azure-event-hub`,
    integrationAzureIoTHub: `${integrationsBase}/azure-iot-hub`,
    integrationAzureServiceBus: `${integrationsBase}/azure-service-bus`,
    integrationOpcUa:  `${integrationsBase}/opc-ua`,
    integrationUdp:  `${integrationsBase}/udp`,
    integrationTcp:  `${integrationsBase}/tcp`,
    integrationKafka:  `${integrationsBase}/kafka`,
    integrationRabbitmq:  `${integrationsBase}/rabbitmq`,
    integrationApachePulsar:  `${integrationsBase}/apache-pulsar`,
    integrationPubsub:  `${integrationsBase}`,
    integrationCoAP:  `${integrationsBase}/coap`,
    integrationKpn:  `${integrationsBase}/kpn-things`,
    integrationCustom:  `${integrationsBase}/custom`,
    integrationTuya:  `${integrationsBase}/tuya`,
    whiteLabeling: `${docsBase}/user-guide/white-labeling/`,
    entityGroups: `${docsBase}/user-guide/groups/`,
    customTranslation: `${docsBase}/user-guide/white-labeling-translation/`,
    customMenu: `${docsBase}/user-guide/white-labeling-menu/`,
    roles: `${docsBase}/user-guide/roles/`,
    selfRegistration: `${docsBase}/user-guide/security/self-registration/`,
    scheduler: `${docsBase}/user-guide/scheduler/`,
    reportTemplates: `${docsBase}/user-guide/reporting/report-templates/`,
    scheduledReports: `${docsBase}/user-guide/reporting/scheduling/`,
    reports: `${docsBase}/user-guide/reporting/getting-started/`,
    trendzSettings: `${helpBaseUrl}/docs/trendz/`,
    secretStorage: `${docsBase}/user-guide/security/secrets-storage/`,
    alarmRules: `${docsBase}/user-guide/alarm-rules/`,
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
  BINARY = 'BINARY',
  HEX = 'HEX'
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
    ],
    [
      ContentType.HEX,
      {
        name: 'content-type.hex',
        code: 'text'
      }
    ]
  ]
);

export const hidePageSizePixelValue = 550;
export const customTranslationsPrefix = 'custom.';
export const i18nPrefix = 'i18n';

export const MODULES_MAP = new InjectionToken<IModulesMap>('ModulesMap');
