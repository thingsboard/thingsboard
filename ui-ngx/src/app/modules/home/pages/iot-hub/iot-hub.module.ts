// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { IotHubComponentsModule } from '@home/components/iot-hub/iot-hub-components.module';
import { IotHubRoutingModule } from './iot-hub-routing.module';
import { TbIotHubHomeComponent } from './iot-hub-home.component';
import { TbIotHubItemsPageComponent } from './iot-hub-items-page.component';
import { TbIotHubAlarmRulesUnavailablePageComponent } from './iot-hub-alarm-rules-unavailable-page.component';
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
    TbIotHubItemResolverComponent,
    TbIotHubAlarmRulesUnavailablePageComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    IotHubComponentsModule,
    IotHubRoutingModule
  ]
})
export class IotHubModule { }
