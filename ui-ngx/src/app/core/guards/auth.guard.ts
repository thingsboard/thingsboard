///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { Injectable, NgZone } from '@angular/core';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { select, Store } from '@ngrx/store';
import { AppState } from '../core.state';
import { selectAuth } from '../auth/auth.selectors';
import { catchError, map, mergeMap, skipWhile, take } from 'rxjs/operators';
import { AuthState } from '../auth/auth.models';
import { forkJoin, Observable, of } from 'rxjs';
import { enterZone } from '@core/operator/enterZone';
import { Authority } from '@shared/models/authority.enum';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import { isObject } from '@core/utils';
import { MobileService } from '@core/services/mobile.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard  {

  constructor(private store: Store<AppState>,
              private router: Router,
              private authService: AuthService,
              private dialogService: DialogService,
              private utils: UtilsService,
              private translate: TranslateService,
              private mobileService: MobileService,
              private zone: NgZone) {}

  getAuthState(): Observable<AuthState> {
    return this.store.pipe(
      select(selectAuth),
      skipWhile((authState) => !authState || !authState.isUserLoaded),
      take(1),
      enterZone(this.zone)
    );
  }

  canActivate(next: ActivatedRouteSnapshot,
              state: RouterStateSnapshot) {

    return this.getAuthState().pipe(
      mergeMap((authState) => {
        const url: string = state.url;

        let lastChild = state.root;
        const urlSegments: string[] = [];
        if (lastChild.url) {
          urlSegments.push(...lastChild.url.map(segment => segment.path));
        }
        while (lastChild.children.length) {
          lastChild = lastChild.children[0];
          if (lastChild.url) {
            urlSegments.push(...lastChild.url.map(segment => segment.path));
          }
        }
        const path = urlSegments.join('.');
        const publicId = this.utils.getQueryParam('publicId');
        const data = lastChild.data || {};
        const params = lastChild.params || {};
        const isPublic = data.module === 'public';

        if (!authState.isAuthenticated || isPublic) {
          if (publicId && publicId.length > 0) {
            this.authService.setUserFromJwtToken(null, null, false);
            this.authService.reloadUser();
            return of(false);
          } else if (!isPublic) {
            this.authService.redirectUrl = url;
            // this.authService.gotoDefaultPlace(false);
            return of(this.authService.defaultUrl(false));
          } else {
            if (path === 'login') {
              return forkJoin([this.authService.loadOAuth2Clients()]).pipe(
                map(() => {
                  return true;
                })
              );
            } else if (path === 'login.mfa') {
              if (authState.authUser?.authority === Authority.PRE_VERIFICATION_TOKEN) {
                return this.authService.getAvailableTwoFaLoginProviders().pipe(
                  map(() => {
                    return true;
                  })
                );
              }
              this.authService.logout();
              return of(this.authService.defaultUrl(false));
            } else if (path === 'login.force-mfa') {
              if (authState.authUser?.authority === Authority.MFA_CONFIGURATION_TOKEN) {
                return this.authService.getAvailableTwoFaProviders().pipe(
                  map(() => {
                    return true;
                  })
                );
              }
              this.authService.logout();
              return of(this.authService.defaultUrl(false));
            } else {
              return of(true);
            }
          }
        } else {
          if (authState.authUser.isPublic) {
            if (this.authService.parsePublicId() !== publicId) {
              if (publicId && publicId.length > 0) {
                this.authService.setUserFromJwtToken(null, null, false);
                this.authService.reloadUser();
              } else {
                this.authService.logout();
              }
              return of(false);
            }
          }
          if (this.mobileService.isMobileApp() && !path.startsWith('dashboard.')) {
            this.mobileService.handleMobileNavigation(path, params);
            return of(false);
          }
          if (authState.authUser.authority === Authority.PRE_VERIFICATION_TOKEN) {
            this.authService.logout();
            return of(false);
          }
          const defaultUrl = this.authService.defaultUrl(true, authState, path, params);
          if (defaultUrl) {
            // this.authService.gotoDefaultPlace(true);
            return of(defaultUrl);
          } else {
            const authority = Authority[authState.authUser.authority];
            if (data.auth && data.auth.indexOf(authority) === -1) {
              this.dialogService.forbidden();
              return of(false);
            } else if (data.redirectTo) {
              let redirect;
              if (isObject(data.redirectTo)) {
                redirect = data.redirectTo[authority];
              } else {
                redirect = data.redirectTo;
              }
              return of(this.router.parseUrl(redirect));
            } else {
              return of(true);
            }
          }
        }
      }),
      catchError((err => { console.error(err); return of(false); } ))
    );
  }

  canActivateChild(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot) {
    return this.canActivate(route, state);
  }
}
