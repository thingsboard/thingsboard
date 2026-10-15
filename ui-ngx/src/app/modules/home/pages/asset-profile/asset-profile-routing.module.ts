// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { Routes } from '@angular/router';

import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { AssetProfilesTableConfigResolver } from './asset-profiles-table-config.resolver';
import { MenuId } from '@core/services/menu.models';

export const assetProfilesRoutes: Routes = [
  {
    path: 'assetProfiles',
    data: {
      breadcrumb: {
        menuId: MenuId.asset_profiles
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'asset-profile.asset-profiles'
        },
        resolve: {
          entitiesTableConfig: AssetProfilesTableConfigResolver
        }
      },
      {
        path: ':entityId',
        component: EntityDetailsPageComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          breadcrumb: {
            labelFunction: entityDetailsPageBreadcrumbLabelFunction,
            icon: 'mdi:alpha-a-box'
          } as BreadCrumbConfig<EntityDetailsPageComponent>,
          auth: [Authority.TENANT_ADMIN],
          title: 'asset-profile.asset-profiles'
        },
        resolve: {
          entitiesTableConfig: AssetProfilesTableConfigResolver
        }
      }
    ]
  }
];

@NgModule({
  providers: [
    AssetProfilesTableConfigResolver
  ]
})
export class AssetProfileRoutingModule { }
