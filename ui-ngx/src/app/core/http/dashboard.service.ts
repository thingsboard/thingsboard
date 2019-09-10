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

import {Inject, Injectable} from '@angular/core';
import {defaultHttpOptions} from './http-utils';
import { Observable, ReplaySubject, Subject } from 'rxjs/index';
import {HttpClient} from '@angular/common/http';
import {PageLink} from '@shared/models/page/page-link';
import {PageData} from '@shared/models/page/page-data';
import {Dashboard, DashboardInfo} from '@shared/models/dashboard.models';
import {WINDOW} from '@core/services/window.service';
import { ActivationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  stDiffSubject: Subject<number>;

  constructor(
    private http: HttpClient,
    private router: Router,
    @Inject(WINDOW) private window: Window
  ) {
    this.router.events.pipe(filter(event => event instanceof ActivationEnd)).subscribe(
      () => {
        if (this.stDiffSubject) {
          this.stDiffSubject.complete();
          this.stDiffSubject = null;
        }
      }
    );
  }

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
      defaultHttpOptions(ignoreLoading, ignoreErrors));
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

  public assignDashboardToCustomer(customerId: string, dashboardId: string,
                                   ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Dashboard> {
    return this.http.post<Dashboard>(`/api/customer/${customerId}/dashboard/${dashboardId}`,
      null, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public unassignDashboardFromCustomer(customerId: string, dashboardId: string,
                                       ignoreErrors: boolean = false, ignoreLoading: boolean = false) {
    return this.http.delete(`/api/customer/${customerId}/dashboard/${dashboardId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public makeDashboardPublic(dashboardId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Dashboard> {
    return this.http.post<Dashboard>(`/api/customer/public/dashboard/${dashboardId}`, null,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public makeDashboardPrivate(dashboardId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Dashboard> {
    return this.http.delete<Dashboard>(`/api/customer/public/dashboard/${dashboardId}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public updateDashboardCustomers(dashboardId: string, customerIds: Array<string>,
                                  ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Dashboard> {
    return this.http.post<Dashboard>(`/api/dashboard/${dashboardId}/customers`, customerIds,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public addDashboardCustomers(dashboardId: string, customerIds: Array<string>,
                               ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Dashboard> {
    return this.http.post<Dashboard>(`/api/dashboard/${dashboardId}/customers/add`, customerIds,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public removeDashboardCustomers(dashboardId: string, customerIds: Array<string>,
                                  ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Dashboard> {
    return this.http.post<Dashboard>(`/api/dashboard/${dashboardId}/customers/remove`, customerIds,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getPublicDashboardLink(dashboard: DashboardInfo): string | null {
    if (dashboard && dashboard.assignedCustomers && dashboard.assignedCustomers.length > 0) {
      const publicCustomers = dashboard.assignedCustomers
        .filter(customerInfo => customerInfo.public);
      if (publicCustomers.length > 0) {
        const publicCustomerId = publicCustomers[0].customerId.id;
        let url = this.window.location.protocol + '//' + this.window.location.hostname;
        const port = this.window.location.port;
        if (port !== '80' && port !== '443') {
          url += ':' + port;
        }
        url += `/dashboard/${dashboard.id.id}?publicId=${publicCustomerId}`;
        return url;
      }
    }
    return null;
  }

  public getServerTimeDiff(): Observable<number> {
    if (this.stDiffSubject) {
      return this.stDiffSubject.asObservable();
    } else {
      this.stDiffSubject = new ReplaySubject<number>(1);
      const url = '/api/dashboard/serverTime';
      const ct1 = Date.now();
      this.http.get<number>(url, defaultHttpOptions(true)).subscribe(
        (st) => {
          const ct2 = Date.now();
          const stDiff = Math.ceil(st - (ct1 + ct2) / 2);
          this.stDiffSubject.next(stDiff);
        },
        () => {
          this.stDiffSubject.error(null);
        }
      );
      return this.stDiffSubject.asObservable();
    }
  }

}
