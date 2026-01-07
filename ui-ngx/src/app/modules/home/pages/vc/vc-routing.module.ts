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
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { Authority } from '@shared/models/authority.enum';
import { VersionControlComponent } from '@home/components/vc/version-control.component';
import { MenuId } from '@core/services/menu.models';

export const vcRoutes: Routes = [
  {
    path: 'vc',
    component: VersionControlComponent,
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      auth: [Authority.TENANT_ADMIN],
      title: 'version-control.version-control',
      breadcrumb: {
        menuId: MenuId.version_control
      }
    }
  }
];

const routes: Routes = [
  {
    path: 'vc',
    redirectTo: '/features/vc'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: []
})
export class VcRoutingModule { }
