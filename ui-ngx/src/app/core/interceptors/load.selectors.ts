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

import { createFeatureSelector, createSelector, select, Store } from '@ngrx/store';

import { AppState } from '../core.state';
import { LoadState } from './load.models';
import { take } from 'rxjs/operators';

export const selectLoadState = createFeatureSelector<AppState, LoadState>(
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
