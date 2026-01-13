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

import { Injectable, NgZone } from '@angular/core';
import { JwtHelperService } from '@auth0/angular-jwt';
import { HttpClient } from '@angular/common/http';

import { Observable, of, ReplaySubject, throwError } from 'rxjs';
import { catchError, map, mergeMap, tap } from 'rxjs/operators';

import { LoginRequest, LoginResponse, PublicLoginRequest } from '@shared/models/login.models';
import { Router, UrlTree } from '@angular/router';
import { defaultHttpOptions, defaultHttpOptionsFromConfig, RequestConfig } from '../http/http-utils';
import { UserService } from '../http/user.service';
import { Store } from '@ngrx/store';
import { AppState } from '../core.state';
import {
  ActionAuthAuthenticated,
  ActionAuthLoadUser,
  ActionAuthUnauthenticated,
  ActionAuthUpdateAuthUser
} from './auth.actions';
import { getCurrentAuthState, getCurrentAuthUser } from './auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { AuthPayload, AuthState, SysParams, SysParamsState } from '@core/auth/auth.models';
import { TranslateService } from '@ngx-translate/core';
import { AuthUser } from '@shared/models/user.model';
import { TimeService } from '@core/services/time.service';
import { UtilsService } from '@core/services/utils.service';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { AlertDialogComponent } from '@shared/components/dialog/alert-dialog.component';
import { OAuth2ClientLoginInfo, PlatformType } from '@shared/models/oauth2.models';
import { isMobileApp } from '@core/utils';
import { TwoFactorAuthProviderType, TwoFaProviderInfo } from '@shared/models/two-factor-auth.models';
import { UserPasswordPolicy } from '@shared/models/settings.models';

@Injectable({
    providedIn: 'root'
})
export class AuthService {

  constructor(
    private store: Store<AppState>,
    private http: HttpClient,
    private userService: UserService,
    private timeService: TimeService,
    private router: Router,
    private zone: NgZone,
    private utils: UtilsService,
    private translate: TranslateService,
    private dialog: MatDialog
  ) {
  }

  redirectUrl: string;
  oauth2Clients: Array<OAuth2ClientLoginInfo> = null;
  twoFactorAuthProviders: Array<TwoFaProviderInfo> = null;

  private refreshTokenSubject: ReplaySubject<LoginResponse> = null;
  private jwtHelper = new JwtHelperService();

  private static _storeGet(key) {
    return localStorage.getItem(key);
  }

  private static isTokenValid(prefix) {
    const clientExpiration = AuthService._storeGet(prefix + '_expiration');
    return clientExpiration && Number(clientExpiration) > (new Date().valueOf() + 2000);
  }

  public static isJwtTokenValid() {
    return AuthService.isTokenValid('jwt_token');
  }

  private static clearTokenData() {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('jwt_token_expiration');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('refresh_token_expiration');
  }

  public static getJwtToken() {
    return AuthService._storeGet('jwt_token');
  }

  public reloadUser() {
    this.loadUser(true).subscribe(
      (authPayload) => {
        this.notifyAuthenticated(authPayload);
        this.notifyUserLoaded(true);
      },
      () => {
        this.notifyUnauthenticated();
        this.notifyUserLoaded(true);
      }
    );
  }


  public login(loginRequest: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', loginRequest, defaultHttpOptions()).pipe(
      tap((loginResponse: LoginResponse) => {
          this.setUserFromJwtToken(loginResponse.token, loginResponse.refreshToken, true);
          if (loginResponse.scope === Authority.PRE_VERIFICATION_TOKEN) {
            this.router.navigateByUrl(`login/mfa`);
          }
        }
      ));
  }

  public checkTwoFaVerificationCode(providerType: TwoFactorAuthProviderType, verificationCode: number): Observable<LoginResponse> {
    return this.http.post<LoginResponse>
    (`/api/auth/2fa/verification/check?providerType=${providerType}&verificationCode=${verificationCode}`,
      null, defaultHttpOptions(false, true)).pipe(
      tap((loginResponse: LoginResponse) => {
          this.setUserFromJwtToken(loginResponse.token, loginResponse.refreshToken, true);
        }
      ));
  }

  public publicLogin(publicId: string): Observable<LoginResponse> {
    const publicLoginRequest: PublicLoginRequest = {
      publicId
    };
    return this.http.post<LoginResponse>('/api/auth/login/public', publicLoginRequest, defaultHttpOptions());
  }

  public sendResetPasswordLink(email: string) {
    return this.http.post('/api/noauth/resetPasswordByEmail',
      {email}, defaultHttpOptions());
  }

  public activate(activateToken: string, password: string, sendActivationMail: boolean): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`/api/noauth/activate?sendActivationMail=${sendActivationMail}`,
      {activateToken, password}, defaultHttpOptions()).pipe(
      tap((loginResponse: LoginResponse) => {
          this.setUserFromJwtToken(loginResponse.token, loginResponse.refreshToken, true);
        }
      ));
  }

  public resetPassword(resetToken: string, password: string): Observable<void> {
    return this.http.post<void>('/api/noauth/resetPassword', {resetToken, password}, defaultHttpOptions());
  }

  public changePassword(currentPassword: string, newPassword: string, config?: RequestConfig) {
    return this.http.post('/api/auth/changePassword', {currentPassword, newPassword}, defaultHttpOptionsFromConfig(config)).pipe(
      tap((loginResponse: LoginResponse) => {
          this.setUserFromJwtToken(loginResponse.token, loginResponse.refreshToken, false);
        }
      ));
  }

  public getUserPasswordPolicy() {
    return this.http.get<UserPasswordPolicy>(`/api/noauth/userPasswordPolicy`, defaultHttpOptions());
  }

  public activateByEmailCode(emailCode: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`/api/noauth/activateByEmailCode?emailCode=${emailCode}`,
      null, defaultHttpOptions());
  }

  public resendEmailActivation(email: string) {
    const encodeEmail = encodeURIComponent(email);
    return this.http.post(`/api/noauth/resendEmailActivation?email=${encodeEmail}`,
      null, defaultHttpOptions());
  }

  public loginAsUser(userId: string) {
    return this.http.get<LoginResponse>(`/api/user/${userId}/token`, defaultHttpOptions()).pipe(
      tap((loginResponse: LoginResponse) => {
          this.setUserFromJwtToken(loginResponse.token, loginResponse.refreshToken, true);
        }
      ));
  }

  public logout(captureLastUrl: boolean = false, ignoreRequest = false) {
    if (captureLastUrl) {
      this.redirectUrl = this.router.url;
    }
    if (!ignoreRequest) {
      this.http.post('/api/auth/logout', null, defaultHttpOptions(true, true))
        .subscribe(() => {
            this.clearJwtToken();
          },
          () => {
            this.clearJwtToken();
          }
        );
    } else {
      this.clearJwtToken();
    }
  }

  private notifyUserLoaded(isUserLoaded: boolean) {
    this.store.dispatch(new ActionAuthLoadUser({isUserLoaded}));
  }

  public gotoDefaultPlace(isAuthenticated: boolean) {
    if (!isMobileApp()) {
      const authState = getCurrentAuthState(this.store);
      const url = this.defaultUrl(isAuthenticated, authState);
      this.zone.run(() => {
        this.router.navigateByUrl(url);
      });
    }
  }

  public loadOAuth2Clients(): Observable<Array<OAuth2ClientLoginInfo>> {
    const url = '/api/noauth/oauth2Clients?platform=' + PlatformType.WEB;
    return this.http.post<Array<OAuth2ClientLoginInfo>>(url,
      null, defaultHttpOptions()).pipe(
      catchError(err => of([])),
      tap((OAuth2Clients) => {
        this.oauth2Clients = OAuth2Clients;
      })
    );
  }

  public getAvailableTwoFaLoginProviders(): Observable<Array<TwoFaProviderInfo>> {
    return this.http.get<Array<TwoFaProviderInfo>>(`/api/auth/2fa/providers`, defaultHttpOptions()).pipe(
      catchError(() => of([])),
      tap((providers) => {
        this.twoFactorAuthProviders = providers;
      })
    );
  }

  public forceDefaultPlace(authState?: AuthState, path?: string, params?: any): boolean {
    if (authState && authState.authUser) {
      if (authState.authUser.authority === Authority.TENANT_ADMIN || authState.authUser.authority === Authority.CUSTOMER_USER) {
        if ((this.userHasDefaultDashboard(authState) && authState.forceFullscreen) || authState.authUser.isPublic) {
          if (path.startsWith('account')) {
            if (this.userHasProfile(authState.authUser)) {
              return false;
            } else {
              return true;
            }
          } else if (path.startsWith('dashboard.') || path.startsWith('dashboards.') &&
              authState.allowedDashboardIds.indexOf(params.dashboardId) > -1) {
            return false;
          } else {
            return true;
          }
        }
      }
    }
    return false;
  }

  public defaultUrl(isAuthenticated: boolean, authState?: AuthState, path?: string, params?: any): UrlTree {
    let result: UrlTree = null;
    if (isAuthenticated) {
      if (authState.authUser.authority === Authority.PRE_VERIFICATION_TOKEN) {
        result = this.router.parseUrl('login/mfa');
      } else if (!path || path === 'login' || this.forceDefaultPlace(authState, path, params)) {
        if (this.redirectUrl) {
          const redirectUrl = this.redirectUrl;
          this.redirectUrl = null;
          result = this.router.parseUrl(redirectUrl);
        } else {
          result = this.router.parseUrl('home');
        }
        if (authState.authUser.authority === Authority.TENANT_ADMIN || authState.authUser.authority === Authority.CUSTOMER_USER) {
          if (this.userHasDefaultDashboard(authState)) {
            const dashboardId = authState.userDetails.additionalInfo.defaultDashboardId;
            if (authState.forceFullscreen) {
              result = this.router.parseUrl(`dashboard/${dashboardId}`);
            } else {
              result = this.router.parseUrl(`dashboards/${dashboardId}`);
            }
          } else if (authState.authUser.isPublic) {
            result = this.router.parseUrl(`dashboard/${authState.lastPublicDashboardId}`);
          }
        }
      }
    } else {
      result = this.router.parseUrl('login');
    }
    return result;
  }

  private loadUser(doTokenRefresh): Observable<AuthPayload> {
    const authUser = getCurrentAuthUser(this.store);
    if (!authUser) {
      const publicId = this.utils.getQueryParam('publicId');
      const accessToken = this.utils.getQueryParam('accessToken');
      const refreshToken = this.utils.getQueryParam('refreshToken');
      const username = this.utils.getQueryParam('username');
      const password = this.utils.getQueryParam('password');
      const loginError = this.utils.getQueryParam('loginError');
      if (publicId) {
        return this.publicLogin(publicId).pipe(
          mergeMap((response) => {
            this.updateAndValidateTokens(response.token, response.refreshToken, false);
            return this.procceedJwtTokenValidate();
          }),
          catchError((err) => {
            this.utils.updateQueryParam('publicId', null);
            throw Error();
          })
        );
      } else if (accessToken) {
        const queryParamsToRemove = ['accessToken'];
        if (refreshToken) {
          queryParamsToRemove.push('refreshToken');
        }
        this.utils.removeQueryParams(queryParamsToRemove);
        try {
          this.updateAndValidateToken(accessToken, 'jwt_token', false);
          if (refreshToken) {
            this.updateAndValidateToken(refreshToken, 'refresh_token', false);
          } else {
            localStorage.removeItem('refresh_token');
            localStorage.removeItem('refresh_token_expiration');
          }
        } catch (e) {
          return throwError(e);
        }
        return this.procceedJwtTokenValidate();
      } else if (username && password) {
        this.utils.updateQueryParam('username', null);
        this.utils.updateQueryParam('password', null);
        const loginRequest: LoginRequest = {
          username,
          password
        };
        return this.http.post<LoginResponse>('/api/auth/login', loginRequest, defaultHttpOptions()).pipe(
          mergeMap((loginResponse: LoginResponse) => {
              this.updateAndValidateTokens(loginResponse.token, loginResponse.refreshToken, false);
              return this.procceedJwtTokenValidate();
            }
          )
        );
      } else if (loginError) {
        Promise.resolve().then(() => this.showLoginErrorDialog(loginError));
        this.utils.updateQueryParam('loginError', null);
        return throwError(Error());
      }
      return this.procceedJwtTokenValidate(doTokenRefresh);
    } else {
      return of({} as AuthPayload);
    }
  }

  private showLoginErrorDialog(loginError: string) {
    this.translate.get(['login.error', 'action.close']).subscribe(
      (translations) => {
        const dialogConfig: MatDialogConfig = {
          disableClose: true,
          data: {
            title: translations['login.error'],
            message: loginError,
            ok: translations['action.close'],
            textMode: true
          }
        };
        this.dialog.open(AlertDialogComponent, dialogConfig);
      }
    );
  }

  private procceedJwtTokenValidate(doTokenRefresh?: boolean): Observable<AuthPayload> {
    const loadUserSubject = new ReplaySubject<AuthPayload>();
    this.validateJwtToken(doTokenRefresh).subscribe(
      () => {
        let authPayload = {} as AuthPayload;
        const jwtToken = AuthService._storeGet('jwt_token');
        authPayload.authUser = this.jwtHelper.decodeToken(jwtToken);
        if (authPayload.authUser && authPayload.authUser.scopes && authPayload.authUser.scopes.length) {
          authPayload.authUser.authority = Authority[authPayload.authUser.scopes[0]];
        } else if (authPayload.authUser) {
          authPayload.authUser.authority = Authority.ANONYMOUS;
        }
        if (authPayload.authUser?.isPublic) {
          authPayload.forceFullscreen = true;
        }
        if (authPayload.authUser?.isPublic) {
          this.loadSystemParams().subscribe(
            (sysParams) => {
              authPayload = {...authPayload, ...sysParams};
              loadUserSubject.next(authPayload);
              loadUserSubject.complete();
            },
            (err) => {
              loadUserSubject.error(err);
            }
          );
        } else if (authPayload.authUser?.authority === Authority.PRE_VERIFICATION_TOKEN) {
          loadUserSubject.next(authPayload);
          loadUserSubject.complete();
        } else if (authPayload.authUser?.userId) {
          this.userService.getUser(authPayload.authUser.userId).subscribe(
            (user) => {
              authPayload.userDetails = user;
              authPayload.forceFullscreen = false;
              if (this.userForceFullscreen(authPayload)) {
                authPayload.forceFullscreen = true;
              }
              this.loadSystemParams().subscribe(
                (sysParams) => {
                  authPayload = {...authPayload, ...sysParams};
                  loadUserSubject.next(authPayload);
                  loadUserSubject.complete();
                },
                (err) => {
                  loadUserSubject.error(err);
                  this.logout();
                });
            },
            (err) => {
              loadUserSubject.error(err);
              this.logout();
            }
          );
        } else {
          loadUserSubject.error(null);
        }
      },
      (err) => {
        loadUserSubject.error(err);
      }
    );
    return loadUserSubject;
  }

  private loadSystemParams(): Observable<SysParamsState> {
    return this.http.get<SysParams>('/api/system/params', defaultHttpOptions()).pipe(
      map((sysParams) => {
        this.timeService.setMaxDatapointsLimit(sysParams.maxDatapointsLimit);
        return sysParams;
      }),
      catchError(() => of({} as SysParamsState))
    );
  }

  public refreshJwtToken(loadUserElseStoreJwtToken = true): Observable<LoginResponse> {
    let response: Observable<LoginResponse> = this.refreshTokenSubject;
    if (this.refreshTokenSubject === null) {
        this.refreshTokenSubject = new ReplaySubject<LoginResponse>(1);
        response = this.refreshTokenSubject;
        const refreshToken = AuthService._storeGet('refresh_token');
        const refreshTokenValid = AuthService.isTokenValid('refresh_token');
        this.setUserFromJwtToken(null, null, false);
        if (!refreshTokenValid) {
          this.translate.get('access.refresh-token-expired').subscribe(
            (translation) => {
              this.refreshTokenSubject.error(new Error(translation));
              this.refreshTokenSubject = null;
            }
          );
        } else {
          const refreshTokenRequest = {
            refreshToken
          };
          const refreshObservable = this.http.post<LoginResponse>('/api/auth/token', refreshTokenRequest, defaultHttpOptions());
          refreshObservable.subscribe((loginResponse: LoginResponse) => {
            if (loadUserElseStoreJwtToken) {
              this.setUserFromJwtToken(loginResponse.token, loginResponse.refreshToken, false);
            } else {
              this.updateAndValidateTokens(loginResponse.token, loginResponse.refreshToken, true);
            }
            this.updatedAuthUserFromToken(loginResponse.token);
            this.refreshTokenSubject.next(loginResponse);
            this.refreshTokenSubject.complete();
            this.refreshTokenSubject = null;
          }, () => {
            this.clearJwtToken();
            this.refreshTokenSubject.error(new Error(this.translate.instant('access.refresh-token-failed')));
            this.refreshTokenSubject = null;
          });
        }
    }
    return response;
  }

  private updatedAuthUserFromToken(token: string) {
    const authUser = getCurrentAuthUser(this.store);
    const tokenData = this.jwtHelper.decodeToken(token);
    if (authUser && tokenData && ['sub', 'firstName', 'lastName'].some(value => authUser[value] !== tokenData[value])) {
      this.store.dispatch(new ActionAuthUpdateAuthUser({
        sub: tokenData.sub,
        firstName: tokenData.firstName,
        lastName: tokenData.lastName,
      }));
    }
  }

  private validateJwtToken(doRefresh): Observable<void> {
    const subject = new ReplaySubject<void>();
    if (!AuthService.isTokenValid('jwt_token')) {
      if (doRefresh) {
        this.refreshJwtToken(!doRefresh).subscribe(
          () => {
            subject.next();
            subject.complete();
          },
          (err) => {
            subject.error(err);
          }
        );
      } else {
        this.clearJwtToken();
        subject.error(null);
      }
    } else {
      subject.next();
      subject.complete();
    }
    return subject;
  }

  public refreshTokenPending() {
    return this.refreshTokenSubject !== null;
  }

  public setUserFromJwtToken(jwtToken, refreshToken, notify): Observable<boolean> {
    const authenticatedSubject = new ReplaySubject<boolean>();
    if (!jwtToken) {
      AuthService.clearTokenData();
      if (notify) {
        this.notifyUnauthenticated();
      }
      authenticatedSubject.next(false);
      authenticatedSubject.complete();
    } else {
      this.updateAndValidateTokens(jwtToken, refreshToken, true);
      if (notify) {
        this.notifyUserLoaded(false);
        this.loadUser(false).subscribe(
          (authPayload) => {
            this.notifyAuthenticated(authPayload);
            this.notifyUserLoaded(true);
            authenticatedSubject.next(true);
            authenticatedSubject.complete();
          },
          () => {
            this.notifyUnauthenticated();
            this.notifyUserLoaded(true);
            authenticatedSubject.next(false);
            authenticatedSubject.complete();
          }
        );
      } else {
        this.loadUser(false).subscribe(
          () => {
            authenticatedSubject.next(true);
            authenticatedSubject.complete();
          },
          () => {
            authenticatedSubject.next(false);
            authenticatedSubject.complete();
          }
        );
      }
    }
    return authenticatedSubject;
  }

  private updateAndValidateTokens(jwtToken, refreshToken, notify: boolean) {
    this.updateAndValidateToken(jwtToken, 'jwt_token', notify);
    this.updateAndValidateToken(refreshToken, 'refresh_token', notify);
  }

  public parsePublicId(): string {
    const token = AuthService.getJwtToken();
    if (token) {
      const tokenData = this.jwtHelper.decodeToken(token);
      if (tokenData && tokenData.isPublic) {
        return tokenData.sub;
      }
    }
    return null;
  }

  private notifyUnauthenticated() {
    this.store.dispatch(new ActionAuthUnauthenticated());
  }

  private notifyAuthenticated(authPayload: AuthPayload) {
    this.store.dispatch(new ActionAuthAuthenticated(authPayload));
  }

  private updateAndValidateToken(token, prefix, notify) {
    let valid = false;
    const tokenData = this.jwtHelper.decodeToken(token);
    const issuedAt = tokenData?.iat;
    const expTime = tokenData?.exp;
    if (issuedAt && expTime) {
      const ttl = expTime - issuedAt;
      if (ttl > 0) {
        const clientExpiration = new Date().valueOf() + ttl * 1000;
        localStorage.setItem(prefix, token);
        localStorage.setItem(prefix + '_expiration', '' + clientExpiration);
        valid = true;
      }
    }
    if (!valid && notify) {
      this.notifyUnauthenticated();
    }
  }

  private clearJwtToken() {
    this.setUserFromJwtToken(null, null, true);
  }

  private userForceFullscreen(authPayload: AuthPayload): boolean {
    return (authPayload.authUser && authPayload.authUser.isPublic) ||
      (authPayload.userDetails && authPayload.userDetails.additionalInfo &&
        authPayload.userDetails.additionalInfo.defaultDashboardId &&
        authPayload.userDetails.additionalInfo.defaultDashboardFullscreen &&
        authPayload.userDetails.additionalInfo.defaultDashboardFullscreen === true);
  }

  private userHasProfile(authUser: AuthUser): boolean {
    return authUser && !authUser.isPublic;
  }

  private userHasDefaultDashboard(authState: AuthState): boolean {
    if (authState && authState.userDetails && authState.userDetails.additionalInfo
      && authState.userDetails.additionalInfo.defaultDashboardId) {
      return true;
    } else {
      return false;
    }
  }

}
