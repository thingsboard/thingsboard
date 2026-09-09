// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { UserComponent } from '@modules/home/pages/user/user.component';
import { UserRoutingModule } from '@modules/home/pages/user/user-routing.module';
import { AddUserDialogComponent } from '@modules/home/pages/user/add-user-dialog.component';
import { ActivationLinkDialogComponent } from '@modules/home/pages/user/activation-link-dialog.component';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { UserTabsComponent } from '@home/pages/user/user-tabs.component';

@NgModule({
  declarations: [
    UserComponent,
    UserTabsComponent,
    AddUserDialogComponent,
    ActivationLinkDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    UserRoutingModule
  ]
})
export class UserModule { }
