import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { TwoFactorAuthSettings } from '@shared/models/two-factor-auth.models';

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

}
