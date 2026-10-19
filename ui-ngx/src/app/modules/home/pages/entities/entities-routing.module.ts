// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Route, RouterModule } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { NgModule } from '@angular/core';
import { devicesRoute } from '@home/pages/device/device-routing.module';
import { assetsRoute } from '@home/pages/asset/asset-routing.module';
import { entityViewsRoute } from '@home/pages/entity-view/entity-view-routing.module';
import { gatewaysRoutes } from '@home/pages/gateways/gateways-routing.module';

export const entitiesRoute = (root = false): Route => ({
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
          redirectTo: 'devices'
        }
      },
      devicesRoute(root),
      assetsRoute(root),
      entityViewsRoute(root),
      ...gatewaysRoutes
    ]
  });

@NgModule({
  imports: [RouterModule.forChild([entitiesRoute(true)])],
  exports: [RouterModule]
})
export class EntitiesRoutingModule { }
