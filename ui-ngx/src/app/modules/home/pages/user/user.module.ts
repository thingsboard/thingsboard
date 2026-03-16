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
