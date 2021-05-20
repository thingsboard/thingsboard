///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { createFeatureSelector, createSelector } from '@ngrx/store';

import { SettingsState } from './settings.models';
import { AppState } from '@app/core/core.state';

export const selectSettingsState = createFeatureSelector<AppState, SettingsState>(
  'settings'
);

export const selectSettings = createSelector(
  selectSettingsState,
  (state: SettingsState) => state
);

export const selectUserLang = createSelector(
  selectSettings,
  (state: SettingsState) => state.userLang
);
