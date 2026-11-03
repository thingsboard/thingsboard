// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { ActionReducerMap, MetaReducer } from '@ngrx/store';
import { storeFreeze } from 'ngrx-store-freeze';

import { environment as env } from '@env/environment';

import { initStateFromLocalStorage } from './meta-reducers/init-state-from-local-storage.reducer';
import { debug } from './meta-reducers/debug.reducer';
import { LoadState } from './interceptors/load.models';
import { loadReducer } from './interceptors/load.reducer';
import { AuthState } from './auth/auth.models';
import { authReducer } from './auth/auth.reducer';
import { settingsReducer } from '@app/core/settings/settings.reducer';
import { SettingsState } from '@app/core/settings/settings.models';
import { Type } from '@angular/core';
import { SettingsEffects } from '@app/core/settings/settings.effects';
import { NotificationState } from '@app/core/notification/notification.models';
import { notificationReducer } from '@app/core/notification/notification.reducer';
import { NotificationEffects } from '@app/core/notification/notification.effects';
import { AuthEffects } from '@core/auth/auth.effects';

export const reducers: ActionReducerMap<AppState> = {
  load: loadReducer,
  auth: authReducer,
  settings: settingsReducer,
  notification: notificationReducer
};

export const metaReducers: MetaReducer<AppState>[] = [
  initStateFromLocalStorage
];
if (!env.production) {
  metaReducers.unshift(storeFreeze);
  metaReducers.unshift(debug);
}

export const effects: Type<any>[] = [
  AuthEffects,
  SettingsEffects,
  NotificationEffects
];

export interface AppState {
  load: LoadState;
  auth: AuthState;
  settings: SettingsState;
  notification: NotificationState;
}
