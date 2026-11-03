// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import {
  AccountTwoFaSettings,
  TwoFactorAuthAccountConfig,
  TwoFactorAuthProviderType,
  TwoFactorAuthSettings
} from '@shared/models/two-factor-auth.models';
import { isDefinedAndNotNull } from '@core/utils';

@Injectable({
  providedIn: 'root'
})
export class TwoFactorAuthenticationService {

  constructor(
    private http: HttpClient
  ) {
  }

  getTwoFaSettings(config?: RequestConfig): Observable<TwoFactorAuthSettings> {
    return this.http.get<TwoFactorAuthSettings>(`/api/2fa/settings`, defaultHttpOptionsFromConfig(config));
  }

  saveTwoFaSettings(settings: TwoFactorAuthSettings, config?: RequestConfig): Observable<TwoFactorAuthSettings> {
    return this.http.post<TwoFactorAuthSettings>(`/api/2fa/settings`, settings, defaultHttpOptionsFromConfig(config));
  }

  getAvailableTwoFaProviders(config?: RequestConfig): Observable<Array<TwoFactorAuthProviderType>> {
    return this.http.get<Array<TwoFactorAuthProviderType>>(`/api/2fa/providers`, defaultHttpOptionsFromConfig(config));
  }

  generateTwoFaAccountConfig(providerType: TwoFactorAuthProviderType, config?: RequestConfig): Observable<TwoFactorAuthAccountConfig> {
    return this.http.post<TwoFactorAuthAccountConfig>(`/api/2fa/account/config/generate?providerType=${providerType}`,
      defaultHttpOptionsFromConfig(config));
  }

  getAccountTwoFaSettings(config?: RequestConfig): Observable<AccountTwoFaSettings> {
    return this.http.get<AccountTwoFaSettings>(`/api/2fa/account/settings`, defaultHttpOptionsFromConfig(config));
  }

  updateTwoFaAccountConfig(providerType: TwoFactorAuthProviderType, useByDefault: boolean,
                           config?: RequestConfig): Observable<AccountTwoFaSettings> {
    return this.http.put<AccountTwoFaSettings>(`/api/2fa/account/config?providerType=${providerType}`, {useByDefault},
      defaultHttpOptionsFromConfig(config));
  }

  submitTwoFaAccountConfig(authConfig: TwoFactorAuthAccountConfig, config?: RequestConfig): Observable<any> {
    return this.http.post(`/api/2fa/account/config/submit`, authConfig, defaultHttpOptionsFromConfig(config));
  }

  verifyAndSaveTwoFaAccountConfig(authConfig: TwoFactorAuthAccountConfig, verificationCode?: number,
                                  config?: RequestConfig): Observable<AccountTwoFaSettings> {
    let url = '/api/2fa/account/config';
    if (isDefinedAndNotNull(verificationCode)) {
      url += `?verificationCode=${verificationCode}`;
    }
    return this.http.post<AccountTwoFaSettings>(url, authConfig, defaultHttpOptionsFromConfig(config));
  }

  deleteTwoFaAccountConfig(providerType: TwoFactorAuthProviderType, config?: RequestConfig): Observable<AccountTwoFaSettings> {
    return this.http.delete<AccountTwoFaSettings>(`/api/2fa/account/config?providerType=${providerType}`,
      defaultHttpOptionsFromConfig(config));
  }

  requestTwoFaVerificationCodeSend(providerType: TwoFactorAuthProviderType, config?: RequestConfig) {
    return this.http.post(`/api/auth/2fa/verification/send?providerType=${providerType}`, defaultHttpOptionsFromConfig(config));
  }

}
