// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Injectable } from '@angular/core';

import { ActivatedRouteSnapshot, Router } from '@angular/router';

import {
  CellActionDescriptor,
  DateEntityTableColumn,
  EntityChipsEntityTableColumn,
  EntityColumn,
  EntityTableColumn,
  EntityTableConfig,
  GroupActionDescriptor,
  HeaderActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { Customer, CustomerInfo, publicCustomer } from '@app/shared/models/customer.model';
import { CustomerService } from '@app/core/http/customer.service';
import { getCurrentAuthState, getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UtilsService } from '@core/services/utils.service';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { AllEntitiesTableConfigService } from '@home/components/entity/all-entities-table-config.service';
import { Observable, of } from 'rxjs';
import { resolveGroupParams } from '@shared/models/entity-group.models';
import { GroupEntityTabsComponent } from '@home/components/group/group-entity-tabs.component';
import { map, mergeMap } from 'rxjs/operators';
import { CustomerComponent } from '@home/pages/customer/customer.component';
import { CustomerTableHeaderComponent } from '@home/pages/customer/customer-table-header.component';
import { AuthUser } from '@shared/models/user.model';
import { Authority } from '@shared/models/authority.enum';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { AuthState } from '@core/auth/auth.models';

@Injectable()
export class CustomersTableConfigResolver  {

  constructor(private allEntitiesTableConfigService: AllEntitiesTableConfigService<CustomerInfo>,
              private userPermissionsService: UserPermissionsService,
              private customerService: CustomerService,
              private homeDialogs: HomeDialogsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private store: Store<AppState>,
              private utils: UtilsService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<CustomerInfo>> {
    const groupParams = resolveGroupParams(route);
    const config = new EntityTableConfig<CustomerInfo>(groupParams);
    this.configDefaults(config);
    const authUser = getCurrentAuthUser(this.store);
    const authState = getCurrentAuthState(this.store);
    config.componentsData = {
      includeCustomers: true,
      includeCustomersChanged: (includeCustomers: boolean) => {
        config.componentsData.includeCustomers = includeCustomers;
        config.columns = this.configureColumns(authUser, config);
        config.getTable().columnsUpdated();
        config.getTable().resetSortAndFilter(true);
      }
    };
    return (config.customerId ?
      this.customerService.getCustomer(config.customerId) : of(null as Customer)).pipe(
      map((parentCustomer) => {
        if (parentCustomer) {
          config.tableTitle = parentCustomer.title + ': ' + this.translate.instant('customer.customers');
        } else {
          config.tableTitle = this.translate.instant('customer.customers');
        }
        config.columns = this.configureColumns(authUser, config);
        this.configureEntityFunctions(config);
        config.cellActionDescriptors = this.configureCellActions(config, authState);
        config.groupActionDescriptors = this.configureGroupActions(config);
        config.addActionDescriptors = this.configureAddActions(config);
        return this.allEntitiesTableConfigService.prepareConfiguration(config);
      })
    );
  }

  configDefaults(config: EntityTableConfig<CustomerInfo>) {
    config.entityType = EntityType.CUSTOMER;
    config.entityComponent = CustomerComponent;
    config.entityTabsComponent = GroupEntityTabsComponent<CustomerInfo>;
    config.entityTranslations = entityTypeTranslations.get(EntityType.CUSTOMER);
    config.entityResources = entityTypeResources.get(EntityType.CUSTOMER);
    config.addDialogStyle = {height: '1060px'};

    config.entityTitle = (customer) => customer ?
      this.utils.customTranslation(customer.title, customer.title) : '';

    config.rowPointer = true;

    config.deleteEntityTitle = customer => this.translate.instant('customer.delete-customer-title', { customerTitle: customer.title });
    config.deleteEntityContent = () => this.translate.instant('customer.delete-customer-text');
    config.deleteEntitiesTitle = count => this.translate.instant('customer.delete-customers-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('customer.delete-customers-text');

    config.loadEntity = id => this.customerService.getCustomerInfo(id.id);
    config.saveEntity = customer => this.customerService.saveCustomer(customer).pipe(
      mergeMap((savedCustomer) => this.customerService.getCustomerInfo(savedCustomer.id.id))
    );
    config.onEntityAction = action => this.onCustomerAction(action, config);
    config.headerComponent = CustomerTableHeaderComponent;
  }

  configureColumns(authUser: AuthUser, config: EntityTableConfig<CustomerInfo>): Array<EntityColumn<CustomerInfo>> {
    const columns: Array<EntityColumn<CustomerInfo>> = [
      new DateEntityTableColumn<CustomerInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<CustomerInfo>('title', 'customer.title', '20%', config.entityTitle),
      new EntityTableColumn<CustomerInfo>('email', 'contact.email', '200px'),
      new EntityTableColumn<CustomerInfo>('country', 'contact.country', '20%'),
      new EntityTableColumn<CustomerInfo>('city', 'contact.city', '20%')
    ];
    if (config.componentsData.includeCustomers) {
      const title = (authUser.authority === Authority.CUSTOMER_USER || config.customerId)
        ? 'entity.parent-sub-customer-name' : 'entity.parent-customer-name';
      columns.push(new EntityTableColumn<CustomerInfo>('ownerName', title, '20%'));
    }
    columns.push(
      new EntityChipsEntityTableColumn<CustomerInfo>( 'groups', 'entity.groups', '20%')
    );
    return columns;
  }

  configureEntityFunctions(config: EntityTableConfig<CustomerInfo>): void {
    if (config.customerId) {
      config.entitiesFetchFunction = pageLink =>
        this.customerService.getCustomerCustomerInfos(config.componentsData.includeCustomers,
          config.customerId, pageLink);
    } else {
      config.entitiesFetchFunction = pageLink =>
        this.customerService.getAllCustomerInfos(config.componentsData.includeCustomers, pageLink);
    }
    config.deleteEntity = id => this.customerService.deleteCustomer(id.id);
    config.deleteEnabled = (customer) => !customer?.additionalInfo?.isPublic && customer?.name !== publicCustomer;
    config.entitySelectionEnabled = (customer) => !customer?.additionalInfo?.isPublic && customer?.name !== publicCustomer;
  }

  configureCellActions(config: EntityTableConfig<CustomerInfo>, authState: AuthState):
    Array<CellActionDescriptor<CustomerInfo>> {
    const actions: Array<CellActionDescriptor<CustomerInfo>> = [];
    if (this.userPermissionsService.hasGenericPermission(Resource.USER, Operation.READ)) {
      actions.push(
        {
          name: this.translate.instant('customer.manage-customer-users'),
          icon: 'account_circle',
          isEnabled: (customer) => !customer?.additionalInfo?.isPublic && customer?.name !== publicCustomer,
          onAction: ($event, entity) => this.manageCustomerUsers($event, entity, config)
        }
      );
    }
    if (this.userPermissionsService.hasGenericPermission(Resource.CUSTOMER, Operation.READ)) {
      actions.push(
        {
          name: this.translate.instant('customer.manage-customers'),
          icon: 'supervisor_account',
          isEnabled: () => true,
          onAction: ($event, entity) => this.manageCustomers($event, entity, config)
        }
      );
    }
    if (this.userPermissionsService.hasGenericPermission(Resource.ASSET, Operation.READ)) {
      actions.push(
        {
          name: this.translate.instant('customer.manage-customer-assets'),
          icon: 'domain',
          isEnabled: (customer) => true,
          onAction: ($event, entity) => this.manageCustomerAssets($event, entity, config)
        }
      );
    }
    if (this.userPermissionsService.hasGenericPermission(Resource.DEVICE, Operation.READ)) {
      actions.push(
        {
          name: this.translate.instant('customer.manage-customer-devices'),
          icon: 'devices_other',
          isEnabled: (customer) => true,
          onAction: ($event, entity) => this.manageCustomerDevices($event, entity, config)
        }
      );
    }
    if (this.userPermissionsService.hasGenericPermission(Resource.ENTITY_VIEW, Operation.READ)) {
      actions.push(
        {
          name: this.translate.instant('customer.manage-customer-entity-views'),
          icon: 'view_quilt',
          isEnabled: (customer) => true,
          onAction: ($event, entity) => this.manageCustomerEntityViews($event, entity, config)
        }
      );
    }
    if (this.userPermissionsService.hasGenericPermission(Resource.EDGE, Operation.READ) && authState.edgesSupportEnabled) {
      actions.push(
        {
          name: this.translate.instant('customer.manage-customer-edges'),
          icon: 'router',
          isEnabled: (customer) => true,
          onAction: ($event, entity) => this.manageCustomerEdges($event, entity, config)
        }
      );
    }
    if (this.userPermissionsService.hasGenericPermission(Resource.DASHBOARD, Operation.READ)) {
      actions.push(
        {
          name: this.translate.instant('customer.manage-customer-dashboards'),
          icon: 'dashboard',
          isEnabled: (customer) => true,
          onAction: ($event, entity) => this.manageCustomerDashboards($event, entity, config)
        });
    }
    return actions;
  }

  configureGroupActions(config: EntityTableConfig<CustomerInfo>): Array<GroupActionDescriptor<CustomerInfo>> {
    const actions: Array<GroupActionDescriptor<CustomerInfo>> = [];
    return actions;
  }

  configureAddActions(config: EntityTableConfig<CustomerInfo>): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    return actions;
  }

  private openCustomer($event: Event, customer: CustomerInfo, config: EntityTableConfig<CustomerInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([customer.id.id], {relativeTo: config.getActivatedRoute()});
    this.router.navigateByUrl(url);
  }

  manageCustomerUsers($event: Event, customer: CustomerInfo, config: EntityTableConfig<CustomerInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customers/all/${customer.id.id}/users`);
  }

  manageCustomers($event: Event, customer: CustomerInfo, config: EntityTableConfig<CustomerInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.navigateToChildCustomerPage(config, customer, '/customers/all', true);
  }

  manageCustomerAssets($event: Event, customer: CustomerInfo, config: EntityTableConfig<CustomerInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.navigateToChildCustomerPage(config, customer, '/entities/assets');
  }

  manageCustomerDevices($event: Event, customer: CustomerInfo, config: EntityTableConfig<CustomerInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.navigateToChildCustomerPage(config, customer, '/entities/devices');
  }

  manageCustomerEntityViews($event: Event, customer: CustomerInfo, config: EntityTableConfig<CustomerInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.navigateToChildCustomerPage(config, customer, '/entities/entityViews');
  }

  manageCustomerDashboards($event: Event, customer: CustomerInfo, config: EntityTableConfig<CustomerInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.navigateToChildCustomerPage(config, customer, '/dashboards');

  }

  manageCustomerEdges($event: Event, customer: CustomerInfo, config: EntityTableConfig<CustomerInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customers/all/${customer.id.id}/edgeManagement/edges`);
  }

  manageCustomerAgents($event: Event, customer: CustomerInfo, config: EntityTableConfig<CustomerInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customers/all/${customer.id.id}/edgeManagement/agents/all`);
  }

  private navigateToChildCustomerPage(config: EntityTableConfig<CustomerInfo>,
                                      customer: CustomerInfo, page: string, forceReload = false) {
    this.router.navigateByUrl(`customers/all/${customer.id.id}${page}`, {onSameUrlNavigation: forceReload ? 'reload' : undefined});
  }

  manageOwnerAndGroups($event: Event, customer: CustomerInfo, config: EntityTableConfig<CustomerInfo>) {
    this.homeDialogs.manageOwnerAndGroups($event, customer).subscribe(
      (res) => {
        if (res) {
          config.updateData();
        }
      }
    );
  }

  onCustomerAction(action: EntityAction<CustomerInfo>, config: EntityTableConfig<CustomerInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.openCustomer(action.event, action.entity, config);
        return true;
      case 'manageUsers':
        this.manageCustomerUsers(action.event, action.entity, config);
        return true;
      case 'manageCustomers':
        this.manageCustomers(action.event, action.entity, config);
        return true;
      case 'manageAssets':
        this.manageCustomerAssets(action.event, action.entity, config);
        return true;
      case 'manageDevices':
        this.manageCustomerDevices(action.event, action.entity, config);
        return true;
      case 'manageEntityViews':
        this.manageCustomerEntityViews(action.event, action.entity, config);
        return true;
      case 'manageDashboards':
        this.manageCustomerDashboards(action.event, action.entity, config);
        return true;
      case 'manageEdges':
        this.manageCustomerEdges(action.event, action.entity, config);
        return true;
      case 'manageAgents':
        this.manageCustomerAgents(action.event, action.entity, config);
        return true;
      case 'manageOwnerAndGroups':
        this.manageOwnerAndGroups(action.event, action.entity, config);
        return true;
    }
    return false;
  }

}
