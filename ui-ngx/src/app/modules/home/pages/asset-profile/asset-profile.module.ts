// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { AssetProfileTabsComponent } from './asset-profile-tabs.component';
import { AssetProfileRoutingModule } from './asset-profile-routing.module';

@NgModule({
  declarations: [
    AssetProfileTabsComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    AssetProfileRoutingModule
  ]
})
export class AssetProfileModule { }
