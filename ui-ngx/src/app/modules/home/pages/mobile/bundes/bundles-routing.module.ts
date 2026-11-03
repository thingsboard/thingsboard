// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Routes } from '@angular/router';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { MenuId } from '@core/services/menu.models';
import { MobileBundleTableConfigResolver } from '@home/pages/mobile/bundes/mobile-bundle-table-config.resolve';
import { NgModule } from '@angular/core';

export const bundlesRoutes: Routes = [
  {
    path: 'bundles',
    component: EntitiesTableComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
      title: 'mobile.bundles',
      breadcrumb: {
        menuId: MenuId.mobile_bundles
      }
    },
    resolve: {
      entitiesTableConfig: MobileBundleTableConfigResolver
    }
  }
];

@NgModule({
  providers: [
    MobileBundleTableConfigResolver
  ],
  imports: [],
  exports: []
})
export class MobileBundleRoutingModule { }
