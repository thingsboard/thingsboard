// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { AuthPayload, AuthState } from './auth.models';
import { AuthActions, AuthActionTypes } from './auth.actions';
import { initialUserSettings, UserSettings } from '@shared/models/user-settings.models';
import { environment as env } from '@env/environment';
import { unset } from '@core/utils';

const emptyUserAuthState: AuthPayload = {
  authUser: null,
  userDetails: null,
  userTokenAccessEnabled: false,
  forceFullscreen: false,
  edgesSupportEnabled: false,
  whiteLabelingAllowed: false,
  customerWhiteLabelingAllowed: false,
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
  allowedDashboardIds: [],
  availableLocales: env.supportedLangs,
  aiEnabled: false,
  allowKeyFiltersOrConditions: true,
  nullsOrderStrategy: 'default',
  edqsEnabled: false,
  iotHubBaseUrl: '',
  licenseVersion: 0,
  edgeEnabled: false,
  trendzEnabled: false,
  integrationsEnabled: true,
  schedulerEnabled: true,
  reportingEnabled: true,
  communityGrantLicense: false
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
    case AuthActionTypes.UPDATE_LICENSE_PARAMS:
      return { ...state, ...action.payload};

    default:
      return state;
  }
};
