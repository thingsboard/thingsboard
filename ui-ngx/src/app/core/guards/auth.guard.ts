// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { SelfRegistrationService } from '@core/http/self-register.service';
import { isDefined, isObject } from '@core/utils';
import { MenuService } from '@core/services/menu.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { MobileService } from '@core/services/mobile.service';
import { DashboardReportService } from '@core/http/dashboard-report.service';

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
              private whiteLabelingService: WhiteLabelingService,
              private selfRegistrationService: SelfRegistrationService,
              private userPermissionsService: UserPermissionsService,
              private menuService: MenuService,
              private mobileService: MobileService,
              private reportService: DashboardReportService,
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
        const publicId = this.utils.getQueryParam('publicId') || (this.reportService.reportView ? this.reportService.publicId : null);
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
          } else if (path === 'login.mfa' && authState.authUser?.authority !== Authority.PRE_VERIFICATION_TOKEN) {
            if (authState.isAuthenticated) {
              this.authService.logout();
            }
            return of(this.authService.defaultUrl(false));
          } else {
            const tasks: Observable<any>[] = [];
            tasks.push(this.whiteLabelingService.loadLoginWhiteLabelingParams());
            if (path === 'login' || path === 'signup' || path === 'signup.recaptcha') {
              tasks.push(this.selfRegistrationService.loadSelfRegistrationParams());
              if (path === 'login') {
                tasks.push(this.authService.loadOAuth2Clients());
              }
            }
            if (path === 'login.mfa') {
              tasks.push(this.authService.getAvailableTwoFaLoginProviders());
            }
            if (path === 'login.force-mfa') {
              tasks.push(this.authService.getAvailableTwoFaProviders());
            }
            return forkJoin(tasks).pipe(
              map(() => {
                if (path === 'signup' && !this.selfRegistrationService.signUpParams.activate) {
                  return this.authService.defaultUrl(false);
                } else if (path === 'login.mfa' && !this.authService.twoFactorAuthProviders) {
                  this.authService.logout();
                  return this.authService.defaultUrl(false);
                } else {
                  return true;
                }
              })
            );
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
            this.mobileService.handleMobileNavigation(path, params, lastChild.queryParams);
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
            if (authState.authUser.isPublic) {
              if (this.authService.forceDefaultPlace(authState, path, params)) {
                this.dialogService.forbidden();
                return of(false);
              }
            }
            const authority = Authority[authState.authUser.authority];
            if (data.auth && data.auth.indexOf(authority) === -1) {
              this.dialogService.forbidden();
              return of(false);
            } else if (isDefined(data.canActivate$)) {
              return (data.canActivate$ as (userPermissionsService: UserPermissionsService, params: any) => Observable<boolean>)(this.userPermissionsService, params).pipe(
                catchError(() => of(false)),
                map((allow) => {
                  if (!allow) {
                    this.dialogService.forbidden();
                  }
                  return allow;
                })
              );
            } else if (isDefined(data.canActivate) && !data.canActivate(this.userPermissionsService)) {
              this.dialogService.forbidden();
              return of(false);
            } else if (data.redirectTo) {
              let redirect;
              if (isObject(data.redirectTo)) {
                redirect = data.redirectTo[authority];
              } else {
                redirect = data.redirectTo;
              }
              return this.menuService.getRedirectPath(path, redirect).pipe(
                map((redirectPath) => {
                  return this.router.parseUrl(redirectPath);
                })
              );
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
