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

import 'hammerjs';

import { Component } from '@angular/core';

import { environment as env } from '@env/environment';

import { TranslateService } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { LocalStorageService } from '@core/local-storage/local-storage.service';
import { DomSanitizer } from '@angular/platform-browser';
import { MatIconRegistry } from '@angular/material/icon';
import { getCurrentAuthState, selectUserReady } from '@core/auth/auth.selectors';
import { filter, skip, tap } from 'rxjs/operators';
import { AuthService } from '@core/auth/auth.service';
import { svgIcons, svgIconsUrl } from '@shared/models/icon.models';
import { ActionSettingsChangeLanguage } from '@core/settings/settings.actions';
import { SETTINGS_KEY } from '@core/settings/settings.effects';
import { initCustomJQueryEvents } from '@shared/models/jquery-event.models';

@Component({
  selector: 'tb-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {

  constructor(private store: Store<AppState>,
              private storageService: LocalStorageService,
              private translate: TranslateService,
              private matIconRegistry: MatIconRegistry,
              private domSanitizer: DomSanitizer,
              private authService: AuthService) {

    console.log(`ThingsBoard Version: ${env.tbVersion}`);

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
    this.setupAuth();

    initCustomJQueryEvents();
  }

  setupTranslate() {
    if (!env.production) {
      console.log(`Supported Langs: ${env.supportedLangs}`);
    }
    this.translate.addLangs(env.supportedLangs);
    if (!env.production) {
      console.log(`Default Lang: ${env.defaultLang}`);
    }
    this.translate.setDefaultLang(env.defaultLang);
  }

  setupAuth() {
    this.store.select(selectUserReady).pipe(
      filter((data) => data.isUserLoaded),
      tap((data) => {
        let userLang = getCurrentAuthState(this.store).userDetails?.additionalInfo?.lang ?? null;
        if (!userLang && !data.isAuthenticated) {
          const settings = this.storageService.getItem(SETTINGS_KEY);
          userLang = settings?.userLang ?? null;
        }
        this.notifyUserLang(userLang);
      }),
      skip(1),
    ).subscribe((data) => {
      this.authService.gotoDefaultPlace(data.isAuthenticated);
    });
    this.authService.reloadUser();
  }

  onActivateComponent(_$event: any) {
    const loadingElement = $('div#tb-loading-spinner');
    if (loadingElement.length) {
      loadingElement.remove();
    }
  }

  private notifyUserLang(userLang: string) {
    this.store.dispatch(new ActionSettingsChangeLanguage({userLang}));
  }

}
