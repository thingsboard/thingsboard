// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { RolesTableConfigResolver } from '@home/pages/role/roles-table-config.resolver';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { MenuId } from '@core/services/menu.models';

const routes: Routes = [
  {
    path: 'roles',
    data: {
      breadcrumb: {
        menuId: MenuId.roles
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'role.roles'
        },
        resolve: {
          entitiesTableConfig: RolesTableConfigResolver
        }
      },
      {
        path: ':entityId',
        component: EntityDetailsPageComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          breadcrumb: {
            labelFunction: entityDetailsPageBreadcrumbLabelFunction,
            icon: 'security'
          } as BreadCrumbConfig<EntityDetailsPageComponent>,
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'role.roles',
          hideTabs: true
        },
        resolve: {
          entitiesTableConfig: RolesTableConfigResolver
        }
      }
    ]
  }
];

export const rolesRoutes: Routes = [
  {
    path: 'roles',
    pathMatch: 'full',
    redirectTo: '/roles'
  },
  {
    path: 'roles/:entityId',
    redirectTo: '/roles/:entityId'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    RolesTableConfigResolver
  ]
})
export class RoleRoutingModule { }
