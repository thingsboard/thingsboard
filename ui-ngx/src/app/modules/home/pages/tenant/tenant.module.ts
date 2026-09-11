// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { TenantComponent } from '@modules/home/pages/tenant/tenant.component';
import { TenantRoutingModule } from '@modules/home/pages/tenant/tenant-routing.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { TenantTabsComponent } from '@home/pages/tenant/tenant-tabs.component';

@NgModule({
  declarations: [
    TenantComponent,
    TenantTabsComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    TenantRoutingModule
  ]
})
export class TenantModule { }
