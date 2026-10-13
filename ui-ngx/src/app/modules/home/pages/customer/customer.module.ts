// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { CustomerComponent } from '@modules/home/pages/customer/customer.component';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { CUSTOMER_GROUP_CONFIG_FACTORY } from '@home/models/group/group-entities-table-config.models';
import { CustomerGroupConfigFactory } from '@home/pages/customer/customer-group-config.factory';
import { CustomerRoutingModule } from '@home/pages/customer/customer-routing.module';
import { CustomerTableHeaderComponent } from '@home/pages/customer/customer-table-header.component';
import { CustomersHierarchyComponent } from '@home/pages/customer/customers-hierarchy.component';

@NgModule({
  declarations: [
    CustomerComponent,
    CustomerTableHeaderComponent,
    CustomersHierarchyComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    CustomerRoutingModule
  ],
  providers: [
    {
      provide: CUSTOMER_GROUP_CONFIG_FACTORY,
      useClass: CustomerGroupConfigFactory
    }
  ]
})
export class CustomerModule { }
