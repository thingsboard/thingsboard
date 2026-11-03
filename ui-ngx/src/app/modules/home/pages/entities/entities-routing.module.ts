// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { NgModule } from '@angular/core';
import { deviceRoutes } from '@home/pages/device/device-routing.module';
import { assetRoutes } from '@home/pages/asset/asset-routing.module';
import { entityViewRoutes } from '@home/pages/entity-view/entity-view-routing.module';
import { gatewaysRoutes } from '@home/pages/gateways/gateways-routing.module';

const routes: Routes = [
  {
    path: 'entities',
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {
        skip: true
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          redirectTo: '/entities/devices'
        }
      },
      ...deviceRoutes,
      ...assetRoutes,
      ...entityViewRoutes,
      ...gatewaysRoutes
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class EntitiesRoutingModule { }
