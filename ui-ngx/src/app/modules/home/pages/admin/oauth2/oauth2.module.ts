// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
import { DomainDialogComponent } from '@home/pages/admin/oauth2/domains/domain-dialog.component';

@NgModule({
  declarations: [
    ClientComponent,
    ClientDialogComponent,
    ClientTableHeaderComponent,
    DomainComponent,
    DomainTableHeaderComponent,
    DomainDialogComponent,
  ],
  imports: [
    Oauth2RoutingModule,
    CommonModule,
    SharedModule,
    HomeComponentsModule
  ],
  exports: [
    DomainDialogComponent,
  ]
})
export class OAuth2Module {
}
