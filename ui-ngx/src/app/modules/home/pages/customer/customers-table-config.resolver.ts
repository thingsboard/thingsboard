///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import { Injectable } from '@angular/core';

import { Resolve, Router } from '@angular/router';

import { Tenant } from '@shared/models/tenant.model';
import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@shared/components/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import {
  EntityType,
  entityTypeResources,
  entityTypeTranslations
} from '@shared/models/entity-type.models';
import { EntityAction } from '@shared/components/entity/entity-component.models';
import {Customer} from '@app/shared/models/customer.model';
import {CustomerService} from '@app/core/http/customer.service';
import {CustomerComponent} from '@modules/home/pages/customer/customer.component';

@Injectable()
export class CustomersTableConfigResolver implements Resolve<EntityTableConfig<Customer>> {

  private readonly config: EntityTableConfig<Customer> = new EntityTableConfig<Customer>();

  constructor(private customerService: CustomerService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router) {

    this.config.entityType = EntityType.CUSTOMER;
    this.config.entityComponent = CustomerComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.CUSTOMER);
    this.config.entityResources = entityTypeResources.get(EntityType.CUSTOMER);

    this.config.columns.push(
      new DateEntityTableColumn<Customer>('createdTime', 'customer.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Customer>('title', 'customer.title'),
      new EntityTableColumn<Customer>('email', 'contact.email'),
      new EntityTableColumn<Customer>('country', 'contact.country'),
      new EntityTableColumn<Customer>('city', 'contact.city')
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('customer.manage-customer-users'),
        icon: 'account_circle',
        isEnabled: (customer) => !customer.additionalInfo || !customer.additionalInfo.isPublic,
        onAction: ($event, entity) => this.manageCustomerUsers($event, entity)
      }
    );

    this.config.deleteEntityTitle = customer => this.translate.instant('customer.delete-customer-title', { customerTitle: customer.title });
    this.config.deleteEntityContent = () => this.translate.instant('customer.delete-customer-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('customer.delete-customers-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('customer.delete-customers-text');

    this.config.entitiesFetchFunction = pageLink => this.customerService.getCustomers(pageLink);
    this.config.loadEntity = id => this.customerService.getCustomer(id.id);
    this.config.saveEntity = customer => this.customerService.saveCustomer(customer);
    this.config.deleteEntity = id => this.customerService.deleteCustomer(id.id);
    this.config.onEntityAction = action => this.onCustomerAction(action);
  }

  resolve(): EntityTableConfig<Customer> {
    this.config.tableTitle = this.translate.instant('customer.customers');

    return this.config;
  }

  manageCustomerUsers($event: Event, customer: Customer) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customers/${customer.id.id}/users`);
  }

  onCustomerAction(action: EntityAction<Customer>): boolean {
    switch (action.action) {
      case 'manageUsers':
        this.manageCustomerUsers(action.event, action.entity);
        return true;
    }
    return false;
  }

}
