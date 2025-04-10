///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { AuthUser, User } from '@shared/models/user.model';
import { UserSettings } from '@shared/models/user-settings.models';

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
