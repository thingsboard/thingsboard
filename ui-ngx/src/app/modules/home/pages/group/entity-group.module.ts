// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { EntityGroupRoutingModule } from '@home/pages/group/entity-group-routing.module';
import { DeviceModule } from '@home/pages/device/device.module';
import { AssetModule } from '@home/pages/asset/asset.module';
import { EntityViewModule } from '@home/pages/entity-view/entity-view.module';
import { DashboardModule } from '@home/pages/dashboard/dashboard.module';
import { UserModule } from '@home/pages/user/user.module';
import { CustomerModule } from '@home/pages/customer/customer.module';
import { EdgeModule } from '@home/pages/edge/edge.module';

@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    DeviceModule,
    AssetModule,
    EntityViewModule,
    DashboardModule,
    UserModule,
    CustomerModule,
    EntityGroupRoutingModule,
    EdgeModule
  ],
  providers: [
  ]
})
export class EntityGroupModule { }
