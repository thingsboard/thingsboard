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
import { TbIotHubBrowseComponent } from './iot-hub-browse.component';
import { TbIotHubCreatorProfileComponent } from './iot-hub-creator-profile.component';
import { TbIotHubInstalledItemsComponent } from './iot-hub-installed-items.component';

const routes: Routes = [
  {
    path: 'iot-hub',
    data: {
      auth: [Authority.TENANT_ADMIN],
      breadcrumb: {
        label: 'iot-hub.iot-hub',
        icon: 'store'
      }
    },
    children: [
      {
        path: '',
        component: TbIotHubBrowseComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'iot-hub.browse'
        }
      },
      {
        path: 'installed',
        component: TbIotHubInstalledItemsComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'iot-hub.installed-items',
          breadcrumb: {
            label: 'iot-hub.installed-items',
            icon: 'inventory_2'
          }
        }
      },
      {
        path: 'creator/:creatorId',
        component: TbIotHubCreatorProfileComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'iot-hub.creator-profile',
          breadcrumb: {
            label: 'iot-hub.creator-profile',
            icon: 'person'
          }
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class IotHubRoutingModule { }
