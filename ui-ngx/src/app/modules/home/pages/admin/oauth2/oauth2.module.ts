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
import { ClientComponent } from '@home/pages/admin/oauth2/clients/client.component';
import { Oauth2RoutingModule } from '@home/pages/admin/oauth2/oauth2-routing.module';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@home/components/home-components.module';
import { CommonModule } from '@angular/common';
import { ClientTableHeaderComponent } from '@home/pages/admin/oauth2/clients/client-table-header.component';
import { DomainComponent } from '@home/pages/admin/oauth2/domains/domain.component';
import { ClientDialogComponent } from '@home/pages/admin/oauth2/clients/client-dialog.component';
import { DomainTableHeaderComponent } from '@home/pages/admin/oauth2/domains/domain-table-header.component';

@NgModule({
  declarations: [
    ClientComponent,
    ClientDialogComponent,
    ClientTableHeaderComponent,
    DomainComponent,
    DomainTableHeaderComponent
  ],
  imports: [
    Oauth2RoutingModule,
    CommonModule,
    SharedModule,
    HomeComponentsModule
  ]
})
export class OAuth2Module {
}
