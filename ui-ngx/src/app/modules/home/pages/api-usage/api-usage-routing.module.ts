// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { inject, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, ResolveFn, RouterModule, RouterStateSnapshot, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { Dashboard } from '@shared/models/dashboard.models';
import { ResourcesService } from '@core/services/resources.service';
import { Observable } from 'rxjs';
import { MenuId } from '@core/services/menu.models';
import { DashboardViewComponent } from '@home/components/dashboard-view/dashboard-view.component';

const apiUsageDashboardJson = '/assets/dashboard/api_usage.json';

export const apiUsageDashboardResolver: ResolveFn<Dashboard> = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  resourcesService = inject(ResourcesService)
): Observable<Dashboard> => resourcesService.loadJsonResource(apiUsageDashboardJson);

const routes: Routes = [
  {
    path: 'usage',
    component: DashboardViewComponent,
    data: {
      auth: [Authority.TENANT_ADMIN],
      title: 'api-usage.api-usage',
      breadcrumb: {
        menuId: MenuId.api_usage
      }
    },
    resolve: {
      dashboard: apiUsageDashboardResolver
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ApiUsageRoutingModule { }
