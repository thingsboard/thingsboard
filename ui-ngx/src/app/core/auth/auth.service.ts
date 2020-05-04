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

import { Injectable, NgZone } from '@angular/core';
import { JwtHelperService } from '@auth0/angular-jwt';
import { HttpClient } from '@angular/common/http';

import { forkJoin, Observable, of, throwError } from 'rxjs';
import { catchError, map, mergeMap, tap } from 'rxjs/operators';

import { LoginRequest, LoginResponse, OAuth2Client, PublicLoginRequest } from '@shared/models/login.models';
import { ActivatedRoute, Router, UrlTree } from '@angular/router';
import { defaultHttpOptions } from '../http/http-utils';
import { ReplaySubject } from 'rxjs/internal/ReplaySubject';
import { UserService } from '../http/user.service';
import { Store } from '@ngrx/store';
import { AppState } from '../core.state';
import { ActionAuthAuthenticated, ActionAuthLoadUser, ActionAuthUnauthenticated } from './auth.actions';
import { getCurrentAuthState, getCurrentAuthUser } from './auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { ActionSettingsChangeLanguage } from '@app/core/settings/settings.actions';
import { AuthPayload, AuthState } from '@core/auth/auth.models';
import { TranslateService } from '@ngx-translate/core';
import { AuthUser } from '@shared/models/user.model';
import { TimeService } from '@core/services/time.service';
import { UtilsService } from '@core/services/utils.service';
import { DashboardService } from '@core/http/dashboard.service';
import { PageLink } from '@shared/models/page/page-link';
import { DashboardInfo } from '@shared/models/dashboard.models';
import { PageData } from '@app/shared/models/page/page-data';
import { AdminService } from '@core/http/admin.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';

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
    private route: ActivatedRoute,
    private zone: NgZone,
    private utils: UtilsService,
    private dashboardService: DashboardService,
    private adminService: AdminService,
    private translate: TranslateService
  ) {
  }

  redirectUrl: string;
  oauth2Clients: Array<OAuth2Client> = null;

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

  public resetPassword(resetToken: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/noauth/resetPassword', {resetToken, password}, defaultHttpOptions()).pipe(
      tap((loginResponse: LoginResponse) => {
          this.setUserFromJwtToken(loginResponse.token, loginResponse.refreshToken, true);
        }
      ));
  }

  public changePassword(currentPassword: string, newPassword: string) {
    return this.http.post('/api/auth/changePassword',
      {currentPassword, newPassword}, defaultHttpOptions());
  }

  public activateByEmailCode(emailCode: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`/api/noauth/activateByEmailCode?emailCode=${emailCode}`,
      null, defaultHttpOptions());
  }

  public resendEmailActivation(email: string) {
    return this.http.post(`/api/noauth/resendEmailActivation?email=${email}`,
      null, defaultHttpOptions());
  }

  public loginAsUser(userId: string) {
    return this.http.get<LoginResponse>(`/api/user/${userId}/token`, defaultHttpOptions()).pipe(
      tap((loginResponse: LoginResponse) => {
          this.setUserFromJwtToken(loginResponse.token, loginResponse.refreshToken, true);
        }
      ));
  }

  public logout(captureLastUrl: boolean = false) {
    if (captureLastUrl) {
      this.redirectUrl = this.router.url;
    }
    this.http.post('/api/auth/logout', null, defaultHttpOptions(true, true))
      .subscribe(() => {
          this.clearJwtToken();
        },
        () => {
          this.clearJwtToken();
        }
      );
  }

  private notifyUserLoaded(isUserLoaded: boolean) {
    this.store.dispatch(new ActionAuthLoadUser({isUserLoaded}));
  }

  public gotoDefaultPlace(isAuthenticated: boolean) {
    const authState = getCurrentAuthState(this.store);
    const url = this.defaultUrl(isAuthenticated, authState);
    this.zone.run(() => {
      this.router.navigateByUrl(url);
    });
  }

  public loadOAuth2Clients(): Observable<Array<OAuth2Client>> {
    return this.http.post<Array<OAuth2Client>>(`/api/noauth/oauth2Clients`,
      null, defaultHttpOptions()).pipe(
        tap((OAuth2Clients) => {
          this.oauth2Clients = OAuth2Clients;
        })
      );
  }

  private forceDefaultPlace(authState?: AuthState, path?: string, params?: any): boolean {
    if (authState && authState.authUser) {
      if (authState.authUser.authority === Authority.TENANT_ADMIN || authState.authUser.authority === Authority.CUSTOMER_USER) {
        if ((this.userHasDefaultDashboard(authState) && authState.forceFullscreen) || authState.authUser.isPublic) {
          if (path === 'profile') {
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
      if (!path || path === 'login' || this.forceDefaultPlace(authState, path, params)) {
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
        } else if (authState.authUser.authority === Authority.SYS_ADMIN) {
          this.adminService.checkUpdates().subscribe((updateMessage) => {
            if (updateMessage && updateMessage.updateAvailable) {
              this.store.dispatch(new ActionNotificationShow(
                {message: updateMessage.message,
                           type: 'info',
                           verticalPosition: 'bottom',
                           horizontalPosition: 'right'}));
            }
          });
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
      if (publicId) {
        return this.publicLogin(publicId).pipe(
          mergeMap((response) => {
            this.updateAndValidateToken(response.token, 'jwt_token', false);
            this.updateAndValidateToken(response.refreshToken, 'refresh_token', false);
            return this.procceedJwtTokenValidate();
          }),
          catchError((err) => {
            this.utils.updateQueryParam('publicId', null);
            throw Error();
          })
        );
      } else if (accessToken) {
        this.utils.updateQueryParam('accessToken', null);
        if (refreshToken) {
          this.utils.updateQueryParam('refreshToken', null);
        }
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
              this.updateAndValidateToken(loginResponse.token, 'jwt_token', false);
              this.updateAndValidateToken(loginResponse.refreshToken, 'refresh_token', false);
              return this.procceedJwtTokenValidate();
            }
          )
        );
      }
      return this.procceedJwtTokenValidate(doTokenRefresh);
    } else {
      return of({} as AuthPayload);
    }
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
        if (authPayload.authUser.isPublic) {
          authPayload.forceFullscreen = true;
        }
        if (authPayload.authUser.isPublic) {
          this.loadSystemParams(authPayload).subscribe(
            (sysParams) => {
              authPayload = {...authPayload, ...sysParams};
              loadUserSubject.next(authPayload);
              loadUserSubject.complete();
            },
            (err) => {
              loadUserSubject.error(err);
            }
          );
        } else if (authPayload.authUser.userId) {
          this.userService.getUser(authPayload.authUser.userId).subscribe(
            (user) => {
              authPayload.userDetails = user;
              authPayload.forceFullscreen = false;
              if (this.userForceFullscreen(authPayload)) {
                authPayload.forceFullscreen = true;
              }
              this.loadSystemParams(authPayload).subscribe(
                (sysParams) => {
                  authPayload = {...authPayload, ...sysParams};
                  let userLang;
                  if (authPayload.userDetails.additionalInfo && authPayload.userDetails.additionalInfo.lang) {
                    userLang = authPayload.userDetails.additionalInfo.lang;
                  } else {
                    userLang = null;
                  }
                  this.notifyUserLang(userLang);
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

  private loadIsUserTokenAccessEnabled(authUser: AuthUser): Observable<boolean> {
    if (authUser.authority === Authority.SYS_ADMIN ||
        authUser.authority === Authority.TENANT_ADMIN) {
      return this.http.get<boolean>('/api/user/tokenAccessEnabled', defaultHttpOptions());
    } else {
      return of(false);
    }
  }

  private loadSystemParams(authPayload: AuthPayload): Observable<any> {
    const sources: Array<Observable<any>> = [this.loadIsUserTokenAccessEnabled(authPayload.authUser),
                                             this.fetchAllowedDashboardIds(authPayload),
                                             this.timeService.loadMaxDatapointsLimit()];
    return forkJoin(sources)
      .pipe(map((data) => {
        const userTokenAccessEnabled: boolean = data[0];
        const allowedDashboardIds: string[] = data[1];
        return {userTokenAccessEnabled, allowedDashboardIds};
      }));
  }

  public refreshJwtToken(): Observable<LoginResponse> {
    let response: Observable<LoginResponse> = this.refreshTokenSubject;
    if (this.refreshTokenSubject === null) {
        this.refreshTokenSubject = new ReplaySubject<LoginResponse>(1);
        response = this.refreshTokenSubject;
        const refreshToken = AuthService._storeGet('refresh_token');
        const refreshTokenValid = AuthService.isTokenValid('refresh_token');
        this.setUserFromJwtToken(null, null, false);
        if (!refreshTokenValid) {
          this.refreshTokenSubject.error(new Error(this.translate.instant('access.refresh-token-expired')));
          this.refreshTokenSubject = null;
        } else {
          const refreshTokenRequest = {
            refreshToken
          };
          const refreshObservable = this.http.post<LoginResponse>('/api/auth/token', refreshTokenRequest, defaultHttpOptions());
          refreshObservable.subscribe((loginResponse: LoginResponse) => {
            this.setUserFromJwtToken(loginResponse.token, loginResponse.refreshToken, false);
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

  private validateJwtToken(doRefresh): Observable<void> {
    const subject = new ReplaySubject<void>();
    if (!AuthService.isTokenValid('jwt_token')) {
      if (doRefresh) {
        this.refreshJwtToken().subscribe(
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

  public setUserFromJwtToken(jwtToken, refreshToken, notify) {
    if (!jwtToken) {
      AuthService.clearTokenData();
      if (notify) {
        this.notifyUnauthenticated();
      }
    } else {
      this.updateAndValidateToken(jwtToken, 'jwt_token', true);
      this.updateAndValidateToken(refreshToken, 'refresh_token', true);
      if (notify) {
        this.notifyUserLoaded(false);
        this.loadUser(false).subscribe(
          (authPayload) => {
            this.notifyUserLoaded(true);
            this.notifyAuthenticated(authPayload);
          },
          () => {
            this.notifyUserLoaded(true);
            this.notifyUnauthenticated();
          }
        );
      } else {
        this.loadUser(false).subscribe();
      }
    }
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

  private notifyUserLang(userLang: string) {
    this.store.dispatch(new ActionSettingsChangeLanguage({userLang}));
  }

  private updateAndValidateToken(token, prefix, notify) {
    let valid = false;
    const tokenData = this.jwtHelper.decodeToken(token);
    const issuedAt = tokenData.iat;
    const expTime = tokenData.exp;
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

  private fetchAllowedDashboardIds(authPayload: AuthPayload): Observable<string[]> {
    if (authPayload.forceFullscreen && (authPayload.authUser.authority === Authority.TENANT_ADMIN ||
      authPayload.authUser.authority === Authority.CUSTOMER_USER)) {
      const pageLink = new PageLink(100);
      let fetchDashboardsObservable: Observable<PageData<DashboardInfo>>;
      if (authPayload.authUser.authority === Authority.TENANT_ADMIN) {
        fetchDashboardsObservable = this.dashboardService.getTenantDashboards(pageLink);
      } else {
        fetchDashboardsObservable = this.dashboardService.getCustomerDashboards(authPayload.authUser.customerId, pageLink);
      }
      return fetchDashboardsObservable.pipe(
        map((result) => {
          const dashboards = result.data;
          return dashboards.map(dashboard => dashboard.id.id);
        })
      );
    } else {
      return of([]);
    }
  }
}
