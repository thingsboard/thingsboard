// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
