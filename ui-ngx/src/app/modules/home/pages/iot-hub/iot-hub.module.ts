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
import { IotHubComponentsModule } from '@home/components/iot-hub/iot-hub-components.module';
import { IotHubRoutingModule } from './iot-hub-routing.module';
import { TbIotHubHomeComponent } from './iot-hub-home.component';
import { TbIotHubItemsPageComponent } from './iot-hub-items-page.component';
import { TbIotHubCreatorProfileComponent } from './iot-hub-creator-profile.component';
import { TbIotHubInstalledItemsComponent } from './iot-hub-installed-items.component';
import { TbIotHubSearchPageComponent } from './iot-hub-search-page.component';
import { TbIotHubItemResolverComponent } from './iot-hub-item-resolver.component';

@NgModule({
  declarations: [
    TbIotHubHomeComponent,
    TbIotHubItemsPageComponent,
    TbIotHubCreatorProfileComponent,
    TbIotHubInstalledItemsComponent,
    TbIotHubSearchPageComponent,
    TbIotHubItemResolverComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    IotHubComponentsModule,
    IotHubRoutingModule
  ]
})
export class IotHubModule { }
