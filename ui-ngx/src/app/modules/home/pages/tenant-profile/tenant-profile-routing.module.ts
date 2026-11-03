// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { TenantProfilesTableConfigResolver } from './tenant-profiles-table-config.resolver';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { MenuId } from '@core/services/menu.models';

const routes: Routes = [
  {
    path: 'tenantProfiles',
    data: {
      breadcrumb: {
        menuId: MenuId.tenant_profiles
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.SYS_ADMIN],
          title: 'tenant-profile.tenant-profiles'
        },
        resolve: {
          entitiesTableConfig: TenantProfilesTableConfigResolver
        }
      },
      {
        path: ':entityId',
        component: EntityDetailsPageComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          breadcrumb: {
            labelFunction: entityDetailsPageBreadcrumbLabelFunction,
            icon: 'mdi:alpha-t-box'
          } as BreadCrumbConfig<EntityDetailsPageComponent>,
          auth: [Authority.SYS_ADMIN],
          title: 'tenant-profile.tenant-profiles'
        },
        resolve: {
          entitiesTableConfig: TenantProfilesTableConfigResolver
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    TenantProfilesTableConfigResolver
  ]
})
export class TenantProfileRoutingModule { }
