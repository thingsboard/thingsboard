///
/// Copyright © 2016-2021 The Thingsboard Authors
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
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { NgModule } from '@angular/core';
import { OtaUpdateTableConfigResolve } from '@home/pages/ota-update/ota-update-table-config.resolve';

const routes: Routes = [
  {
    path: 'otaUpdates',
    component: EntitiesTableComponent,
    data: {
      auth: [Authority.TENANT_ADMIN],
      title: 'ota-update.ota-updates',
      breadcrumb: {
        label: 'ota-update.ota-updates',
        icon: 'memory'
      }
    },
    resolve: {
      entitiesTableConfig: OtaUpdateTableConfigResolve
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    OtaUpdateTableConfigResolve
  ]
})
export class OtaUpdateRoutingModule { }
