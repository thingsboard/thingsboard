// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { RouterModule, Routes } from '@angular/router';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { NgModule } from '@angular/core';
import { OtaUpdateTableConfigResolve } from '@home/pages/ota-update/ota-update-table-config.resolve';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { MenuId } from '@core/services/menu.models';

export const otaUpdatesRoutes: Routes = [
  {
    path: 'otaUpdates',
    data: {
      breadcrumb: {
        menuId: MenuId.otaUpdates
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'ota-update.ota-updates'
        },
        resolve: {
          entitiesTableConfig: OtaUpdateTableConfigResolve
        }
      },
      {
        path: ':entityId',
        component: EntityDetailsPageComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          breadcrumb: {
            labelFunction: entityDetailsPageBreadcrumbLabelFunction,
            icon: 'memory'
          } as BreadCrumbConfig<EntityDetailsPageComponent>,
          auth: [Authority.TENANT_ADMIN],
          title: 'ota-update.ota-updates'
        },
        resolve: {
          entitiesTableConfig: OtaUpdateTableConfigResolve
        }
      }
    ]
  }
];

const routes: Routes = [
  {
    path: 'otaUpdates',
    pathMatch: 'full',
    redirectTo: '/features/otaUpdates'
  },
  {
    path: 'otaUpdates/:entityId',
    redirectTo: '/features/otaUpdates/:entityId'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    OtaUpdateTableConfigResolve
  ]
})
export class OtaUpdateRoutingModule { }
