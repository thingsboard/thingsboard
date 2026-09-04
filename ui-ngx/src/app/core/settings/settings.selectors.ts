// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { createFeatureSelector, createSelector } from '@ngrx/store';

import { SettingsState } from './settings.models';
import { AppState } from '@app/core/core.state';

export const selectSettingsState = createFeatureSelector< SettingsState>(
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
