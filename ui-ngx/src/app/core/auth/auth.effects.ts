// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UserSettingsService } from '@core/http/user-settings.service';
import { concatMap, mergeMap, withLatestFrom } from 'rxjs/operators';
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

  persistAiTour = createEffect(() => this.actions$.pipe(
    ofType(
      AuthActionTypes.UPDATE_AI_TOUR,
    ),
    withLatestFrom(this.store.pipe(select(selectAuthState))),
    concatMap(([action, state]) => this.userSettingsService.putUserSettings({ aiTour: state.userSettings.aiTour ?? {} }))
  ), {dispatch: false});

  persistHomeCards = createEffect(() => this.actions$.pipe(
    ofType(
      AuthActionTypes.UPDATE_HOME_CARDS,
    ),
    withLatestFrom(this.store.pipe(select(selectAuthState))),
    concatMap(([action, state]) => this.userSettingsService.putUserSettings({ homeCards: state.userSettings.homeCards ?? {} }))
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
