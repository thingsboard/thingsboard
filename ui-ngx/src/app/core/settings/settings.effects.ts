///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { ActivationEnd, Router } from '@angular/router';
import { Injectable } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { TranslateService } from '@ngx-translate/core';
import { merge } from 'rxjs';
import { distinctUntilChanged, filter, map, tap, withLatestFrom } from 'rxjs/operators';

import { SettingsActions, SettingsActionTypes, } from './settings.actions';
import { selectSettingsState } from './settings.selectors';
import { AppState } from '@app/core/core.state';
import { LocalStorageService } from '@app/core/local-storage/local-storage.service';
import { TitleService } from '@app/core/services/title.service';
import { updateUserLang } from '@app/core/settings/settings.utils';
import { AuthService } from '@core/auth/auth.service';
import { UtilsService } from '@core/services/utils.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { ActionAuthUpdateLastPublicDashboardId } from '../auth/auth.actions';

export const SETTINGS_KEY = 'SETTINGS';

@Injectable()
export class SettingsEffects {
  constructor(
    private actions$: Actions<SettingsActions>,
    private store: Store<AppState>,
    private authService: AuthService,
    private utils: UtilsService,
    private router: Router,
    private localStorageService: LocalStorageService,
    private titleService: TitleService,
    private translate: TranslateService
  ) {
  }

  @Effect({dispatch: false})
  persistSettings = this.actions$.pipe(
    ofType(
      SettingsActionTypes.CHANGE_LANGUAGE,
    ),
    withLatestFrom(this.store.pipe(select(selectSettingsState))),
    tap(([action, settings]) =>
      this.localStorageService.setItem(SETTINGS_KEY, settings)
    )
  );

  @Effect({dispatch: false})
  setTranslateServiceLanguage = this.store.pipe(
    select(selectSettingsState),
    map(settings => settings.userLang),
    distinctUntilChanged(),
    tap(userLang => updateUserLang(this.translate, userLang))
  );

  @Effect({dispatch: false})
  setTitle = merge(
    this.actions$.pipe(ofType(SettingsActionTypes.CHANGE_LANGUAGE)),
    this.router.events.pipe(filter(event => event instanceof ActivationEnd))
  ).pipe(
    tap(() => {
      this.titleService.setTitle(
        this.router.routerState.snapshot.root,
        this.translate
      );
    })
  );

  @Effect({dispatch: false})
  setPublicId = merge(
    this.router.events.pipe(filter(event => event instanceof ActivationEnd))
  ).pipe(
    tap((event) => {
      const authUser = getCurrentAuthUser(this.store);
      const snapshot = (event as ActivationEnd).snapshot;
      if (authUser && authUser.isPublic && snapshot.url && snapshot.url.length
          && snapshot.url[0].path === 'dashboard') {
        this.utils.updateQueryParam('publicId', authUser.sub);
        this.store.dispatch(new ActionAuthUpdateLastPublicDashboardId(
          { lastPublicDashboardId: snapshot.params.dashboardId}));
      }
    })
  );
}
