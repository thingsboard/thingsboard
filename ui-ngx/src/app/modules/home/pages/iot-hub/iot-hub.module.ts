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
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { IotHubRoutingModule } from './iot-hub-routing.module';
import { TbIotHubBrowseComponent } from './iot-hub-browse.component';
import { TbIotHubCreatorProfileComponent } from './iot-hub-creator-profile.component';
import { TbIotHubItemDetailDialogComponent } from './iot-hub-item-detail-dialog.component';
import { TbIotHubInstallDialogComponent } from './iot-hub-install-dialog.component';
import { TbIotHubItemCardComponent } from './iot-hub-item-card.component';
import { TbIotHubInstalledItemsComponent } from './iot-hub-installed-items.component';
import { TbIotHubUpdateDialogComponent } from './iot-hub-update-dialog.component';

@NgModule({
  declarations: [
    TbIotHubBrowseComponent,
    TbIotHubCreatorProfileComponent,
    TbIotHubItemDetailDialogComponent,
    TbIotHubInstallDialogComponent,
    TbIotHubItemCardComponent,
    TbIotHubInstalledItemsComponent,
    TbIotHubUpdateDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    IotHubRoutingModule
  ]
})
export class IotHubModule { }
