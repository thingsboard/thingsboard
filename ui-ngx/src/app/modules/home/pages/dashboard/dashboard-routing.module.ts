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

import { Injectable, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, RouterModule, Routes } from '@angular/router';

import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { DashboardsTableConfigResolver } from './dashboards-table-config.resolver';
import { DashboardPageComponent } from '@home/components/dashboard-page/dashboard-page.component';
import { BreadCrumbConfig, BreadCrumbLabelFunction } from '@shared/components/breadcrumb';
import { mergeMap, Observable, of } from 'rxjs';
import { Dashboard } from '@app/shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { catchError, map } from 'rxjs/operators';
import { UserSettingsService } from '@core/http/user-settings.service';
import { UserDashboardAction } from '@shared/models/user-settings.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { MenuId } from '@core/services/menu.models';

@Injectable()
export class DashboardResolver  {

  constructor(private store: Store<AppState>,
              private dashboardService: DashboardService,
              private userSettingService: UserSettingsService,
              private dashboardUtils: DashboardUtilsService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<Dashboard> {
    const dashboardId = route.params.dashboardId;
    return this.dashboardService.getDashboard(dashboardId).pipe(
      mergeMap((dashboard) =>
        (getCurrentAuthUser(this.store).isPublic ? of(null) :
          this.userSettingService.reportUserDashboardAction(dashboardId, UserDashboardAction.VISIT,
            {ignoreLoading: true, ignoreErrors: true})).pipe(
          catchError(() => of(dashboard)),
          map(() => dashboard)
        )),
      map((dashboard) => this.dashboardUtils.validateAndUpdateDashboard(dashboard))
    );
  }
}

export const dashboardBreadcumbLabelFunction: BreadCrumbLabelFunction<DashboardPageComponent>
  = ((route, translate, component) => component.dashboard.title);

const routes: Routes = [
  {
    path: 'dashboards',
    data: {
      breadcrumb: {
        menuId: MenuId.dashboards
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'dashboard.dashboards',
          dashboardsType: 'tenant'
        },
        resolve: {
          entitiesTableConfig: DashboardsTableConfigResolver
        }
      },
      {
        path: ':dashboardId',
        component: DashboardPageComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          breadcrumb: {
            labelFunction: dashboardBreadcumbLabelFunction,
            icon: 'dashboard'
          } as BreadCrumbConfig<DashboardPageComponent>,
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'dashboard.dashboard',
          widgetEditMode: false
        },
        resolve: {
          dashboard: DashboardResolver
        }
      }
    ]
  }
];

// @dynamic
@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    DashboardsTableConfigResolver,
    DashboardResolver
  ]
})
export class DashboardRoutingModule { }
