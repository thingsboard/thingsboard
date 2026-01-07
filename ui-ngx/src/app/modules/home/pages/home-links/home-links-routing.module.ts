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

import { inject, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, ResolveFn, RouterModule, RouterStateSnapshot, Routes } from '@angular/router';

import { HomeLinksComponent } from './home-links.component';
import { Authority } from '@shared/models/authority.enum';
import { mergeMap, Observable, of } from 'rxjs';
import { HomeDashboard } from '@shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { map } from 'rxjs/operators';
import {
  getCurrentAuthUser,
  selectHomeDashboardParams,
  selectMobileQrEnabled,
  selectPersistDeviceStateToTelemetry
} from '@core/auth/auth.selectors';
import { EntityKeyType } from '@shared/models/query/query.models';
import { ResourcesService } from '@core/services/resources.service';
import { isDefinedAndNotNull } from '@core/utils';
import { MenuId } from '@core/services/menu.models';

const sysAdminHomePageJson = '/assets/dashboard/sys_admin_home_page.json';
const tenantAdminHomePageJson = '/assets/dashboard/tenant_admin_home_page.json';
const customerUserHomePageJson = '/assets/dashboard/customer_user_home_page.json';

const getHomeDashboard = (store: Store<AppState>, resourcesService: ResourcesService) => {
  const authority = getCurrentAuthUser(store).authority;
  switch (authority) {
    case Authority.SYS_ADMIN:
      return applySystemParametersToHomeDashboard(store, resourcesService.loadJsonResource(sysAdminHomePageJson), authority);
    case Authority.TENANT_ADMIN:
      return applySystemParametersToHomeDashboard(store, resourcesService.loadJsonResource(tenantAdminHomePageJson), authority);
    case Authority.CUSTOMER_USER:
      return applySystemParametersToHomeDashboard(store, resourcesService.loadJsonResource(customerUserHomePageJson), authority);
    default:
      return of(null);
  }
};

const applySystemParametersToHomeDashboard = (store: Store<AppState>,
                                              dashboard$: Observable<HomeDashboard>,
                                              authority: Authority): Observable<HomeDashboard> => {
  let selectParams$: Observable<{persistDeviceStateToTelemetry?: boolean, mobileQrEnabled?: boolean}>;
  switch (authority) {
    case Authority.SYS_ADMIN:
      selectParams$ = store.pipe(
        select(selectMobileQrEnabled),
        map(mobileQrEnabled => ({mobileQrEnabled}))
      );
      break;
    case Authority.TENANT_ADMIN:
      selectParams$ = store.pipe(select(selectHomeDashboardParams));
      break;
    case Authority.CUSTOMER_USER:
      selectParams$ = store.pipe(
        select(selectPersistDeviceStateToTelemetry),
        map(persistDeviceStateToTelemetry => ({persistDeviceStateToTelemetry}))
      );
      break;
  }
  return selectParams$.pipe(
    mergeMap((params) => dashboard$.pipe(
      map((dashboard) => {
        if (params.persistDeviceStateToTelemetry) {
          for (const filterId of Object.keys(dashboard.configuration.filters)) {
            if (['Active Devices', 'Inactive Devices'].includes(dashboard.configuration.filters[filterId].filter)) {
              dashboard.configuration.filters[filterId].keyFilters[0].key.type = EntityKeyType.TIME_SERIES;
            }
          }
        }
        if (isDefinedAndNotNull(params.mobileQrEnabled)) {
          for (const widgetId of Object.keys(dashboard.configuration.widgets)) {
            if (dashboard.configuration.widgets[widgetId].config.title === 'Select show mobile QR code') {
              dashboard.configuration.widgets[widgetId].config.settings.markdownTextFunction =
                (dashboard.configuration.widgets[widgetId].config.settings.markdownTextFunction as string)
                  .replace(/\${mobileQrEnabled:([^}]+)}/, `\${mobileQrEnabled:${String(params.mobileQrEnabled)}}`);
              break;
            }
          }
        }
        dashboard.hideDashboardToolbar = true;
        return dashboard;
      })
    ))
  );
};

export const homeDashboardResolver: ResolveFn<HomeDashboard> = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  dashboardService = inject(DashboardService),
  resourcesService = inject(ResourcesService),
  store: Store<AppState> = inject(Store<AppState>)
): Observable<HomeDashboard> =>
  dashboardService.getHomeDashboard().pipe(
    mergeMap((dashboard) => {
      if (!dashboard) {
        return getHomeDashboard(store, resourcesService);
      }
      return of(dashboard);
    })
  );

const routes: Routes = [
  {
    path: 'home',
    component: HomeLinksComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'home.home',
      breadcrumb: {
        menuId: MenuId.home
      }
    },
    resolve: {
      homeDashboard: homeDashboardResolver
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class HomeLinksRoutingModule { }
