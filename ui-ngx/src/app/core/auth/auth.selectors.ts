// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
