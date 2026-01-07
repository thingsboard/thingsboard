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

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { OAuth2Client, OAuth2ClientInfo, OAuth2ClientRegistrationTemplate } from '@shared/models/oauth2.models';
import { PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';

@Injectable({
  providedIn: 'root'
})
export class OAuth2Service {

  constructor(
    private http: HttpClient
  ) {
  }

  public getOAuth2Template(config?: RequestConfig): Observable<Array<OAuth2ClientRegistrationTemplate>> {
    return this.http.get<Array<OAuth2ClientRegistrationTemplate>>(`/api/oauth2/config/template`, defaultHttpOptionsFromConfig(config));
  }

  public saveOAuth2Client(oAuth2Client: OAuth2Client, config?: RequestConfig): Observable<OAuth2Client> {
    return this.http.post<OAuth2Client>('/api/oauth2/client', oAuth2Client, defaultHttpOptionsFromConfig(config));
  }

  public findTenantOAuth2ClientInfos(pageLink: PageLink, config?: RequestConfig): Observable<PageData<OAuth2ClientInfo>> {
    return this.http.get<PageData<OAuth2ClientInfo>>(`/api/oauth2/client/infos${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public findTenantOAuth2ClientInfosByIds(clientIds: Array<string>, config?: RequestConfig): Observable<Array<OAuth2ClientInfo>> {
    return this.http.get<Array<OAuth2ClientInfo>>(`/api/oauth2/client/infos?clientIds=${clientIds.join(',')}`, defaultHttpOptionsFromConfig(config))
  }

  public getOAuth2ClientById(id: string, config?: RequestConfig): Observable<OAuth2Client> {
    return this.http.get<OAuth2Client>(`/api/oauth2/client/${id}`, defaultHttpOptionsFromConfig(config));
  }

  public deleteOauth2Client(id: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/oauth2/client/${id}`, defaultHttpOptionsFromConfig(config));
  }

  public getLoginProcessingUrl(config?: RequestConfig): Observable<string> {
    return this.http.get<string>('/api/oauth2/loginProcessingUrl', defaultHttpOptionsFromConfig(config));
  }

}
