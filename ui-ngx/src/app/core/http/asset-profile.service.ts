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
import { PageLink } from '@shared/models/page/page-link';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { AssetProfile, AssetProfileInfo } from '@shared/models/asset.models';
import { EntityInfoData } from '@shared/models/entity.models';
import { isDefinedAndNotNull } from '@core/utils';

@Injectable({
  providedIn: 'root'
})
export class AssetProfileService {

  constructor(
    private http: HttpClient
  ) {
  }

  public getAssetProfiles(pageLink: PageLink, config?: RequestConfig): Observable<PageData<AssetProfile>> {
    return this.http.get<PageData<AssetProfile>>(`/api/assetProfiles${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getAssetProfile(assetProfileId: string, config?: RequestConfig): Observable<AssetProfile> {
    return this.http.get<AssetProfile>(`/api/assetProfile/${assetProfileId}`, defaultHttpOptionsFromConfig(config));
  }

  public exportAssetProfile(assetProfileId: string, config?: RequestConfig): Observable<AssetProfile> {
    return this.http.get<AssetProfile>(`/api/assetProfile/${assetProfileId}?inlineImages=true`, defaultHttpOptionsFromConfig(config));
  }

  public saveAssetProfile(assetProfile: AssetProfile, config?: RequestConfig): Observable<AssetProfile> {
    return this.http.post<AssetProfile>('/api/assetProfile', assetProfile, defaultHttpOptionsFromConfig(config));
  }

  public deleteAssetProfile(assetProfileId: string, config?: RequestConfig) {
    return this.http.delete(`/api/assetProfile/${assetProfileId}`, defaultHttpOptionsFromConfig(config));
  }

  public setDefaultAssetProfile(assetProfileId: string, config?: RequestConfig): Observable<AssetProfile> {
    return this.http.post<AssetProfile>(`/api/assetProfile/${assetProfileId}/default`, defaultHttpOptionsFromConfig(config));
  }

  public getDefaultAssetProfileInfo(config?: RequestConfig): Observable<AssetProfileInfo> {
    return this.http.get<AssetProfileInfo>('/api/assetProfileInfo/default', defaultHttpOptionsFromConfig(config));
  }

  public getAssetProfileInfo(assetProfileId: string, config?: RequestConfig): Observable<AssetProfileInfo> {
    return this.http.get<AssetProfileInfo>(`/api/assetProfileInfo/${assetProfileId}`, defaultHttpOptionsFromConfig(config));
  }

  public getAssetProfileInfos(pageLink: PageLink, config?: RequestConfig): Observable<PageData<AssetProfileInfo>> {
    return this.http.get<PageData<AssetProfileInfo>>(`/api/assetProfileInfos${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getAssetProfileNames(activeOnly: boolean = false, config?: RequestConfig): Observable<Array<EntityInfoData>> {
    let url = '/api/assetProfile/names';
    if (isDefinedAndNotNull(activeOnly)) {
      url += `?activeOnly=${activeOnly}`;
    }
    return this.http.get<Array<EntityInfoData>>(url, defaultHttpOptionsFromConfig(config));
  }

}
