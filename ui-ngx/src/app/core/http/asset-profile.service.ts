// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
