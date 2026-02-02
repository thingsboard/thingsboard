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
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { MobileApp, MobileAppBundle, MobileAppBundleInfo } from '@shared/models/mobile-app.models';
import { PlatformType } from '@shared/models/oauth2.models';

@Injectable({
  providedIn: 'root'
})
export class MobileAppService {

  constructor(
    private http: HttpClient
  ) {
  }

  public saveMobileApp(mobileApp: MobileApp, config?: RequestConfig): Observable<MobileApp> {
    return this.http.post<MobileApp>(`/api/mobile/app`, mobileApp, defaultHttpOptionsFromConfig(config));
  }

  public getTenantMobileAppInfos(pageLink: PageLink, platformType?: PlatformType, config?: RequestConfig): Observable<PageData<MobileApp>> {
    let url = `/api/mobile/app${pageLink.toQuery()}`;
    if (platformType) {
      url += `&platformType=${platformType}`
    }
    return this.http.get<PageData<MobileApp>>(url, defaultHttpOptionsFromConfig(config));
  }

  public getMobileAppInfoById(id: string, config?: RequestConfig): Observable<MobileApp> {
    return this.http.get<MobileApp>(`/api/mobile/app/${id}`, defaultHttpOptionsFromConfig(config));
  }

  public deleteMobileApp(id: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/mobile/app/${id}`, defaultHttpOptionsFromConfig(config));
  }

  public saveMobileAppBundle(mobileAppBundle: MobileAppBundle, oauth2ClientIds?: Array<string>, config?: RequestConfig) {
    let url = '/api/mobile/bundle';
    if (oauth2ClientIds?.length) {
      url += `?oauth2ClientIds=${oauth2ClientIds.join(',')}`;
    }
    return this.http.post<MobileAppBundle>(url, mobileAppBundle, defaultHttpOptionsFromConfig(config));
  }

  public updateOauth2Clients(id: string, oauth2ClientIds: Array<string>, config?: RequestConfig) {
    return this.http.put(`/api/mobile/bundle/${id}/oauth2Clients`, oauth2ClientIds ?? [], defaultHttpOptionsFromConfig(config));
  }

  public getTenantMobileAppBundleInfos(pageLink: PageLink, config?: RequestConfig): Observable<PageData<MobileAppBundleInfo>> {
    return this.http.get<PageData<MobileAppBundleInfo>>(`/api/mobile/bundle/infos${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getMobileAppBundleInfoById(id: string, config?: RequestConfig): Observable<MobileAppBundleInfo> {
    return this.http.get<MobileAppBundleInfo>(`/api/mobile/bundle/info/${id}`, defaultHttpOptionsFromConfig(config));
  }

  public deleteMobileAppBundle(id: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/mobile/bundle/${id}`, defaultHttpOptionsFromConfig(config));
  }

}
