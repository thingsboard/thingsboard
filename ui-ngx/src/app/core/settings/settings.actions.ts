// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Action } from '@ngrx/store';

export enum SettingsActionTypes {
  CHANGE_LANGUAGE = '[Settings] Change Language'
}

export class ActionSettingsChangeLanguage implements Action {
  readonly type = SettingsActionTypes.CHANGE_LANGUAGE;

  constructor(readonly payload: { userLang: string }) {}
}

export type SettingsActions =
  | ActionSettingsChangeLanguage;
