///
/// Copyright © 2016-2024 The Thingsboard Authors
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
import { MobileApp, MobileAppInfo } from '@shared/models/oauth2.models';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';

@Injectable({
  providedIn: 'root'
})
export class MobileAppService {

  constructor(
    private http: HttpClient
  ) {
  }

  public saveMobileApp(mobileApp: MobileApp, oauth2ClientIds: Array<string>, config?: RequestConfig): Observable<MobileApp> {
    return this.http.post<MobileApp>(`/api/mobileApp?oauth2ClientIds=${oauth2ClientIds.join(',')}`,
      mobileApp, defaultHttpOptionsFromConfig(config));
  }

  public updateOauth2Clients(id: string, oauth2ClientRegistrationIds: Array<string>, config?: RequestConfig): Observable<void> {
    return this.http.put<void>(`/api/mobileApp/${id}/oauth2Clients`, oauth2ClientRegistrationIds, defaultHttpOptionsFromConfig(config));
  }

  public getTenantMobileAppInfos(pageLink: PageLink, config?: RequestConfig): Observable<PageData<MobileAppInfo>> {
    return this.http.get<PageData<MobileAppInfo>>(`/api/mobileApp/infos${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getMobileAppInfoById(id: string, config?: RequestConfig): Observable<MobileAppInfo> {
    return this.http.get<MobileAppInfo>(`/api/mobileApp/info/${id}`, defaultHttpOptionsFromConfig(config));
  }

  public deleteMobileApp(id: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/mobileApp/${id}`, defaultHttpOptionsFromConfig(config));
  }

}
