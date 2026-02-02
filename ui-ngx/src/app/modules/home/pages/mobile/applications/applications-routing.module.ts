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

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { MenuId } from '@core/services/menu.models';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { MobileAppTableConfigResolver } from '@home/pages/mobile/applications/mobile-app-table-config.resolver';
import { DevicesTableConfigResolver } from '@home/pages/device/devices-table-config.resolver';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';

export const applicationsRoutes: Routes = [
  {
    path: 'applications',
    data: {
      breadcrumb: {
        menuId: MenuId.mobile_apps
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
          title: 'mobile.applications',
        },
        resolve: {
          entitiesTableConfig: MobileAppTableConfigResolver
        }
      },
      {
        path: ':entityId',
        component: EntityDetailsPageComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          breadcrumb: {
            labelFunction: entityDetailsPageBreadcrumbLabelFunction,
            icon: 'list'
          } as BreadCrumbConfig<EntityDetailsPageComponent>,
          auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
          title: 'mobile.applications',
        },
        resolve: {
          entitiesTableConfig: MobileAppTableConfigResolver
        }
      }
    ]
  }
];

const routes: Routes = [
  {
    path: 'security-settings/oauth2/mobile-applications',
    pathMatch: 'full',
    redirectTo: '/mobile-center/applications'
  }
];

@NgModule({
  providers: [
    MobileAppTableConfigResolver
  ],
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ApplicationsRoutingModule { }
