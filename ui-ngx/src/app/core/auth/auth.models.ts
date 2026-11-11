// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { AuthUser, User } from '@shared/models/user.model';
import { UserSettings } from '@shared/models/user-settings.models';
import { NullsOrderStrategy } from '@shared/models/page/page-link';

export interface SysParamsState {
  userTokenAccessEnabled: boolean;
  allowedDashboardIds: string[];
  edgesSupportEnabled: boolean;
  whiteLabelingAllowed: boolean;
  customerWhiteLabelingAllowed: boolean;
  hasRepository: boolean;
  tbelEnabled: boolean;
  persistDeviceStateToTelemetry: boolean;
  mobileQrEnabled: boolean;
  userSettings: UserSettings;
  maxResourceSize: number;
  maxDebugModeDurationMinutes: number;
  maxDataPointsPerRollingArg: number;
  maxArgumentsPerCF: number;
  minAllowedDeduplicationIntervalInSecForCF: number;
  minAllowedAggregationIntervalInSecForCF: number;
  minAllowedScheduledUpdateIntervalInSecForCF: number;
  maxRelationLevelPerCfArgument: number;
  maxRelatedEntitiesToReturnPerCfArgument: number;
  ruleChainDebugPerTenantLimitsConfiguration?: string;
  calculatedFieldDebugPerTenantLimitsConfiguration?: string;
  intermediateAggregationIntervalInSecForCF: number;
  integrationDebugPerTenantLimitsConfiguration?: string;
  converterDebugPerTenantLimitsConfiguration?: string;
  availableLocales: string[];
  aiEnabled: boolean;
  allowKeyFiltersOrConditions: boolean;
  nullsOrderStrategy: NullsOrderStrategy;
  edqsEnabled: boolean;
  iotHubBaseUrl: string;
  licenseVersion: number;
  edgeEnabled: boolean;
  trendzEnabled: boolean;
  integrationsEnabled: boolean;
  schedulerEnabled: boolean;
  reportingEnabled: boolean;
  communityGrantLicense: boolean;
}

export interface SysParams extends SysParamsState {
  maxDatapointsLimit: number;
}

export interface AuthPayload extends SysParamsState {
  authUser: AuthUser;
  userDetails: User;
  forceFullscreen: boolean;
}

export interface AuthState extends AuthPayload {
  isAuthenticated: boolean;
  isUserLoaded: boolean;
  lastPublicDashboardId: string;
}
