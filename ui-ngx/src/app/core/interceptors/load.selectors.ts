// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { createFeatureSelector, createSelector, select, Store } from '@ngrx/store';

import { AppState } from '../core.state';
import { LoadState } from './load.models';
import { take } from 'rxjs/operators';

export const selectLoadState = createFeatureSelector< LoadState>(
  'load'
);

export const selectLoad = createSelector(
  selectLoadState,
  (state: LoadState) => state
);

export const selectIsLoading = createSelector(
  selectLoadState,
  (state: LoadState) => state.isLoading
);

export function getCurrentIsLoading(store: Store<AppState>): boolean {
  let isLoading: boolean;
  store.pipe(select(selectIsLoading), take(1)).subscribe(
    val => isLoading = val
  );
  return isLoading;
}
