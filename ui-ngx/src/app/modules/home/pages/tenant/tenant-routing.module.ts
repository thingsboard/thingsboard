// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { TenantsTableConfigResolver } from '@modules/home/pages/tenant/tenants-table-config.resolver';
import { UsersTableConfigResolver } from '../user/users-table-config.resolver';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { MenuId } from '@core/services/menu.models';

const routes: Routes = [
  {
    path: 'tenants',
    data: {
      breadcrumb: {
        menuId: MenuId.tenants
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.SYS_ADMIN],
          title: 'tenant.tenants'
        },
        resolve: {
          entitiesTableConfig: TenantsTableConfigResolver
        }
      },
      {
        path: ':entityId',
        component: EntityDetailsPageComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          breadcrumb: {
            labelFunction: entityDetailsPageBreadcrumbLabelFunction,
            icon: 'supervisor_account'
          } as BreadCrumbConfig<EntityDetailsPageComponent>,
          auth: [Authority.SYS_ADMIN],
          title: 'tenant.tenants'
        },
        resolve: {
          entitiesTableConfig: TenantsTableConfigResolver
        }
      },
      {
        path: ':tenantId/users',
        data: {
          breadcrumb: {
            label: 'user.tenant-admins',
            icon: 'account_circle'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.SYS_ADMIN],
              title: 'user.tenant-admins'
            },
            resolve: {
              entitiesTableConfig: UsersTableConfigResolver
            }
          },
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'account_circle'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.SYS_ADMIN],
              title: 'user.tenant-admins'
            },
            resolve: {
              entitiesTableConfig: UsersTableConfigResolver
            }
          }
        ]
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    TenantsTableConfigResolver
  ]
})
export class TenantRoutingModule { }
