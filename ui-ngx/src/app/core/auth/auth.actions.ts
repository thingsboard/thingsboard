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

import { Action } from '@ngrx/store';
import { AuthUser, User } from '@shared/models/user.model';
import { AuthPayload } from '@core/auth/auth.models';
import { UserSettings } from '@shared/models/user-settings.models';
import { TrendzSettings } from "@shared/models/trendz-settings.models";

export enum AuthActionTypes {
  AUTHENTICATED = '[Auth] Authenticated',
  UNAUTHENTICATED = '[Auth] Unauthenticated',
  LOAD_USER = '[Auth] Load User',
  UPDATE_USER_DETAILS = '[Auth] Update User Details',
  UPDATE_AUTH_USER = '[Auth] Update Auth User',
  UPDATE_LAST_PUBLIC_DASHBOARD_ID = '[Auth] Update Last Public Dashboard Id',
  UPDATE_HAS_REPOSITORY = '[Auth] Change Has Repository',
  UPDATE_MOBILE_QR_ENABLED = '[Auth] Update Mobile QR Enabled',
  UPDATE_OPENED_MENU_SECTION = '[Preferences] Update Opened Menu Section',
  PUT_USER_SETTINGS = '[Preferences] Put user settings',
  DELETE_USER_SETTINGS = '[Preferences] Delete user settings',
  UPDATE_TRENDZ_SETTINGS = '[Auth] Update Trendz Settings',
}

export class ActionAuthAuthenticated implements Action {
  readonly type = AuthActionTypes.AUTHENTICATED;

  constructor(readonly payload: AuthPayload) {}
}

export class ActionAuthUnauthenticated implements Action {
  readonly type = AuthActionTypes.UNAUTHENTICATED;
}

export class ActionAuthLoadUser implements Action {
  readonly type = AuthActionTypes.LOAD_USER;

  constructor(readonly payload: { isUserLoaded: boolean }) {}
}

export class ActionAuthUpdateUserDetails implements Action {
  readonly type = AuthActionTypes.UPDATE_USER_DETAILS;

  constructor(readonly payload: { userDetails: User }) {}
}

export class ActionAuthUpdateAuthUser implements Action {
  readonly type = AuthActionTypes.UPDATE_AUTH_USER;

  constructor(readonly payload: Partial<AuthUser>) {}
}

export class ActionAuthUpdateLastPublicDashboardId implements Action {
  readonly type = AuthActionTypes.UPDATE_LAST_PUBLIC_DASHBOARD_ID;

  constructor(readonly payload: { lastPublicDashboardId: string }) {}
}

export class ActionAuthUpdateHasRepository implements Action {
  readonly type = AuthActionTypes.UPDATE_HAS_REPOSITORY;

  constructor(readonly payload: { hasRepository: boolean }) {}
}

export class ActionUpdateMobileQrCodeEnabled implements Action {
  readonly type = AuthActionTypes.UPDATE_MOBILE_QR_ENABLED;

  constructor(readonly payload: { mobileQrEnabled: boolean }) {}
}

export class ActionPreferencesUpdateOpenedMenuSection implements Action {
  readonly type = AuthActionTypes.UPDATE_OPENED_MENU_SECTION;

  constructor(readonly payload: { path: string; opened: boolean }) {}
}

export class ActionPreferencesPutUserSettings implements Action {
  readonly type = AuthActionTypes.PUT_USER_SETTINGS;

  constructor(readonly payload: Partial<UserSettings>) {}
}

export class ActionPreferencesDeleteUserSettings implements Action {
  readonly type = AuthActionTypes.DELETE_USER_SETTINGS;

  constructor(readonly payload: Array<NestedKeyOf<UserSettings>>) {}
}

export class ActionAuthUpdateTrendzSettings implements Action {
  readonly type = AuthActionTypes.UPDATE_TRENDZ_SETTINGS;

  constructor(readonly payload: TrendzSettings) {}
}

export type AuthActions = ActionAuthAuthenticated | ActionAuthUnauthenticated |
  ActionAuthLoadUser | ActionAuthUpdateUserDetails | ActionAuthUpdateLastPublicDashboardId | ActionAuthUpdateHasRepository |
  ActionPreferencesUpdateOpenedMenuSection | ActionPreferencesPutUserSettings | ActionPreferencesDeleteUserSettings |
  ActionAuthUpdateAuthUser | ActionUpdateMobileQrCodeEnabled | ActionAuthUpdateTrendzSettings;
