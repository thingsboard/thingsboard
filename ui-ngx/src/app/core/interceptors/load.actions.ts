// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Action } from '@ngrx/store';

export enum LoadActionTypes {
  START_LOAD = '[Load] Start',
  FINISH_LOAD = '[Load] Finish'
}

export class ActionLoadStart implements Action {
  readonly type = LoadActionTypes.START_LOAD;
}

export class ActionLoadFinish implements Action {
  readonly type = LoadActionTypes.FINISH_LOAD;
}

export type LoadActions = ActionLoadStart | ActionLoadFinish;
