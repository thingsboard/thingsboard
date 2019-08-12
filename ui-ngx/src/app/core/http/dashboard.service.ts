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
import { Observable } from 'rxjs/index';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { Tenant } from '@shared/models/tenant.model';
import {DashboardInfo, Dashboard} from '@shared/models/dashboard.models';
import {map} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  constructor(
    private http: HttpClient
  ) { }

  public getTenantDashboards(pageLink: PageLink, ignoreErrors: boolean = false,
                             ignoreLoading: boolean = false): Observable<PageData<DashboardInfo>> {
    return this.http.get<PageData<DashboardInfo>>(`/api/tenant/dashboards${pageLink.toQuery()}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getTenantDashboardsByTenantId(tenantId: string, pageLink: PageLink, ignoreErrors: boolean = false,
                                       ignoreLoading: boolean = false): Observable<PageData<DashboardInfo>> {
    return this.http.get<PageData<DashboardInfo>>(`/api/tenant/${tenantId}/dashboards${pageLink.toQuery()}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getCustomerDashboards(customerId: string, pageLink: PageLink, ignoreErrors: boolean = false,
                               ignoreLoading: boolean = false): Observable<PageData<DashboardInfo>> {
    return this.http.get<PageData<DashboardInfo>>(`/api/customer/${customerId}/dashboards${pageLink.toQuery()}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors)).pipe(
      map( dashboards => {
        dashboards.data = dashboards.data.filter(dashboard => {
          return dashboard.title.toUpperCase().includes(pageLink.textSearch.toUpperCase());
        });
        return dashboards;
      }
    ));
  }

  public getDashboard(dashboardId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Dashboard> {
    return this.http.get<Dashboard>(`/api/dashboard/${dashboardId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getDashboardInfo(dashboardId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<DashboardInfo> {
    return this.http.get<DashboardInfo>(`/api/dashboard/info/${dashboardId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public saveDashboard(dashboard: Dashboard, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Dashboard> {
    return this.http.post<Dashboard>('/api/dashboard', dashboard, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public deleteDashboard(dashboardId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false) {
    return this.http.delete(`/api/dashboard/${dashboardId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

}
