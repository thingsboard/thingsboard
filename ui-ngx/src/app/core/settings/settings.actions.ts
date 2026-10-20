// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Action } from '@ngrx/store';

export enum SettingsActionTypes {
  CHANGE_LANGUAGE = '[Settings] Change Language',
  CHANGE_WHITE_LABELING = '[Settings] Change White-labeling',
}

export class ActionSettingsChangeLanguage implements Action {
  readonly type = SettingsActionTypes.CHANGE_LANGUAGE;

  constructor(readonly payload: { userLang: string; reload: boolean; ignoredLoad: boolean}) {}
}

export class ActionSettingsChangeWhiteLabeling implements Action {
  readonly type = SettingsActionTypes.CHANGE_WHITE_LABELING;

  constructor(readonly payload: {}) {}
}

export type SettingsActions =
  | ActionSettingsChangeLanguage | ActionSettingsChangeWhiteLabeling;
