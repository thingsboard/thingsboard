// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  CaptchaParams,
  SignUpSelfRegistrationParams,
  WebSelfRegistrationParams
} from '@shared/models/self-register.models';
import { Observable, of } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { isDefinedAndNotNull } from '@core/utils';

@Injectable({
  providedIn: 'root'
})
export class SelfRegistrationService {

  signUpParams: SignUpSelfRegistrationParams = null;

  constructor(
    private router: Router,
    private http: HttpClient,
  ) {
  }

  public getRegistrationLink(domainName: string): string {
    return `${domainName}/signup`;
  }

  public loadSelfRegistrationParams(): Observable<SignUpSelfRegistrationParams> {
    return this.http.get<SignUpSelfRegistrationParams>('/api/noauth/selfRegistration/signUpSelfRegistrationParams').pipe(
      tap((signUpParams) => {
        this.signUpParams = signUpParams || {} as SignUpSelfRegistrationParams;
        this.signUpParams.activate = isDefinedAndNotNull(signUpParams?.captcha?.siteKey);
        if (!isDefinedAndNotNull(signUpParams?.captcha)) {
          this.signUpParams.captcha = {} as CaptchaParams;
        }
        this.signUpParams.captcha.version = signUpParams?.captcha?.version || 'v3';
      })
    );
  }

  public loadPrivacyPolicy(): Observable<string> {
    return this.http.get<string>('/api/noauth/selfRegistration/privacyPolicy')
  }

  public loadTermsOfUse(): Observable<string> {
    return this.http.get<string>('/api/noauth/selfRegistration/termsOfUse')
  }

  public isAvailablePage(): Observable<any> {
    if (this.signUpParams) {
      if (!this.signUpParams.activate) {
        this.router.navigateByUrl('login');
      }
      return of(null);
    } else {
      return this.loadSelfRegistrationParams().pipe(
        tap(() => {
          if (!this.signUpParams.activate) {
            this.router.navigateByUrl('login');
          }
        })
      );
    }
  }

  public saveSelfRegistrationParams(selfRegistrationParams: WebSelfRegistrationParams,
                                    config?: RequestConfig): Observable<WebSelfRegistrationParams> {
    return this.http.post<WebSelfRegistrationParams>('/api/selfRegistration/selfRegistrationParams',
      selfRegistrationParams, defaultHttpOptionsFromConfig(config));
  }

  public getSelfRegistrationParams(config?: RequestConfig): Observable<WebSelfRegistrationParams> {
    return this.http.get<WebSelfRegistrationParams>(`/api/selfRegistration/selfRegistrationParams`, defaultHttpOptionsFromConfig(config));
  }

  public deleteSelfRegistrationParams(config?: RequestConfig) {
    return this.http.delete(`/api/selfRegistration/selfRegistrationParams`, defaultHttpOptionsFromConfig(config));
  }

}
