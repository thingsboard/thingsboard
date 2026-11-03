// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
