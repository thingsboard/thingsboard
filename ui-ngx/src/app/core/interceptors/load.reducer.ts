// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { LoadState } from './load.models';
import { LoadActions, LoadActionTypes } from './load.actions';

export const initialState: LoadState = {
  isLoading: false
};

export function loadReducer(
  state: LoadState = initialState,
  action: LoadActions
): LoadState {
  switch (action.type) {
    case LoadActionTypes.START_LOAD:
      return { ...state, isLoading: true };

    case LoadActionTypes.FINISH_LOAD:
      return { ...state, isLoading: false };

    default:
      return state;
  }
}
