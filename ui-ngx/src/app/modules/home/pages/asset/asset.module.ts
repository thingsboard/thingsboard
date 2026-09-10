// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { AssetComponent } from './asset.component';
import { AssetTableHeaderComponent } from './asset-table-header.component';
import { AssetRoutingModule } from './asset-routing.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { AssetTabsComponent } from '@home/pages/asset/asset-tabs.component';

@NgModule({
  declarations: [
    AssetComponent,
    AssetTabsComponent,
    AssetTableHeaderComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    AssetRoutingModule
  ]
})
export class AssetModule { }
