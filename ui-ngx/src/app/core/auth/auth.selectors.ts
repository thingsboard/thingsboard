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

import { createFeatureSelector, createSelector, select, Store } from '@ngrx/store';

import { AppState } from '../core.state';
import { AuthState } from './auth.models';
import { take } from 'rxjs/operators';
import { AuthUser } from '@shared/models/user.model';
import { UserSettings } from '@shared/models/user-settings.models';
import { getDescendantProp } from '@core/utils';

export const selectAuthState = createFeatureSelector< AuthState>(
  'auth'
);

export const selectAuth = createSelector(
  selectAuthState,
  (state: AuthState) => state
);

export const selectIsAuthenticated = createSelector(
  selectAuthState,
  (state: AuthState) => state.isAuthenticated
);

export const selectIsUserLoaded = createSelector(
  selectAuthState,
  (state: AuthState) => state.isUserLoaded
);

export const selectUserReady = createSelector(
  selectIsAuthenticated,
  selectIsUserLoaded,
  (isAuthenticated, isUserLoaded) => ({isAuthenticated, isUserLoaded})
);

export const selectAuthUser = createSelector(
  selectAuthState,
  (state: AuthState) => state.authUser
);

export const selectUserDetails = createSelector(
  selectAuthState,
  (state: AuthState) => state.userDetails
);

export const selectUserTokenAccessEnabled = createSelector(
  selectAuthState,
  (state: AuthState) => state.userTokenAccessEnabled
);

export const selectHasRepository = createSelector(
  selectAuthState,
  (state: AuthState) => state.hasRepository
);

export const selectTbelEnabled = createSelector(
  selectAuthState,
  (state: AuthState) => state.tbelEnabled
);

export const selectPersistDeviceStateToTelemetry = createSelector(
  selectAuthState,
  (state: AuthState) => state.persistDeviceStateToTelemetry
);

export const selectMobileQrEnabled = createSelector(
  selectAuthState,
  (state: AuthState) => state.mobileQrEnabled
);

export const selectHomeDashboardParams = createSelector(
  selectPersistDeviceStateToTelemetry,
  selectMobileQrEnabled,
  (persistDeviceStateToTelemetry, mobileQrEnabled) => ({persistDeviceStateToTelemetry, mobileQrEnabled})
);

export const selectUserSettings = createSelector(
  selectAuthState,
  (state: AuthState) => state.userSettings
);

export const selectUserSettingsProperty = (path: NestedKeyOf<UserSettings>) => createSelector(
  selectAuthState,
  (state: AuthState) => getDescendantProp(state.userSettings, path)
);

export const selectOpenedMenuSections = createSelector(
  selectAuthState,
  (state: AuthState) => state.userSettings.openedMenuSections
);


export const getCurrentAuthState = (store: Store<AppState>): AuthState => {
  let state: AuthState;
  store.pipe(select(selectAuth), take(1)).subscribe(
    val => state = val
  );
  return state;
};

export const getCurrentAuthUser = (store: Store<AppState>): AuthUser => {
  let authUser: AuthUser;
  store.pipe(select(selectAuthUser), take(1)).subscribe(
    val => authUser = val
  );
  return authUser;
};

export const getCurrentUserSettings = (store: Store<AppState>): UserSettings => {
  let userSettings: UserSettings;
  store.pipe(select(selectUserSettings), take(1)).subscribe(
    val => userSettings = val
  );
  return userSettings;
};

export const getCurrentOpenedMenuSections = (store: Store<AppState>): string[] => {
  let openedMenuSections: string[];
  store.pipe(select(selectOpenedMenuSections), take(1)).subscribe(
    val => openedMenuSections = val
  );
  return openedMenuSections;
};
