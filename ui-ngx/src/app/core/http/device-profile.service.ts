///
/// Copyright © 2016-2021 The Thingsboard Authors
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
import { Observable, of, throwError } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { DeviceProfile, DeviceProfileInfo, DeviceTransportType } from '@shared/models/device.models';
import { deepClone, isDefinedAndNotNull, isEmptyStr } from '@core/utils';
import {
  ObjectLwM2M,
  securityConfigMode,
  ServerSecurityConfig,
  ServerSecurityConfigInfo
} from '@home/components/profile/device/lwm2m/lwm2m-profile-config.models';
import { SortOrder } from '@shared/models/page/sort-order';
import { OtaPackageService } from '@core/http/ota-package.service';
import { map, mergeMap, tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class DeviceProfileService {

  private lwm2mBootstrapSecurityInfoInMemoryCache = new Map<boolean, ServerSecurityConfigInfo>();

  constructor(
    private http: HttpClient,
    private otaPackageService: OtaPackageService
  ) {
  }

  public getDeviceProfiles(pageLink: PageLink, config?: RequestConfig): Observable<PageData<DeviceProfile>> {
    return this.http.get<PageData<DeviceProfile>>(`/api/deviceProfiles${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getDeviceProfile(deviceProfileId: string, config?: RequestConfig): Observable<DeviceProfile> {
    return this.http.get<DeviceProfile>(`/api/deviceProfile/${deviceProfileId}`, defaultHttpOptionsFromConfig(config));
  }

  public getLwm2mObjects(sortOrder: SortOrder, objectIds?: string[], searchText?: string, config?: RequestConfig):
    Observable<Array<ObjectLwM2M>> {
    let url = `/api/resource/lwm2m/?sortProperty=${sortOrder.property}&sortOrder=${sortOrder.direction}`;
    if (isDefinedAndNotNull(objectIds) && objectIds.length > 0) {
      url += `&objectIds=${objectIds}`;
    }
    if (isDefinedAndNotNull(searchText) && !isEmptyStr(searchText)) {
      url += `&searchText=${searchText}`;
    }
    return this.http.get<Array<ObjectLwM2M>>(url, defaultHttpOptionsFromConfig(config));
  }

  public getLwm2mBootstrapSecurityInfo(isBootstrapServer: boolean, config?: RequestConfig): Observable<ServerSecurityConfigInfo> {
    const securityConfig = this.lwm2mBootstrapSecurityInfoInMemoryCache.get(isBootstrapServer);
    if (securityConfig) {
      return of(securityConfig);
    } else {
      return this.http.get<ServerSecurityConfigInfo>(
        `/api/lwm2m/deviceProfile/bootstrap/${isBootstrapServer}`,
        defaultHttpOptionsFromConfig(config)
      ).pipe(
        tap(serverConfig => this.lwm2mBootstrapSecurityInfoInMemoryCache.set(isBootstrapServer, serverConfig))
      );
    }
  }

  public getLwm2mBootstrapSecurityInfoBySecurityType(isBootstrapServer: boolean, securityMode = securityConfigMode.NO_SEC,
                                                     config?: RequestConfig): Observable<ServerSecurityConfig> {
    return this.getLwm2mBootstrapSecurityInfo(isBootstrapServer, config).pipe(
      map(securityConfig => {
        const serverSecurityConfigInfo = deepClone(securityConfig);
        switch (securityMode) {
          case securityConfigMode.PSK:
            serverSecurityConfigInfo.port = serverSecurityConfigInfo.securityPort;
            serverSecurityConfigInfo.host = serverSecurityConfigInfo.securityHost;
            serverSecurityConfigInfo.serverPublicKey = '';
            break;
          case securityConfigMode.RPK:
          case securityConfigMode.X509:
            serverSecurityConfigInfo.port = serverSecurityConfigInfo.securityPort;
            serverSecurityConfigInfo.host = serverSecurityConfigInfo.securityHost;
            break;
          case securityConfigMode.NO_SEC:
            serverSecurityConfigInfo.serverPublicKey = '';
            break;
        }
        return serverSecurityConfigInfo;
      })
    );
  }

  public getLwm2mObjectsPage(pageLink: PageLink, config?: RequestConfig): Observable<Array<ObjectLwM2M>> {
    return this.http.get<Array<ObjectLwM2M>>(
      `/api/resource/lwm2m/page${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  public saveDeviceProfileAndConfirmOtaChange(originDeviceProfile: DeviceProfile, deviceProfile: DeviceProfile,
                                              config?: RequestConfig): Observable<DeviceProfile> {
    return this.otaPackageService.confirmDialogUpdatePackage(deviceProfile, originDeviceProfile).pipe(
      mergeMap((update) => update ? this.saveDeviceProfile(deviceProfile, config) : throwError('Canceled saving device profiles'))
    );
  }

  public saveDeviceProfile(deviceProfile: DeviceProfile, config?: RequestConfig): Observable<DeviceProfile> {
    return this.http.post<DeviceProfile>('/api/deviceProfile', deviceProfile, defaultHttpOptionsFromConfig(config));
  }

  public deleteDeviceProfile(deviceProfileId: string, config?: RequestConfig) {
    return this.http.delete(`/api/deviceProfile/${deviceProfileId}`, defaultHttpOptionsFromConfig(config));
  }

  public setDefaultDeviceProfile(deviceProfileId: string, config?: RequestConfig): Observable<DeviceProfile> {
    return this.http.post<DeviceProfile>(`/api/deviceProfile/${deviceProfileId}/default`, defaultHttpOptionsFromConfig(config));
  }

  public getDefaultDeviceProfileInfo(config?: RequestConfig): Observable<DeviceProfileInfo> {
    return this.http.get<DeviceProfileInfo>('/api/deviceProfileInfo/default', defaultHttpOptionsFromConfig(config));
  }

  public getDeviceProfileInfo(deviceProfileId: string, config?: RequestConfig): Observable<DeviceProfileInfo> {
    return this.http.get<DeviceProfileInfo>(`/api/deviceProfileInfo/${deviceProfileId}`, defaultHttpOptionsFromConfig(config));
  }

  public getDeviceProfileInfos(pageLink: PageLink, transportType?: DeviceTransportType,
                               config?: RequestConfig): Observable<PageData<DeviceProfileInfo>> {
    let url = `/api/deviceProfileInfos${pageLink.toQuery()}`;
    if (isDefinedAndNotNull(transportType)) {
      url += `&transportType=${transportType}`;
    }
    return this.http.get<PageData<DeviceProfileInfo>>(url, defaultHttpOptionsFromConfig(config));
  }

  public getDeviceProfileDevicesAttributesKeys(deviceProfileId?: string, config?: RequestConfig): Observable<Array<string>> {
    let url = `/api/deviceProfile/devices/keys/attributes`;
    if (isDefinedAndNotNull(deviceProfileId)) {
      url += `?deviceProfileId=${deviceProfileId}`;
    }
    return this.http.get<Array<string>>(url, defaultHttpOptionsFromConfig(config));
  }

  public getDeviceProfileDevicesTimeseriesKeys(deviceProfileId?: string, config?: RequestConfig): Observable<Array<string>> {
    let url = `/api/deviceProfile/devices/keys/timeseries`;
    if (isDefinedAndNotNull(deviceProfileId)) {
      url += `?deviceProfileId=${deviceProfileId}`;
    }
    return this.http.get<Array<string>>(url, defaultHttpOptionsFromConfig(config));
  }

}
