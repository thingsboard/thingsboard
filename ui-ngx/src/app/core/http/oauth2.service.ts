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

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { OAuth2ClientRegistrationTemplate, OAuth2ClientsParams } from '@shared/models/oauth2.models';

@Injectable({
  providedIn: 'root'
})
export class OAuth2Service {

  constructor(
    private http: HttpClient
  ) { }

  public getOAuth2Settings(config?: RequestConfig): Observable<OAuth2ClientsParams> {
    return this.http.get<OAuth2ClientsParams>(`/api/oauth2/config`, defaultHttpOptionsFromConfig(config));
  }

  public getOAuth2Template(config?: RequestConfig): Observable<Array<OAuth2ClientRegistrationTemplate>> {
    return this.http.get<Array<OAuth2ClientRegistrationTemplate>>(`/api/oauth2/config/template`, defaultHttpOptionsFromConfig(config));
  }

  public saveOAuth2Settings(OAuth2Setting: OAuth2ClientsParams, config?: RequestConfig): Observable<OAuth2ClientsParams> {
    return this.http.post<OAuth2ClientsParams>('/api/oauth2/config', OAuth2Setting,
      defaultHttpOptionsFromConfig(config));
  }
}
