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

import { Injectable } from '@angular/core';
import { defaultHttpOptions } from './http-utils';
import { Observable, Subject, ReplaySubject } from 'rxjs/index';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { Tenant } from '@shared/models/tenant.model';
import {DashboardInfo, Dashboard} from '@shared/models/dashboard.models';
import {map} from 'rxjs/operators';
import {DeviceInfo, Device, DeviceCredentials} from '@app/shared/models/device.models';
import {EntitySubtype} from '@app/shared/models/entity-type.models';
import {AuthService} from '../auth/auth.service';

@Injectable({
  providedIn: 'root'
})
export class DeviceService {

  constructor(
    private http: HttpClient
  ) { }

  public getTenantDeviceInfos(pageLink: PageLink, type: string = '', ignoreErrors: boolean = false,
                              ignoreLoading: boolean = false): Observable<PageData<DeviceInfo>> {
    return this.http.get<PageData<DeviceInfo>>(`/api/tenant/deviceInfos${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getCustomerDeviceInfos(customerId: string, pageLink: PageLink, type: string = '', ignoreErrors: boolean = false,
                                ignoreLoading: boolean = false): Observable<PageData<DeviceInfo>> {
    return this.http.get<PageData<DeviceInfo>>(`/api/customer/${customerId}/deviceInfos${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getDevice(deviceId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Device> {
    return this.http.get<Device>(`/api/device/${deviceId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getDeviceInfo(deviceId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<DeviceInfo> {
    return this.http.get<DeviceInfo>(`/api/device/info/${deviceId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public saveDevice(device: Device, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Device> {
    return this.http.post<Device>('/api/device', device, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public deleteDevice(deviceId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false) {
    return this.http.delete(`/api/device/${deviceId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getDeviceTypes(ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Array<EntitySubtype>> {
    return this.http.get<Array<EntitySubtype>>('/api/device/types', defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getDeviceCredentials(deviceId: string, sync: boolean = false, ignoreErrors: boolean = false,
                              ignoreLoading: boolean = false): Observable<DeviceCredentials> {
    const url = `/api/device/${deviceId}/credentials`;
    if (sync) {
      const responseSubject = new ReplaySubject<DeviceCredentials>();
      const request = new XMLHttpRequest();
      request.open('GET', url, false);
      request.setRequestHeader('Accept', 'application/json, text/plain, */*');
      const jwtToken = AuthService.getJwtToken();
      if (jwtToken) {
        request.setRequestHeader('X-Authorization', 'Bearer ' + jwtToken);
      }
      request.send(null);
      if (request.status === 200) {
        const credentials = JSON.parse(request.responseText) as DeviceCredentials;
        responseSubject.next(credentials);
      } else {
        responseSubject.error(null);
      }
      return responseSubject.asObservable();
    } else {
      return this.http.get<DeviceCredentials>(url, defaultHttpOptions(ignoreLoading, ignoreErrors));
    }
  }

  public saveDeviceCredentials(deviceCredentials: DeviceCredentials, ignoreErrors: boolean = false,
                               ignoreLoading: boolean = false): Observable<DeviceCredentials> {
    return this.http.post<DeviceCredentials>('/api/device/credentials', deviceCredentials, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public makeDevicePublic(deviceId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Device> {
    return this.http.post<Device>(`/api/customer/public/device/${deviceId}`, null, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public assignDeviceToCustomer(customerId: string, deviceId: string,
                                  ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Device> {
    return this.http.post<Device>(`/api/customer/${customerId}/device/${deviceId}`, null, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public unassignDeviceFromCustomer(deviceId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false) {
    return this.http.delete(`/api/customer/device/${deviceId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

}
