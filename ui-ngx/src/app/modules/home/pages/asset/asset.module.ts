// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { AssetComponent } from './asset.component';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { ASSET_GROUP_CONFIG_FACTORY } from '@home/models/group/group-entities-table-config.models';
import { AssetGroupConfigFactory } from '@home/pages/asset/asset-group-config.factory';
import { AssetRoutingModule } from '@home/pages/asset/asset-routing.module';
import { AssetTableHeaderComponent } from '@home/pages/asset/asset-table-header.component';

@NgModule({
  declarations: [
    AssetComponent,
    AssetTableHeaderComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    AssetRoutingModule,
  ],
  providers: [
    {
      provide: ASSET_GROUP_CONFIG_FACTORY,
      useClass: AssetGroupConfigFactory
    }
  ]
})
export class AssetModule { }
