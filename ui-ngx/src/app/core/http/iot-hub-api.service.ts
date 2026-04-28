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
import { HttpClient, HttpParams } from '@angular/common/http';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';
import { MpItemVersionQuery, MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { CreatorView } from '@shared/models/iot-hub/iot-hub-creator.models';
import { IotHubInstalledItem, InstallItemVersionResult, UpdateItemVersionResult, ItemPublishedVersionInfo } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { ItemType, ItemTypeFilterInfo, WidgetCategory } from '@shared/models/iot-hub/iot-hub-item.models';
import { InterceptorHttpParams } from '@core/interceptors/interceptor-http-params';
import { InterceptorConfig } from '@core/interceptors/interceptor-config';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { DeviceConnectivitySettings } from '@shared/models/settings.models';
import { environment as env } from '@env/environment';

export function tbVersionToInt(version: string): number {
  const parts = version.replace(/[^0-9.]/g, '').split('.');
  const major = parseInt(parts[0], 10) || 0;
  const minor = parseInt(parts[1], 10) || 0;
  const patch = parseInt(parts[2], 10) || 0;
  return major * 100 + minor * 10 + patch;
}

export function iotHubResourceUrl(baseUrl: string, path: string): string {
  if (!path) {
    return path;
  }
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path;
  }
  return `${baseUrl}${path.startsWith('/') ? '' : '/'}${path}`;
}

export interface IotHubRequestConfig {
  ignoreLoading?: boolean;
  ignoreErrors?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class IotHubApiService {

  constructor(
    private http: HttpClient,
    private store: Store<AppState>
  ) {}

  get baseUrl(): string {
    return getCurrentAuthState(this.store)?.iotHubBaseUrl || '';
  }

  public resolveResourceUrl(path: string): string {
    return iotHubResourceUrl(this.baseUrl, path);
  }

  public getPublishedVersions(query: MpItemVersionQuery, config?: IotHubRequestConfig): Observable<PageData<MpItemVersionView>> {
    if (query.options.tbVersion == null) {
      query.options.tbVersion = tbVersionToInt(env.tbVersion);
    }
    if (query.options.peOnly == null) {
      query.options.peOnly = false;
    }
    return this.http.get<PageData<MpItemVersionView>>(
      `${this.baseUrl}/api/versions/published${query.toQuery()}`,
      { params: this.buildParams(config) }
    );
  }

  public getFilterInfo(itemType: ItemType, config?: IotHubRequestConfig): Observable<ItemTypeFilterInfo> {
    const url = `${this.baseUrl}/api/item-listing/filterInfo/${itemType}`
      + `?peOnly=false&tbVersion=${tbVersionToInt(env.tbVersion)}`;
    return this.http.get<ItemTypeFilterInfo>(url, { params: this.buildParams(config) });
  }

  public getWidgetCategories(textSearch?: string, scadaFirst?: boolean,
                             config?: IotHubRequestConfig): Observable<WidgetCategory[]> {
    const queryParams: string[] = [
      `peOnly=false`,
      `tbVersion=${tbVersionToInt(env.tbVersion)}`
    ];
    if (textSearch?.trim()) {
      queryParams.push(`textSearch=${encodeURIComponent(textSearch.trim())}`);
    }
    if (scadaFirst != null) {
      queryParams.push(`scadaFirst=${scadaFirst}`);
    }
    const url = `${this.baseUrl}/api/item-listing/widgetCategories?${queryParams.join('&')}`;
    return this.http.get<WidgetCategory[]>(url, { params: this.buildParams(config) });
  }

  public getVersionInfo(versionId: string, config?: IotHubRequestConfig): Observable<MpItemVersionView> {
    return this.http.get<MpItemVersionView>(
      `${this.baseUrl}/api/versions/${versionId}`,
      { params: this.buildParams(config) }
    );
  }

  public getPublishedVersion(itemId: string, config?: IotHubRequestConfig): Observable<MpItemVersionView> {
    return this.http.get<MpItemVersionView>(
      `${this.baseUrl}/api/items/${itemId}/published`,
      { params: this.buildParams(config) }
    );
  }

  public getVersionReadme(versionId: string, config?: IotHubRequestConfig): Observable<string> {
    return this.http.get(`${this.baseUrl}/api/versions/${versionId}/readme`, {
      params: this.buildParams(config),
      responseType: 'text'
    });
  }

  public getVersionFileData(versionId: string, config?: IotHubRequestConfig): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/api/versions/${versionId}/fileData`, {
      params: this.buildParams(config),
      responseType: 'blob'
    });
  }

  public getConnectivitySettings(config?: IotHubRequestConfig): Observable<DeviceConnectivitySettings> {
    return this.http.get<DeviceConnectivitySettings>(
      `/api/iot-hub/connectivity`,
      { params: this.buildParams(config) }
    );
  }

  public registerDeviceInstall(
    versionId: string,
    descriptor: { type?: string; createdEntityIds: { entityType: string; id: string }[]; dashboardId?: { entityType: string; id: string }; selectedInstallMethod?: string; installState?: Record<string, any> },
    config?: IotHubRequestConfig
  ): Observable<InstallItemVersionResult> {
    return this.http.post<InstallItemVersionResult>(
      `/api/iot-hub/device/register?versionId=${versionId}`,
      { type: 'DEVICE', ...descriptor },
      { params: this.buildParams(config) }
    );
  }

  public installItemVersion(versionId: string, config?: IotHubRequestConfig, data?: any): Observable<InstallItemVersionResult> {
    return this.http.post<InstallItemVersionResult>(
      `/api/iot-hub/versions/${versionId}/install`,
      data || null,
      { params: this.buildParams(config) }
    );
  }

  public updateItemVersion(installedItemId: string, versionId: string, config?: IotHubRequestConfig, force?: boolean): Observable<UpdateItemVersionResult> {
    let params = this.buildParams(config);
    if (force) {
      params = params.set('force', 'true');
    }
    return this.http.post<UpdateItemVersionResult>(
      `/api/iot-hub/installedItems/${installedItemId}/update/${versionId}`,
      null,
      { params }
    );
  }

  public getInstalledItemIds(config?: IotHubRequestConfig): Observable<string[]> {
    return this.http.get<string[]>(
      `/api/iot-hub/installedItems/itemIds`,
      { params: this.buildParams(config) }
    );
  }

  public getInstalledItemCounts(itemType: string, config?: IotHubRequestConfig): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(
      `/api/iot-hub/installedItems/counts?itemType=${itemType}`,
      { params: this.buildParams(config) }
    );
  }

  public getInstalledItems(pageLink: PageLink, itemTypes?: string | string[],
                           itemId?: string, config?: IotHubRequestConfig): Observable<PageData<IotHubInstalledItem>> {
    let query = pageLink.toQuery();
    if (itemTypes) {
      const types = Array.isArray(itemTypes) ? itemTypes : [itemTypes];
      types.forEach(t => query += `&itemTypes=${t}`);
    }
    if (itemId) {
      query += `&itemId=${itemId}`;
    }
    return this.http.get<PageData<IotHubInstalledItem>>(
      `/api/iot-hub/installedItems${query}`,
      { params: this.buildParams(config) }
    );
  }

  public getInstalledItemsCount(itemType?: string, config?: IotHubRequestConfig): Observable<number> {
    let url = '/api/iot-hub/installedItems/count';
    if (itemType) {
      url += `?itemType=${itemType}`;
    }
    return this.http.get<number>(url, { params: this.buildParams(config) });
  }

  public deleteInstalledItem(installedItemId: string, config?: IotHubRequestConfig): Observable<void> {
    return this.http.delete<void>(
      `/api/iot-hub/installedItems/${installedItemId}`,
      { params: this.buildParams(config) }
    );
  }

  public getItemsPublishedVersions(itemIds: string[], config?: IotHubRequestConfig): Observable<ItemPublishedVersionInfo[]> {
    return this.http.post<ItemPublishedVersionInfo[]>(
      `${this.baseUrl}/api/versions/publishedVersions`,
      itemIds,
      { params: this.buildParams(config) }
    );
  }

  public getCreatorProfile(creatorId: string, config?: IotHubRequestConfig): Observable<CreatorView> {
    return this.http.get<CreatorView>(
      `${this.baseUrl}/api/creators/${creatorId}/profile`,
      { params: this.buildParams(config) }
    );
  }

  private buildParams(config?: IotHubRequestConfig): HttpParams {
    return new InterceptorHttpParams(
      new InterceptorConfig(config?.ignoreLoading ?? false, config?.ignoreErrors ?? false)
    );
  }
}
