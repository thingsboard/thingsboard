// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { NgModule } from '@angular/core';
import { otaUpdatesRoutes } from '@home/pages/ota-update/ota-update-routing.module';
import { vcRoutes } from '@home/pages/vc/vc-routing.module';
import { MenuId } from '@core/services/menu.models';

const routes: Routes = [
  {
    path: 'features',
    data: {
      auth: [Authority.TENANT_ADMIN],
      breadcrumb: {
        menuId: MenuId.features
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN],
          redirectTo: '/features/otaUpdates'
        }
      },
      ...otaUpdatesRoutes,
      ...vcRoutes
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class FeaturesRoutingModule { }
