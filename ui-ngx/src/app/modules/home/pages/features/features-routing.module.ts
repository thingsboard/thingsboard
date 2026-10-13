// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { NgModule } from '@angular/core';
import { otaUpdatesRoutes } from '@home/pages/ota-update/ota-update-routing.module';
import { vcRoutes } from '@home/pages/vc/vc-routing.module';
import { schedulerRoutes } from '@home/pages/scheduler/scheduler-routing.module';
import { MenuId } from '@core/services/menu.models';
import { taskManagerRoutes } from '@home/pages/task-manager/task-manager-routing.module';

const routes: Routes = [
  {
    path: 'features',
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {
        menuId: MenuId.features,
        skip: true
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          redirectTo: {
            TENANT_ADMIN: '/features/otaUpdates',
            CUSTOMER_USER: '/features/scheduler'
          }
        }
      },
      ...otaUpdatesRoutes,
      ...vcRoutes,
      ...schedulerRoutes,
      ...taskManagerRoutes
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class FeaturesRoutingModule { }
