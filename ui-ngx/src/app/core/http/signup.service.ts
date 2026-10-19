// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { SignupRequestValues, SignUpResult } from '@shared/models/signup.models';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable, of } from 'rxjs';
import { LoginResponse } from '@shared/models/login.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';

@Injectable({
  providedIn: 'root'
})
export class SignupService {

  constructor(
    private store: Store<AppState>,
    private http: HttpClient
  ) {
  }

  public signup(signupRequest: SignupRequestValues, config?: RequestConfig): Observable<SignUpResult> {
    return this.http.post<SignUpResult>('/api/noauth/signup', signupRequest, defaultHttpOptionsFromConfig(config));
  }

  public acceptPrivacyPolicy(config?: RequestConfig): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/signup/acceptPrivacyPolicy', null, defaultHttpOptionsFromConfig(config));
  }

  public deleteTenantAccount(config?: RequestConfig): Observable<any> {
    return this.http.delete('/api/signup/tenantAccount', defaultHttpOptionsFromConfig(config));
  }

  public isDisplayWelcome(): Observable<boolean> {
    const authUser = getCurrentAuthUser(this.store);
    if (authUser.authority === Authority.TENANT_ADMIN) {
      return this.http.get<boolean>('/api/signup/displayWelcome')
    } else {
      return of(false);
    }
  }

  public setNotDisplayWelcome(): Observable<any> {
    return this.http.post('/api/signup/notDisplayWelcome', null);
  }

}
