// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
