// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Injectable } from '@angular/core';
import { createDefaultHttpOptions, defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { EntitySubtype } from '@app/shared/models/entity-type.models';
import { Asset, AssetInfo, AssetSearchQuery } from '@app/shared/models/asset.models';
import { map } from 'rxjs/operators';
import { sortEntitiesByIds } from '@shared/models/base-data';
import { BulkImportRequest, BulkImportResult } from '@shared/import-export/import-export.models';
import { SaveEntityWithGroupParams, toSaveParams } from '@shared/models/entity.models';

@Injectable({
  providedIn: 'root'
})
export class AssetService {

  constructor(
    private http: HttpClient
  ) { }

/*  public getTenantAssetInfos(pageLink: PageLink, type: string = '', config?: RequestConfig): Observable<PageData<AssetInfo>> {
    return this.http.get<PageData<AssetInfo>>(`/api/tenant/assetInfos${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getTenantAssetInfosByAssetProfileId(pageLink: PageLink, assetProfileId: string = '',
                                             config?: RequestConfig): Observable<PageData<AssetInfo>> {
    return this.http.get<PageData<AssetInfo>>(`/api/tenant/assetInfos${pageLink.toQuery()}&assetProfileId=${assetProfileId}`,
      defaultHttpOptionsFromConfig(config));
  } */

  public getTenantAssets(pageLink: PageLink, type: string = '', config?: RequestConfig): Observable<PageData<Asset>> {
    return this.http.get<PageData<Asset>>(`/api/tenant/assets${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomerAssets(customerId: string, pageLink: PageLink, type: string = '',
                           config?: RequestConfig): Observable<PageData<Asset>> {
    return this.http.get<PageData<Asset>>(`/api/customer/${customerId}/assets${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

/*  public getCustomerAssetInfosByAssetProfileId(customerId: string, pageLink: PageLink, assetProfileId: string = '',
                                               config?: RequestConfig): Observable<PageData<AssetInfo>> {
    return this.http.get<PageData<AssetInfo>>
    (`/api/customer/${customerId}/assetInfos${pageLink.toQuery()}&assetProfileId=${assetProfileId}`,
      defaultHttpOptionsFromConfig(config));
  } */

  public getAsset(assetId: string, config?: RequestConfig): Observable<Asset> {
    return this.http.get<Asset>(`/api/asset/${assetId}`, defaultHttpOptionsFromConfig(config));
  }

  public getAssets(assetIds: Array<string>, config?: RequestConfig): Observable<Array<Asset>> {
    return this.http.get<Array<Asset>>(`/api/assets?assetIds=${assetIds.join(',')}`, defaultHttpOptionsFromConfig(config)).pipe(
      map((assets) => sortEntitiesByIds(assets, assetIds))
    );
  }

  public getUserAssets(pageLink: PageLink, type: string = '', config?: RequestConfig): Observable<PageData<Asset>> {
    return this.http.get<PageData<Asset>>(`/api/user/assets${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAllAssetInfos(includeCustomers: boolean,
                          pageLink: PageLink, assetProfileId: string = '', config?: RequestConfig): Observable<PageData<AssetInfo>> {
    let url = `/api/assetInfos/all${pageLink.toQuery()}&assetProfileId=${assetProfileId}`;
    if (includeCustomers) {
      url += `&includeCustomers=true`;
    }
    return this.http.get<PageData<AssetInfo>>(url,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomerAssetInfos(includeCustomers: boolean, customerId: string,
                               pageLink: PageLink, assetProfileId: string = '',
                               config?: RequestConfig): Observable<PageData<AssetInfo>> {
    let url = `/api/customer/${customerId}/assetInfos${pageLink.toQuery()}&assetProfileId=${assetProfileId}`;
    if (includeCustomers) {
      url += `&includeCustomers=true`;
    }
    return this.http.get<PageData<AssetInfo>>(url,
      defaultHttpOptionsFromConfig(config));
  }

  public getAssetInfo(assetId: string, config?: RequestConfig): Observable<AssetInfo> {
    return this.http.get<AssetInfo>(`/api/asset/info/${assetId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveAsset(asset: Asset, entityGroupIds?: string | string[], config?: RequestConfig): Observable<Asset>;
  public saveAsset(asset: Asset, saveParams?: SaveEntityWithGroupParams, config?: RequestConfig): Observable<Asset>;
  public saveAsset(asset: Asset, saveParams?: string | string[] | SaveEntityWithGroupParams, config?: RequestConfig): Observable<Asset> {
    const params = toSaveParams(saveParams);
    return this.http.post<Asset>('/api/asset', asset, createDefaultHttpOptions(params, config));
  }

  public deleteAsset(assetId: string, config?: RequestConfig) {
    return this.http.delete(`/api/asset/${assetId}`, defaultHttpOptionsFromConfig(config));
  }

  public getAssetTypes(config?: RequestConfig): Observable<Array<EntitySubtype>> {
    return this.http.get<Array<EntitySubtype>>('/api/asset/types', defaultHttpOptionsFromConfig(config));
  }

/*  public makeAssetPublic(assetId: string, config?: RequestConfig): Observable<Asset> {
    return this.http.post<Asset>(`/api/customer/public/asset/${assetId}`, null, defaultHttpOptionsFromConfig(config));
  }

  public assignAssetToCustomer(customerId: string, assetId: string,
                               config?: RequestConfig): Observable<Asset> {
    return this.http.post<Asset>(`/api/customer/${customerId}/asset/${assetId}`, null, defaultHttpOptionsFromConfig(config));
  }

  public unassignAssetFromCustomer(assetId: string, config?: RequestConfig) {
    return this.http.delete(`/api/customer/asset/${assetId}`, defaultHttpOptionsFromConfig(config));
  }*/

  public findByQuery(query: AssetSearchQuery,
                     config?: RequestConfig): Observable<Array<Asset>> {
    return this.http.post<Array<Asset>>('/api/assets', query, defaultHttpOptionsFromConfig(config));
  }

  public findByName(assetName: string, config?: RequestConfig): Observable<Asset> {
    return this.http.get<Asset>(`/api/tenant/assets?assetName=${assetName}`, defaultHttpOptionsFromConfig(config));
  }

  /*  public assignAssetToEdge(edgeId: string, assetId: string, config?: RequestConfig): Observable<Asset> {
      return this.http.post<Asset>(`/api/edge/${edgeId}/asset/${assetId}`, null,
        defaultHttpOptionsFromConfig(config));
    }

    public unassignAssetFromEdge(edgeId: string, assetId: string,
                                 config?: RequestConfig) {
      return this.http.delete(`/api/edge/${edgeId}/asset/${assetId}`, defaultHttpOptionsFromConfig(config));
    }

    public getEdgeAssets(edgeId: string, pageLink: PageLink, type: string = '',
                         config?: RequestConfig): Observable<PageData<AssetInfo>> {
      return this.http.get<PageData<AssetInfo>>(`/api/edge/${edgeId}/assets${pageLink.toQuery()}&type=${type}`,
        defaultHttpOptionsFromConfig(config));
    }*/

  public bulkImportAssets(entitiesData: BulkImportRequest, config?: RequestConfig): Observable<BulkImportResult> {
    return this.http.post<BulkImportResult>('/api/asset/bulk_import', entitiesData, defaultHttpOptionsFromConfig(config));
  }
}
