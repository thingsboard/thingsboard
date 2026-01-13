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

import { RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { NgModule } from '@angular/core';
import { deviceProfilesRoutes } from '@home/pages/device-profile/device-profile-routing.module';
import { assetProfilesRoutes } from '@home/pages/asset-profile/asset-profile-routing.module';
import { MenuId } from '@core/services/menu.models';

const routes: Routes = [
  {
    path: 'profiles',
    data: {
      auth: [Authority.TENANT_ADMIN],
      breadcrumb: {
        menuId: MenuId.profiles
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN],
          redirectTo: '/profiles/deviceProfiles'
        }
      },
      ...deviceProfilesRoutes,
      ...assetProfilesRoutes
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ProfilesRoutingModule { }
