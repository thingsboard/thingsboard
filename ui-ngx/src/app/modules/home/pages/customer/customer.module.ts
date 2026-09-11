// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { CustomerComponent } from '@modules/home/pages/customer/customer.component';
import { CustomerRoutingModule } from './customer-routing.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { CustomerTabsComponent } from '@home/pages/customer/customer-tabs.component';

@NgModule({
  declarations: [
    CustomerComponent,
    CustomerTabsComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    CustomerRoutingModule
  ]
})
export class CustomerModule { }
