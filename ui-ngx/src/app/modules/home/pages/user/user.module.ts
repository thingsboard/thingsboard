// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { UserComponent } from '@modules/home/pages/user/user.component';
import { UserRoutingModule } from '@modules/home/pages/user/user-routing.module';
import { ActivationLinkDialogComponent } from '@modules/home/pages/user/activation-link-dialog.component';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { UserTabsComponent } from '@home/pages/user/user-tabs.component';
import { AddUserDialogComponent } from '@home/pages/user/add-user-dialog.component';
import { USER_GROUP_CONFIG_FACTORY } from '@home/models/group/group-entities-table-config.models';
import { UserGroupConfigFactory } from '@home/pages/user/user-group-config.factory';
import { UserTableHeaderComponent } from '@home/pages/user/user-table-header.component';

@NgModule({
  declarations: [
    UserComponent,
    UserTableHeaderComponent,
    UserTabsComponent,
    AddUserDialogComponent,
    ActivationLinkDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    UserRoutingModule
  ],
  providers: [
    {
      provide: USER_GROUP_CONFIG_FACTORY,
      useClass: UserGroupConfigFactory
    }
  ]
})
export class UserModule { }
