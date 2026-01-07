///
/// Copyright © 2016-2026 The Thingsboard Authors
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
import { Authority } from '@shared/models/authority.enum';
import { MenuId } from '@core/services/menu.models';
import {
  MobileQrCodeWidgetSettingsComponent
} from '@home/pages/mobile/qr-code-widget/mobile-qr-code-widget-settings.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';

export const qrCodeWidgetRoutes: Routes = [
  {
    path: 'qr-code-widget',
    component: MobileQrCodeWidgetSettingsComponent,
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
      title: 'mobile.qr-code-widget',
      breadcrumb: {
        menuId: MenuId.mobile_qr_code_widget
      }
    }
  }
];

const routes: Routes = [
  {
    path: 'settings/mobile-app',
    pathMatch: 'full',
    redirectTo: '/mobile-center/qr-code-widget'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class MobileQrCodeWidgetSettingsRoutingModule { }
