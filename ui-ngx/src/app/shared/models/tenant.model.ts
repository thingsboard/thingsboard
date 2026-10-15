// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { ContactBased } from '@shared/models/contact-based.model';
import { TenantId } from './id/tenant-id';
import { TenantProfileId } from '@shared/models/id/tenant-profile-id';
import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { QueueInfo } from '@shared/models/queue.models';
import { FormControl } from '@angular/forms';

export type FormControlsFrom<T> = {
  [K in keyof T]-?: FormControl<T[K] | null>;
};

export enum TenantProfileType {
  DEFAULT = 'DEFAULT'
}

export interface DefaultTenantProfileConfiguration {
  maxDevices: number;
  maxAssets: number;
  maxCustomers: number;
  maxUsers: number;
  maxDashboards: number;
  maxRuleChains: number;
  maxEdges: number;
  maxIntegrations: number;
  maxConverters: number;
  maxSchedulerEvents: number;
  maxAgents: number;
  maxAgentApplications: number;
  maxGeneratedReports: number;
  maxResourcesInBytes: number;
  maxOtaPackagesInBytes: number;
  maxResourceSize: number;
  maxReportSizeInBytes: number;

  transportTenantMsgRateLimit?: string;
  transportTenantTelemetryMsgRateLimit?: string;
  transportTenantTelemetryDataPointsRateLimit?: string;
  transportDeviceMsgRateLimit?: string;
  transportDeviceTelemetryMsgRateLimit?: string;
  transportDeviceTelemetryDataPointsRateLimit?: string;

  transportGatewayMsgRateLimit?: string;
  transportGatewayTelemetryMsgRateLimit?: string;
  transportGatewayTelemetryDataPointsRateLimit?: string;
  transportGatewayDeviceMsgRateLimit?: string;
  transportGatewayDeviceTelemetryMsgRateLimit?: string;
  transportGatewayDeviceTelemetryDataPointsRateLimit?: string;

  integrationMsgsPerTenantRateLimit?: string;
  integrationMsgsPerDeviceRateLimit?: string;
  integrationMsgsPerAssetRateLimit?: string;

  tenantEntityExportRateLimit?: string;
  tenantEntityImportRateLimit?: string;
  tenantNotificationRequestsRateLimit?: string;
  tenantNotificationRequestsPerRuleRateLimit?: string;

  maxTransportMessages: number;
  maxTransportDataPoints: number;
  maxREExecutions: number;
  maxJSExecutions: number;
  maxTbelExecutions: number;
  maxDPStorageDays: number;
  maxRuleNodeExecutionsPerMessage: number;
  maxEmails: number;
  maxSms: number;
  smsEnabled: boolean;
  maxCreatedAlarms: number;
  maxAiCredits: number;

  maxDebugModeDurationMinutes: number;

  tenantServerRestLimitsConfiguration: string;
  customerServerRestLimitsConfiguration: string;

  maxWsSessionsPerTenant: number;
  maxWsSessionsPerCustomer: number;
  maxWsSessionsPerRegularUser: number;
  maxWsSessionsPerPublicUser: number;
  wsMsgQueueLimitPerSession: number;
  maxWsSubscriptionsPerTenant: number;
  maxWsSubscriptionsPerCustomer: number;
  maxWsSubscriptionsPerRegularUser: number;
  maxWsSubscriptionsPerPublicUser: number;
  wsUpdatesPerSessionRateLimit: string;

  cassandraWriteQueryTenantCoreRateLimits: string;
  cassandraReadQueryTenantCoreRateLimits: string;
  cassandraWriteQueryTenantRuleEngineRateLimits: string;
  cassandraReadQueryTenantRuleEngineRateLimits: string;

  edgeEventRateLimits?: string;
  edgeEventRateLimitsPerEdge?: string;
  edgeUplinkMessagesRateLimits?: string;
  edgeUplinkMessagesRateLimitsPerEdge?: string;

  agentEventRateLimits?: string;
  agentEventRateLimitsPerAgent?: string;
  agentLogChunkRateLimits?: string;
  agentLogChunkRateLimitsPerAgent?: string;

  defaultStorageTtlDays: number;
  alarmsTtlDays: number;
  rpcTtlDays: number;
  queueStatsTtlDays: number;
  ruleEngineExceptionsTtlDays: number;
  blobEntityTtlDays: number;
  reportTtlDays: number;

  maxCalculatedFieldsPerEntity: number;
  maxArgumentsPerCF: number;
  maxRelationLevelPerCfArgument: number;
  minAllowedDeduplicationIntervalInSecForCF: number;
  minAllowedAggregationIntervalInSecForCF: number;
  maxRelatedEntitiesToReturnPerCfArgument: number;
  minAllowedScheduledUpdateIntervalInSecForCF: number;
  intermediateAggregationIntervalInSecForCF: number;
  cfReevaluationCheckInterval: number;
  alarmsReevaluationInterval: number;

  maxDataPointsPerRollingArg: number;
  maxStateSizeInKBytes: number;
  maxSingleValueArgumentSizeInKBytes: number;
  calculatedFieldDebugEventsRateLimit: string;

  aiChatRequestsPerTenantRateLimit: string;
}

export type TenantProfileConfigurations = DefaultTenantProfileConfiguration;

export interface TenantProfileConfiguration extends TenantProfileConfigurations {
  type: TenantProfileType;
}

export function createTenantProfileConfiguration(type: TenantProfileType): TenantProfileConfiguration {
  let configuration: TenantProfileConfiguration = null;
  if (type) {
    switch (type) {
      case TenantProfileType.DEFAULT:
        const defaultConfiguration: DefaultTenantProfileConfiguration = {
          maxDevices: 0,
          maxAssets: 0,
          maxCustomers: 0,
          maxUsers: 0,
          maxDashboards: 0,
          maxRuleChains: 0,
          maxEdges: 0,
          maxIntegrations: 0,
          maxConverters: 0,
          maxSchedulerEvents: 0,
          maxAgents: 0,
          maxAgentApplications: 0,
          maxGeneratedReports: 0,
          maxAiCredits: 0,
          maxResourcesInBytes: 0,
          maxOtaPackagesInBytes: 0,
          maxResourceSize: 0,
          maxReportSizeInBytes: 0,
          maxTransportMessages: 0,
          maxTransportDataPoints: 0,
          maxREExecutions: 0,
          maxJSExecutions: 0,
          maxTbelExecutions: 0,
          maxDPStorageDays: 0,
          maxRuleNodeExecutionsPerMessage: 0,
          maxEmails: 0,
          maxSms: 0,
          smsEnabled: true,
          maxCreatedAlarms: 0,
          maxDebugModeDurationMinutes: 15,
          tenantServerRestLimitsConfiguration: '',
          customerServerRestLimitsConfiguration: '',
          maxWsSessionsPerTenant: 0,
          maxWsSessionsPerCustomer: 0,
          maxWsSessionsPerRegularUser: 0,
          maxWsSessionsPerPublicUser: 0,
          wsMsgQueueLimitPerSession: 0,
          maxWsSubscriptionsPerTenant: 0,
          maxWsSubscriptionsPerCustomer: 0,
          maxWsSubscriptionsPerRegularUser: 0,
          maxWsSubscriptionsPerPublicUser: 0,
          wsUpdatesPerSessionRateLimit: '',
          cassandraWriteQueryTenantCoreRateLimits: '',
          cassandraReadQueryTenantCoreRateLimits: '',
          cassandraWriteQueryTenantRuleEngineRateLimits: '',
          cassandraReadQueryTenantRuleEngineRateLimits: '',
          defaultStorageTtlDays: 0,
          alarmsTtlDays: 0,
          rpcTtlDays: 0,
          queueStatsTtlDays: 0,
          ruleEngineExceptionsTtlDays: 0,
          blobEntityTtlDays: 0,
          reportTtlDays: 0,
          maxCalculatedFieldsPerEntity: 100,
          maxArgumentsPerCF: 10,
          maxDataPointsPerRollingArg: 1000,
          maxRelationLevelPerCfArgument: 2,
          minAllowedDeduplicationIntervalInSecForCF: 10,
          minAllowedAggregationIntervalInSecForCF: 60,
          maxRelatedEntitiesToReturnPerCfArgument: 1000,
          minAllowedScheduledUpdateIntervalInSecForCF: 10,
          intermediateAggregationIntervalInSecForCF: 300,
          cfReevaluationCheckInterval: 60,
          alarmsReevaluationInterval: 60,
          maxStateSizeInKBytes: 512,
          maxSingleValueArgumentSizeInKBytes: 32,
          calculatedFieldDebugEventsRateLimit: '',
          aiChatRequestsPerTenantRateLimit: ''
        };
        configuration = {...defaultConfiguration, type: TenantProfileType.DEFAULT};
        break;
    }
  }
  return configuration;
}

export interface TenantProfileData {
  configuration: TenantProfileConfiguration;
  queueConfiguration?: Array<QueueInfo>;
}

export interface TenantProfile extends BaseData<TenantProfileId>, ExportableEntity<TenantProfileId> {
  name: string;
  description?: string;
  default?: boolean;
  isolatedTbRuleEngine?: boolean;
  profileData?: TenantProfileData;
}

export interface Tenant extends ContactBased<TenantId> {
  title: string;
  region: string;
  tenantProfileId: TenantProfileId;
  additionalInfo?: any;
}

export interface TenantInfo extends Tenant {
  tenantProfileName: string;
}
