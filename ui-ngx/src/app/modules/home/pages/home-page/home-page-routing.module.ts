// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { inject, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, ResolveFn, RouterModule, RouterStateSnapshot, Routes } from '@angular/router';

import { HomePageComponent } from './home-page.component';
import { Authority } from '@shared/models/authority.enum';
import { mergeMap, Observable, of } from 'rxjs';
import { HomeDashboard } from '@shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { catchError, first, map } from 'rxjs/operators';
import { MenuId } from '@core/services/menu.models';
import { MenuService } from '@core/services/menu.service';

const resolveMenuHomeDashboard = (menuService: MenuService,
                                  dashboardService: DashboardService): Observable<HomeDashboard> =>
  menuService.menuSections().pipe(first()).pipe(
    mergeMap((sections) => {
      const homeSection = sections.find(s => s.id === MenuId.home);
      if (homeSection?.homeDashboardId) {
        return dashboardService.getDashboard(homeSection.homeDashboardId, {ignoreErrors: true}).pipe(
          map((dashboard) => ({
            ...dashboard,
            hideDashboardToolbar: homeSection.homeHideDashboardToolbar
          })),
          catchError(() => of(null))
        );
      }
      return of(null);
    })
  );

const resolveHomeDashboard = (menuService: MenuService,
                              dashboardService: DashboardService): Observable<HomeDashboard> =>
  dashboardService.getHomeDashboard().pipe(
    mergeMap((dashboard) => {
      if (!dashboard) {
        return resolveMenuHomeDashboard(menuService, dashboardService);
      }
      return of(dashboard);
    }),
    catchError(() => resolveMenuHomeDashboard(menuService, dashboardService))
  );

export const homeDashboardResolver: ResolveFn<HomeDashboard> = (
  _route: ActivatedRouteSnapshot,
  _state: RouterStateSnapshot,
  menuService = inject(MenuService),
  dashboardService = inject(DashboardService)
): Observable<HomeDashboard> => resolveHomeDashboard(menuService, dashboardService);

const routes: Routes = [
  {
    path: 'home',
    component: HomePageComponent,
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
export class HomePageRoutingModule { }
