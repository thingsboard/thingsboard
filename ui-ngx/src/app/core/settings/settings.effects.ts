// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { ActivationEnd, Router } from '@angular/router';
import { Inject, Injectable, DOCUMENT } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { TranslateService, TranslateStore } from '@ngx-translate/core';
import { merge } from 'rxjs';
import { filter, tap, withLatestFrom } from 'rxjs/operators';

import { SettingsActions, SettingsActionTypes, } from './settings.actions';
import { selectSettingsState } from './settings.selectors';
import { AppState } from '@app/core/core.state';
import { LocalStorageService } from '@app/core/local-storage/local-storage.service';
import { TitleService } from '@app/core/services/title.service';
import { updateUserLang } from '@app/core/settings/settings.utils';
import { UtilsService } from '@core/services/utils.service';
import { getCurrentAuthState, getCurrentAuthUser } from '@core/auth/auth.selectors';
import { ActionAuthUpdateLastPublicDashboardId } from '../auth/auth.actions';

import { FaviconService } from '@core/services/favicon.service';
import { DashboardReportService } from '@core/http/dashboard-report.service';

export const SETTINGS_KEY = 'SETTINGS';

@Injectable()
export class SettingsEffects {
  constructor(
    private actions$: Actions<SettingsActions>,
    private store: Store<AppState>,
    private utils: UtilsService,
    private router: Router,
    private localStorageService: LocalStorageService,
    private titleService: TitleService,
    private translate: TranslateService,
    private translateStore: TranslateStore,
    @Inject(DOCUMENT) private document: Document,
    private faviconService: FaviconService,
    private reportService: DashboardReportService,
  ) {
  }

  setTranslateServiceLanguage = createEffect(() => this.actions$.pipe(
    ofType(
      SettingsActionTypes.CHANGE_LANGUAGE,
    ),
    withLatestFrom(this.store.pipe(select(selectSettingsState))),
    tap(([action, settings]) => {
      this.localStorageService.setItem(SETTINGS_KEY, {userLang: settings.userLang});
      if (!settings.ignoredLoad) {
        const availableLocales = getCurrentAuthState(this.store)?.availableLocales;
        updateUserLang(this.translate, this.translateStore, this.document, settings.userLang, availableLocales, settings.reload)
          .subscribe(() => {});
      }
    })
  ), {dispatch: false});

  setTitle = createEffect(() => merge(
    this.actions$.pipe(ofType(SettingsActionTypes.CHANGE_LANGUAGE, SettingsActionTypes.CHANGE_WHITE_LABELING)),
    this.router.events.pipe(filter(event => event instanceof ActivationEnd))
  ).pipe(
    tap(() => {
      this.titleService.setTitle(
        this.router.routerState.snapshot.root,
        this.translate
      );
    })
  ), {dispatch: false});

  setFavicon = createEffect(() => merge(
    this.actions$.pipe(ofType(SettingsActionTypes.CHANGE_WHITE_LABELING)),
  ).pipe(
    tap(() => {
      this.faviconService.setFavicon();
    })
  ), {dispatch: false});

  setPublicId = createEffect(() => merge(
    this.router.events.pipe(filter(event => event instanceof ActivationEnd))
  ).pipe(
    tap((event) => {
      const authUser = getCurrentAuthUser(this.store);
      const snapshot = (event as ActivationEnd).snapshot;
      if (!this.reportService.reportView && authUser && authUser.isPublic && snapshot.url && snapshot.url.length
          && snapshot.url[0].path === 'dashboard') {
        this.utils.updateQueryParam('publicId', authUser.sub);
        this.store.dispatch(new ActionAuthUpdateLastPublicDashboardId(
          { lastPublicDashboardId: snapshot.params.dashboardId}));
      }
    })
  ), {dispatch: false});
}
