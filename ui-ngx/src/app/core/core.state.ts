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
