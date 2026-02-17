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

import { AuthPayload, AuthState } from './auth.models';
import { AuthActions, AuthActionTypes } from './auth.actions';
import { initialUserSettings, UserSettings } from '@shared/models/user-settings.models';
import { initialTrendzSettings } from '@shared/models/trendz-settings.models';
import { unset } from '@core/utils';

const emptyUserAuthState: AuthPayload = {
  authUser: null,
  userDetails: null,
  userTokenAccessEnabled: false,
  forceFullscreen: false,
  allowedDashboardIds: [],
  edgesSupportEnabled: false,
  hasRepository: false,
  tbelEnabled: false,
  persistDeviceStateToTelemetry: false,
  mobileQrEnabled: false,
  maxResourceSize: 0,
  maxArgumentsPerCF: 0,
  minAllowedDeduplicationIntervalInSecForCF: 0,
  minAllowedAggregationIntervalInSecForCF: 0,
  minAllowedScheduledUpdateIntervalInSecForCF: 0,
  maxRelationLevelPerCfArgument: 0,
  maxRelatedEntitiesToReturnPerCfArgument: 0,
  maxDataPointsPerRollingArg: 0,
  maxDebugModeDurationMinutes: 0,
  intermediateAggregationIntervalInSecForCF: 0,
  userSettings: initialUserSettings,
  trendzSettings: initialTrendzSettings
};

export const initialState: AuthState = {
  isAuthenticated: false,
  isUserLoaded: false,
  lastPublicDashboardId: null,
  ...emptyUserAuthState
};

export const authReducer = (
  state: AuthState = initialState,
  action: AuthActions
): AuthState => {
  let userSettings: UserSettings;
  switch (action.type) {
    case AuthActionTypes.AUTHENTICATED:
      return { ...state, isAuthenticated: true, ...action.payload };

    case AuthActionTypes.UNAUTHENTICATED:
      return { ...state, isAuthenticated: false, ...emptyUserAuthState };

    case AuthActionTypes.LOAD_USER:
      return { ...state, ...action.payload, isAuthenticated: action.payload.isUserLoaded ? state.isAuthenticated : false,
        ...action.payload.isUserLoaded ? {} : emptyUserAuthState };

    case AuthActionTypes.UPDATE_USER_DETAILS:
      return { ...state, ...action.payload};

    case AuthActionTypes.UPDATE_AUTH_USER:
      const authUser = {...state.authUser, ...action.payload};
      return { ...state, ...{ authUser }};

    case AuthActionTypes.UPDATE_LAST_PUBLIC_DASHBOARD_ID:
      return { ...state, ...action.payload};

    case AuthActionTypes.UPDATE_HAS_REPOSITORY:
      return { ...state, ...action.payload};

    case AuthActionTypes.UPDATE_MOBILE_QR_ENABLED:
      return { ...state, ...action.payload};

    case AuthActionTypes.UPDATE_OPENED_MENU_SECTION:
      const openedMenuSections = new Set(state.userSettings.openedMenuSections);
      if (action.payload.opened) {
        if (!openedMenuSections.has(action.payload.path)) {
          openedMenuSections.add(action.payload.path);
        }
      } else {
        openedMenuSections.delete(action.payload.path);
      }
      userSettings = {...state.userSettings, ...{ openedMenuSections: Array.from(openedMenuSections)}};
      return { ...state, ...{ userSettings }};

    case AuthActionTypes.PUT_USER_SETTINGS:
      userSettings = {...state.userSettings, ...action.payload};
      return { ...state, ...{ userSettings }};

    case AuthActionTypes.DELETE_USER_SETTINGS:
      userSettings = {...state.userSettings};
      action.payload.forEach(path => unset(userSettings, path));
      return { ...state, ...{ userSettings }};

    case AuthActionTypes.UPDATE_TRENDZ_SETTINGS:
      return { ...state, trendzSettings: action.payload };

    default:
      return state;
  }
};
