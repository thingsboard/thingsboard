// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { Authority } from '@shared/models/authority.enum';
import { MenuId } from '@core/services/menu.models';
import { applicationsRoutes } from '@home/pages/mobile/applications/applications-routing.module';
import { bundlesRoutes } from '@home/pages/mobile/bundes/bundles-routing.module';
import { qrCodeWidgetRoutes } from '@home/pages/mobile/qr-code-widget/mobile-qr-code-widget-settings-routing.module';

const routes: Routes = [
  {
    path: 'mobile-center',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
      breadcrumb: {
        menuId: MenuId.mobile_center
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER, Authority.SYS_ADMIN],
          redirectTo: '/mobile-center/bundles'
        }
      },
      ...applicationsRoutes,
      ...bundlesRoutes,
      ...qrCodeWidgetRoutes
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class MobileRoutingModule { }
