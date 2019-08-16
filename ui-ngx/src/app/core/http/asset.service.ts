///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import {Injectable} from '@angular/core';
import {defaultHttpOptions} from './http-utils';
import {Observable} from 'rxjs/index';
import {HttpClient} from '@angular/common/http';
import {PageLink} from '@shared/models/page/page-link';
import {PageData} from '@shared/models/page/page-data';
import {EntitySubtype} from '@app/shared/models/entity-type.models';
import {Asset, AssetInfo} from '@app/shared/models/asset.models';

@Injectable({
  providedIn: 'root'
})
export class AssetService {

  constructor(
    private http: HttpClient
  ) { }

  public getTenantAssetInfos(pageLink: PageLink, type: string = '', ignoreErrors: boolean = false,
                             ignoreLoading: boolean = false): Observable<PageData<AssetInfo>> {
    return this.http.get<PageData<AssetInfo>>(`/api/tenant/assetInfos${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getCustomerAssetInfos(customerId: string, pageLink: PageLink, type: string = '', ignoreErrors: boolean = false,
                               ignoreLoading: boolean = false): Observable<PageData<AssetInfo>> {
    return this.http.get<PageData<AssetInfo>>(`/api/customer/${customerId}/assetInfos${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getAsset(assetId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Asset> {
    return this.http.get<Asset>(`/api/asset/${assetId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getAssets(assetIds: Array<string>, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Array<Asset>> {
    return this.http.get<Array<Asset>>(`/api/assets?assetIds=${assetIds.join(',')}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getAssetInfo(assetId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<AssetInfo> {
    return this.http.get<AssetInfo>(`/api/asset/info/${assetId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public saveAsset(asset: Asset, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Asset> {
    return this.http.post<Asset>('/api/asset', asset, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public deleteAsset(assetId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false) {
    return this.http.delete(`/api/asset/${assetId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getAssetTypes(ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Array<EntitySubtype>> {
    return this.http.get<Array<EntitySubtype>>('/api/asset/types', defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public makeAssetPublic(assetId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Asset> {
    return this.http.post<Asset>(`/api/customer/public/asset/${assetId}`, null, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public assignAssetToCustomer(customerId: string, assetId: string,
                               ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Asset> {
    return this.http.post<Asset>(`/api/customer/${customerId}/asset/${assetId}`, null, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public unassignAssetFromCustomer(assetId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false) {
    return this.http.delete(`/api/customer/asset/${assetId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

}
