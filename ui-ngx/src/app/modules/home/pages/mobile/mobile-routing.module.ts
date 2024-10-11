///
/// Copyright © 2016-2024 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { Authority } from '@shared/models/authority.enum';
import { MenuId } from '@core/services/menu.models';
import { MobileAppTableConfigResolver } from '@home/pages/mobile/applications/mobile-app-table-config.resolver';
import { MobileAppSettingsComponent } from '@home/pages/admin/mobile-app-settings.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { applicationsRoutes } from '@home/pages/mobile/applications/applications-routing.module';

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
          redirectTo: '/mobile-center/applications'
        }
      },
      ...applicationsRoutes,
      {
        path: 'mobile-app',
        component: MobileAppSettingsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN],
          title: 'admin.mobile-app.mobile-app',
          breadcrumb: {
            menuId: MenuId.mobile_app_settings
          }
        }
      }
    ]
  }
];

routes.push(
  {
    path: 'security-settings/oauth2/mobile-applications',
    pathMatch: 'full',
    redirectTo: '/mobile-center/applications'
  }
);

@NgModule({
  providers: [
    MobileAppTableConfigResolver
  ],
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class MobileRoutingModule { }
