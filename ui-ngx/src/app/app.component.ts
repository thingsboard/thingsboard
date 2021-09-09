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

import 'hammerjs';

import { Component, OnInit } from '@angular/core';

import { environment as env } from '@env/environment';

import { TranslateService } from '@ngx-translate/core';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { LocalStorageService } from '@core/local-storage/local-storage.service';
import { DomSanitizer } from '@angular/platform-browser';
import { MatIconRegistry } from '@angular/material/icon';
import { combineLatest } from 'rxjs';
import { selectIsAuthenticated, selectIsUserLoaded } from '@core/auth/auth.selectors';
import { distinctUntilChanged, filter, map, skip } from 'rxjs/operators';
import { AuthService } from '@core/auth/auth.service';

@Component({
  selector: 'tb-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {

  constructor(private store: Store<AppState>,
              private storageService: LocalStorageService,
              private translate: TranslateService,
              private matIconRegistry: MatIconRegistry,
              private domSanitizer: DomSanitizer,
              private authService: AuthService) {

    console.log(`ThingsBoard Version: ${env.tbVersion}`);

    this.matIconRegistry.addSvgIconSetInNamespace('mdi',
      this.domSanitizer.bypassSecurityTrustResourceUrl('./assets/mdi.svg'));

    this.matIconRegistry.addSvgIconLiteral(
      'google-logo',
      this.domSanitizer.bypassSecurityTrustHtml(
        '<svg viewBox="0 0 48 48"><path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/><path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/><path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/><path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/><path fill="none" d="M0 0h48v48H0z"/></svg>'
      )
    );

    this.matIconRegistry.addSvgIconLiteral(
      'github-logo',
      this.domSanitizer.bypassSecurityTrustHtml(
        '<svg viewBox="0 0 32.7 32.7"><path d="M16.3 0C7.3 0 0 7.3 0 16.3c0 7.2 4.7 13.3 11.1 15.5.8.1 1.1-.4 1.1-.8v-2.8c-4.5 1-5.5-2.2-5.5-2.2-.7-1.9-1.8-2.4-1.8-2.4-1.5-1 .1-1 .1-1 1.6.1 2.5 1.7 2.5 1.7 1.5 2.5 3.8 1.8 4.7 1.4.1-1.1.6-1.8 1-2.2-3.6-.4-7.4-1.8-7.4-8.1 0-1.8.6-3.2 1.7-4.4-.2-.4-.7-2.1.2-4.3 0 0 1.4-.4 4.5 1.7 1.3-.4 2.7-.5 4.1-.5s2.8.2 4.1.5c3.1-2.1 4.5-1.7 4.5-1.7.9 2.2.3 3.9.2 4.3 1 1.1 1.7 2.6 1.7 4.4 0 6.3-3.8 7.6-7.4 8 .6.5 1.1 1.5 1.1 3v4.5c0 .4.3.9 1.1.8 6.5-2.2 11.1-8.3 11.1-15.5C32.6 7.3 25.3 0 16.3 0z" fill="#211c19"/></svg>'
      )
    );

    this.matIconRegistry.addSvgIconLiteral(
      'facebook-logo',
      this.domSanitizer.bypassSecurityTrustHtml(
        '<svg viewBox="0 0 263 263"><path d="M263 131.5C263 58.9 204.1 0 131.5 0S0 58.9 0 131.5c0 65.6 48.1 120 110.9 129.9v-91.9H77.5v-38h33.4v-29c0-33 19.6-51.2 49.7-51.2 14.4 0 29.4 2.6 29.4 2.6v32.4h-16.5c-16.3 0-21.4 10.1-21.4 20.5v24.7h36.4l-5.8 38h-30.6v91.9c62.8-9.9 110.9-64.3 110.9-129.9z" fill="#1877f2"/><path d="M182.7 169.5l5.8-38H152v-24.7c0-10.4 5.1-20.5 21.4-20.5H190V53.9s-15-2.6-29.4-2.6c-30 0-49.7 18.2-49.7 51.2v29H77.5v38h33.4v91.9c6.7 1.1 13.6 1.6 20.5 1.6s13.9-.5 20.5-1.6v-91.9h30.8z" fill="#fff"/></svg>'
      )
    );

    this.matIconRegistry.addSvgIconLiteral(
      'apple-logo',
      this.domSanitizer.bypassSecurityTrustHtml(
        '<svg viewBox="0 0 256 315"><path d="M213.803394,167.030943 C214.2452,214.609646 255.542482,230.442639 256,230.644727 C255.650812,231.761357 249.401383,253.208293 234.24263,275.361446 C221.138555,294.513969 207.538253,313.596333 186.113759,313.991545 C165.062051,314.379442 158.292752,301.507828 134.22469,301.507828 C110.163898,301.507828 102.642899,313.596301 82.7151126,314.379442 C62.0350407,315.16201 46.2873831,293.668525 33.0744079,274.586162 C6.07529317,235.552544 -14.5576169,164.286328 13.147166,116.18047 C26.9103111,92.2909053 51.5060917,77.1630356 78.2026125,76.7751096 C98.5099145,76.3877456 117.677594,90.4371851 130.091705,90.4371851 C142.497945,90.4371851 165.790755,73.5415029 190.277627,76.0228474 C200.528668,76.4495055 229.303509,80.1636878 247.780625,107.209389 C246.291825,108.132333 213.44635,127.253405 213.803394,167.030988 M174.239142,50.1987033 C185.218331,36.9088319 192.607958,18.4081019 190.591988,0 C174.766312,0.636050225 155.629514,10.5457909 144.278109,23.8283506 C134.10507,35.5906758 125.195775,54.4170275 127.599657,72.4607932 C145.239231,73.8255433 163.259413,63.4970262 174.239142,50.1987249" fill="#000000"></path></svg>'
      )
    );

    this.storageService.testLocalStorage();

    this.setupTranslate();
    this.setupAuth();
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
    combineLatest([
      this.store.pipe(select(selectIsAuthenticated)),
      this.store.pipe(select(selectIsUserLoaded))]
    ).pipe(
      map(results => ({isAuthenticated: results[0], isUserLoaded: results[1]})),
      distinctUntilChanged(),
      filter((data) => data.isUserLoaded ),
      skip(1),
    ).subscribe((data) => {
      this.authService.gotoDefaultPlace(data.isAuthenticated);
    });
    this.authService.reloadUser();
  }

  ngOnInit() {
  }

  onActivateComponent($event: any) {
    const loadingElement = $('div#tb-loading-spinner');
    if (loadingElement.length) {
      loadingElement.remove();
    }
  }

}
