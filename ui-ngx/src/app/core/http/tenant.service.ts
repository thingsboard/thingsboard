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
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { Tenant, TenantInfo } from '@shared/models/tenant.model';

@Injectable({
  providedIn: 'root'
})
export class TenantService {

  constructor(
    private http: HttpClient
  ) { }

  public getTenants(pageLink: PageLink, config?: RequestConfig): Observable<PageData<Tenant>> {
    return this.http.get<PageData<Tenant>>(`/api/tenants${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getTenantInfos(pageLink: PageLink, config?: RequestConfig): Observable<PageData<TenantInfo>> {
    return this.http.get<PageData<TenantInfo>>(`/api/tenantInfos${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getTenant(tenantId: string, config?: RequestConfig): Observable<Tenant> {
    return this.http.get<Tenant>(`/api/tenant/${tenantId}`, defaultHttpOptionsFromConfig(config));
  }

  public getTenantInfo(tenantId: string, config?: RequestConfig): Observable<TenantInfo> {
    return this.http.get<TenantInfo>(`/api/tenant/info/${tenantId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveTenant(tenant: Tenant, config?: RequestConfig): Observable<Tenant> {
    return this.http.post<Tenant>('/api/tenant', tenant, defaultHttpOptionsFromConfig(config));
  }

  public deleteTenant(tenantId: string, config?: RequestConfig) {
    return this.http.delete(`/api/tenant/${tenantId}`, defaultHttpOptionsFromConfig(config));
  }

}
