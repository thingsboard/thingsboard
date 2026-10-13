// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import 'hammerjs';

import { Component } from '@angular/core';

import { environment as env } from '@env/environment';

import { TranslateService, TranslateStore } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { LocalStorageService } from '@core/local-storage/local-storage.service';
import { DomSanitizer } from '@angular/platform-browser';
import { MatIconRegistry } from '@angular/material/icon';
import { getCurrentAuthState, selectUserReady } from '@core/auth/auth.selectors';
import { filter, skip, tap } from 'rxjs/operators';
import { AuthService } from '@core/auth/auth.service';
import { DashboardReportService } from '@core/http/dashboard-report.service';
import { DevelopmentService } from '@core/http/development.service';
import { svgIcons, svgIconsUrl } from '@shared/models/icon.models';
import { ActionSettingsChangeLanguage } from '@core/settings/settings.actions';
import { SETTINGS_KEY } from '@core/settings/settings.effects';
import { initCustomJQueryEvents } from '@shared/models/jquery-event.models';
import { TranslateDefaultLoader } from '@core/translate/translate-default-loader';
import { SystemSetupService } from '@core/http/system-setup.service';
import { SystemSetupState } from '@shared/models/system-setup.models';

@Component({
    selector: 'tb-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    standalone: false
})
export class AppComponent {

  constructor(private store: Store<AppState>,
              private storageService: LocalStorageService,
              private translateStore: TranslateStore,
              private translate: TranslateService,
              private matIconRegistry: MatIconRegistry,
              private domSanitizer: DomSanitizer,
              private authService: AuthService,
              private reportService: DashboardReportService,
              private developmentService: DevelopmentService,
              private systemSetupService: SystemSetupService) {

    if (!env.production) {
      console.log(`ThingsBoard Version: ${env.tbVersion}`);
    }

    this.matIconRegistry.addSvgIconResolver((name, namespace) => {
      if (namespace === 'mdi') {
        return this.domSanitizer.bypassSecurityTrustResourceUrl(`./assets/mdi/${name}.svg`);
      } else {
        return null;
      }
    });

    for (const svgIcon of Object.keys(svgIcons)) {
      this.matIconRegistry.addSvgIconLiteral(
        svgIcon,
        this.domSanitizer.bypassSecurityTrustHtml(
          svgIcons[svgIcon]
        )
      );
    }

    for (const svgIcon of Object.keys(svgIconsUrl)) {
      this.matIconRegistry.addSvgIcon(svgIcon, this.domSanitizer.bypassSecurityTrustResourceUrl(svgIconsUrl[svgIcon]));
    }

    this.storageService.testLocalStorage();

    this.setupTranslate();

    this.systemSetupService.checkSetupState().subscribe((gotoDefaultPlace) => {
      (this.translate.currentLoader as TranslateDefaultLoader).isSetupCompleted = true;

      this.developmentService.checkIsDevelopment();

      this.setupAuth(gotoDefaultPlace);

      initCustomJQueryEvents();
    });

    this.routeAuthenticationChangesWhileSetupIsIncomplete();
  }

  /**
   * setupAuth() below routes authentication changes, but it is installed only from checkSetupState()'s emission,
   * which fires for READY alone. On a locked instance nothing installs it, so a successful sign-in left the user
   * on the login form with nothing to click. Kept general: the sign-out ending forced 2FA is stranded the same way.
   *
   * Does nothing while the instance reports READY, so both subscriptions may be live after a mid-session relock -
   * routing then happens twice and a captured redirect URL is dropped. Harmless: the next 423 re-routes anyway.
   *
   * skip(1) matches setupAuth()'s cold-start handling - the initial load is not a change the user made.
   */
  private routeAuthenticationChangesWhileSetupIsIncomplete() {
    this.store.select(selectUserReady).pipe(
      filter((data) => data.isUserLoaded),
      skip(1),
      filter(() => {
        const setupState = this.systemSetupService.getState();
        return !!setupState && setupState !== SystemSetupState.READY;
      })
    ).subscribe((data) => {
      this.authService.gotoDefaultPlace(data.isAuthenticated);
    });
  }

  setupTranslate() {
    if (!env.production) {
      console.log(`Supported Langs: ${env.supportedLangs}`);
    }
    this.translate.addLangs(env.supportedLangs);
    if (!env.production) {
      console.log(`Default Lang: ${env.defaultLang}`);
    }
    this.translateStore.setFallbackLang(env.defaultLang);
    this.translate.setTranslation(undefined, {
      "access": {
        "refresh-token-expired": "Session has expired",
        "refresh-token-failed": "Unable to refresh session"
      }
    }, true);
  }

  setupAuth(gotoDefaultPlace: boolean) {
    this.store.select(selectUserReady).pipe(
      filter((data) => data.isUserLoaded),
      tap((data) => {
        if (!data.isAuthenticated) {
          const settings = this.storageService.getItem(SETTINGS_KEY);
          const userLang = settings?.userLang ?? null;
          (this.translate.currentLoader as TranslateDefaultLoader).isAuthenticated = false;
          this.notifyUserLang(userLang);
        } else {
          this.notifyUserLang(this.translate.currentLang, true);
        }
      }),
      skip(gotoDefaultPlace ? 0 : 1),
    ).subscribe((data) => {
      this.authService.gotoDefaultPlace(data.isAuthenticated);
    });
    // Only if nobody is loaded yet. The setup screen establishes the session before handing off, precisely so
    // this does not re-authenticate a working one - a failed cold reload would sign the user out.
    if (!this.reportService.loadReportParams() && !getCurrentAuthState(this.store).isUserLoaded) {
      this.authService.reloadUser();
    }
  }

  onActivateComponent(_$event: any) {
    const loadingElement = $('div#tb-loading-spinner');
    if (loadingElement.length) {
      loadingElement.remove();
    }
  }

  private notifyUserLang(userLang: string, ignoredLoad = false) {
    this.store.dispatch(new ActionSettingsChangeLanguage({userLang, reload: true, ignoredLoad}));
  }

}
