// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { AuthUser, User } from '@shared/models/user.model';
import { UserSettings } from '@shared/models/user-settings.models';
import { TrendzSettings } from '@shared/models/trendz-settings.models';
import { NullsOrderStrategy } from '@shared/models/page/page-link';

export interface SysParamsState {
  userTokenAccessEnabled: boolean;
  allowedDashboardIds: string[];
  edgesSupportEnabled: boolean;
  hasRepository: boolean;
  tbelEnabled: boolean;
  persistDeviceStateToTelemetry: boolean;
  mobileQrEnabled: boolean;
  userSettings: UserSettings;
  maxResourceSize: number;
  maxDebugModeDurationMinutes: number;
  maxDataPointsPerRollingArg: number;
  maxArgumentsPerCF: number;
  ruleChainDebugPerTenantLimitsConfiguration?: string;
  calculatedFieldDebugPerTenantLimitsConfiguration?: string;
  trendzSettings: TrendzSettings;
  nullsOrderStrategy: NullsOrderStrategy;
  edqsEnabled: boolean;
  iotHubBaseUrl: string;
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
