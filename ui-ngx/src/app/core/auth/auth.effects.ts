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

import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UserSettingsService } from '@core/http/user-settings.service';
import { mergeMap, withLatestFrom } from 'rxjs/operators';
import { AuthActions, AuthActionTypes } from '@core/auth/auth.actions';
import { selectAuthState } from '@core/auth/auth.selectors';

@Injectable()
export class AuthEffects {
  constructor(
    private actions$: Actions<AuthActions>,
    private store: Store<AppState>,
    private userSettingsService: UserSettingsService
  ) {
  }

  persistOpenedMenuSections = createEffect(() => this.actions$.pipe(
    ofType(
      AuthActionTypes.UPDATE_OPENED_MENU_SECTION,
    ),
    withLatestFrom(this.store.pipe(select(selectAuthState))),
    mergeMap(([action, state]) => this.userSettingsService.putUserSettings({ openedMenuSections: state.userSettings.openedMenuSections }))
  ), {dispatch: false});

  putUserSettings = createEffect(() => this.actions$.pipe(
    ofType(
      AuthActionTypes.PUT_USER_SETTINGS,
    ),
    mergeMap((state) => this.userSettingsService.putUserSettings(state.payload))
  ), {dispatch: false});

  deleteUserSettings = createEffect(() => this.actions$.pipe(
    ofType(
      AuthActionTypes.DELETE_USER_SETTINGS,
    ),
    mergeMap((state) => this.userSettingsService.deleteUserSettings(state.payload))
  ), {dispatch: false});
}
