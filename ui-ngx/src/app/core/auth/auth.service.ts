///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import {Injectable, NgZone} from '@angular/core';
import {JwtHelperService} from '@auth0/angular-jwt';
import {HttpClient} from '@angular/common/http';

import {combineLatest, forkJoin, Observable, of} from 'rxjs';
import {distinctUntilChanged, filter, map, skip, tap} from 'rxjs/operators';

import {LoginRequest, LoginResponse} from '../../shared/models/login.models';
import {ActivatedRoute, Router, UrlTree} from '@angular/router';
import {defaultHttpOptions} from '../http/http-utils';
import {ReplaySubject} from 'rxjs/internal/ReplaySubject';
import {UserService} from '../http/user.service';
import {select, Store} from '@ngrx/store';
import {AppState} from '../core.state';
import {ActionAuthAuthenticated, ActionAuthLoadUser, ActionAuthUnauthenticated} from './auth.actions';
import {getCurrentAuthUser, selectIsAuthenticated, selectIsUserLoaded} from './auth.selectors';
import {Authority} from '../../shared/models/authority.enum';
import {ActionSettingsChangeLanguage} from '@app/core/settings/settings.actions';
import {AuthPayload} from '@core/auth/auth.models';
import {TranslateService} from '@ngx-translate/core';
import {AuthUser} from '@shared/models/user.model';
import {TimeService} from '@core/services/time.service';

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
    private translate: TranslateService
  ) {
    combineLatest(
      this.store.pipe(select(selectIsAuthenticated)),
      this.store.pipe(select(selectIsUserLoaded))
    ).pipe(
      map(results => ({isAuthenticated: results[0], isUserLoaded: results[1]})),
      distinctUntilChanged(),
      filter((data) => data.isUserLoaded ),
      skip(1),
    ).subscribe((data) => {
      this.gotoDefaultPlace(data.isAuthenticated);
    });
    this.reloadUser();
  }

  redirectUrl: string;

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

  public sendResetPasswordLink(email: string) {
    return this.http.post('/api/noauth/resetPasswordByEmail',
      {email}, defaultHttpOptions());
  }

  public activate(activateToken: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/noauth/activate', {activateToken, password}, defaultHttpOptions()).pipe(
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
    this.clearJwtToken();
  }

  private notifyUserLoaded(isUserLoaded: boolean) {
    this.store.dispatch(new ActionAuthLoadUser({isUserLoaded}));
  }

  public gotoDefaultPlace(isAuthenticated: boolean) {
    const url = this.defaultUrl(isAuthenticated);
    this.zone.run(() => {
      this.router.navigateByUrl(url);
    });
  }

  public defaultUrl(isAuthenticated: boolean): UrlTree {
    if (isAuthenticated) {
      if (this.redirectUrl) {
        const redirectUrl = this.redirectUrl;
        this.redirectUrl = null;
        return this.router.parseUrl(redirectUrl);
      } else {

        // TODO:

        return this.router.parseUrl('home');
      }
    } else {
      return this.router.parseUrl('login');
    }
  }

  private loadUser(doTokenRefresh): Observable<AuthPayload> {
    const authUser = getCurrentAuthUser(this.store);
    if (!authUser) {
      return this.procceedJwtTokenValidate(doTokenRefresh);
    } else {
      return of({} as AuthPayload);
    }
  }

  private procceedJwtTokenValidate(doTokenRefresh: boolean): Observable<AuthPayload> {
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
        const sysParamsObservable = this.loadSystemParams(authPayload.authUser);
        if (authPayload.authUser.isPublic) {

          // TODO:

        } else if (authPayload.authUser.userId) {
          this.userService.getUser(authPayload.authUser.userId).subscribe(
            (user) => {
              sysParamsObservable.subscribe(
                (sysParams) => {
                  authPayload = {...authPayload, ...sysParams};
                  authPayload.userDetails = user;
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

  private loadSystemParams(authUser: AuthUser): Observable<any> {
    const sources: Array<Observable<any>> = [this.loadIsUserTokenAccessEnabled(authUser),
                                             this.timeService.loadMaxDatapointsLimit()];
    return forkJoin(sources)
      .pipe(map((data) => {
        const userTokenAccessEnabled: boolean = data[0];
        return {userTokenAccessEnabled};
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
        this.loadUser(false);
      }
    }
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

}
