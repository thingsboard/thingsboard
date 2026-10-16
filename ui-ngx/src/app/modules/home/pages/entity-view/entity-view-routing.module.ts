// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { EntityViewsTableConfigResolver } from '@modules/home/pages/entity-view/entity-views-table-config.resolver';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { MenuId } from '@core/services/menu.models';

export const entityViewRoutes: Routes = [
  {
    path: 'entityViews',
    data: {
      breadcrumb: {
        menuId: MenuId.entity_views
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'entity-view.entity-views',
          entityViewsType: 'tenant'
        },
        resolve: {
          entitiesTableConfig: EntityViewsTableConfigResolver
        }
      },
      {
        path: ':entityId',
        component: EntityDetailsPageComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          breadcrumb: {
            labelFunction: entityDetailsPageBreadcrumbLabelFunction,
            icon: 'view_quilt'
          } as BreadCrumbConfig<EntityDetailsPageComponent>,
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'entity-view.entity-views',
          entityViewsType: 'tenant'
        },
        resolve: {
          entitiesTableConfig: EntityViewsTableConfigResolver
        }
      }
    ]
  }
];

const routes: Routes = [
  {
    path: 'entityViews',
    pathMatch: 'full',
    redirectTo: '/entities/entityViews'
  },
  {
    path: 'entityViews/:entityId',
    redirectTo: '/entities/entityViews/:entityId'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    EntityViewsTableConfigResolver
  ]
})
export class EntityViewRoutingModule { }
