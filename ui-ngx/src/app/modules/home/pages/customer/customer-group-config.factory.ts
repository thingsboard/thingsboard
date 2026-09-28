// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Observable } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import {
  EntityGroupStateConfigFactory,
  EntityGroupStateInfo,
  GroupEntityTableConfig
} from '@home/models/group/group-entities-table-config.models';
import { Inject, Injectable } from '@angular/core';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { MatDialog } from '@angular/material/dialog';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityGroupParams, ShortEntityView } from '@shared/models/entity-group.models';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { GroupConfigTableConfigService } from '@home/components/group/group-config-table-config.service';
import { CustomerInfo, publicCustomer } from '@shared/models/customer.model';
import { CustomerService } from '@core/http/customer.service';
import { CustomerComponent } from '@home/pages/customer/customer.component';
import { Router, UrlTree } from '@angular/router';
import { Operation, Resource } from '@shared/models/security.models';
import { EntityType } from '@shared/models/entity-type.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { WINDOW } from '@core/services/window.service';
import { mergeMap } from 'rxjs/operators';

@Injectable()
export class CustomerGroupConfigFactory implements EntityGroupStateConfigFactory<CustomerInfo> {

  constructor(private groupConfigTableConfigService: GroupConfigTableConfigService<CustomerInfo>,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private utils: UtilsService,
              private dialog: MatDialog,
              private homeDialogs: HomeDialogsService,
              private customerService: CustomerService,
              private router: Router,
              private store: Store<AppState>,
              @Inject(WINDOW) private window: Window) {
  }

  createConfig(params: EntityGroupParams, entityGroup: EntityGroupStateInfo<CustomerInfo>):
    Observable<GroupEntityTableConfig<CustomerInfo>> {
    const authState = getCurrentAuthState(this.store);
    const config = new GroupEntityTableConfig<CustomerInfo>(entityGroup, params);

    config.entityComponent = CustomerComponent;
    config.addDialogStyle = {height: '1060px'};

    config.entityTitle = (customer) => customer ?
      this.utils.customTranslation(customer.title, customer.title) : '';

    config.deleteEntityTitle = customer => this.translate.instant('customer.delete-customer-title', { customerTitle: customer.title });
    config.deleteEntityContent = () => this.translate.instant('customer.delete-customer-text');
    config.deleteEntitiesTitle = count => this.translate.instant('customer.delete-customers-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('customer.delete-customers-text');

    config.loadEntity = id => this.customerService.getCustomerInfo(id.id);
    config.saveEntity = customer => this.customerService.saveCustomer(customer).pipe(
      mergeMap((savedCustomer) => this.customerService.getCustomerInfo(savedCustomer.id.id))
    );
    config.deleteEntity = id => this.customerService.deleteCustomer(id.id);
    config.deleteEnabled = (customer) => !customer?.additionalInfo?.isPublic && customer?.name !== publicCustomer;
    config.entitySelectionEnabled = (customer) => !customer?.additionalInfo?.isPublic && customer?.name !== publicCustomer;

    if (params.hierarchyView) {
      config.entityAdded = customer => {
        params.hierarchyCallbacks.customerAdded(params.nodeId, customer);
      };
      config.entityUpdated = customer => {
        params.hierarchyCallbacks.customerUpdated(customer);
      };
      config.entitiesDeleted = customerIds => {
        params.hierarchyCallbacks.customersDeleted(customerIds.map(id => id.id));
      };
    }

    config.onEntityAction = action => this.onCustomerAction(action, config, params);

    if (this.userPermissionsService.hasGenericPermission(Resource.USER, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('customer.manage-customer-users'),
          icon: 'account_circle',
          isEnabled: (customer) => !customer?.additionalInfo?.isPublic && customer?.name !== publicCustomer && config.manageUsersEnabled(customer),
          onAction: ($event, entity) => this.manageUsers($event, entity, config, params)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.CUSTOMER, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('customer.manage-customers'),
          icon: 'supervisor_account',
          isEnabled: (customer) => !customer?.additionalInfo?.isPublic && customer?.name !== publicCustomer && config.manageCustomersEnabled(customer),
          onAction: ($event, entity) => this.manageCustomers($event, entity, config, params)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.ASSET, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('customer.manage-customer-assets'),
          icon: 'domain',
          isEnabled: config.manageAssetsEnabled,
          onAction: ($event, entity) => this.manageAssets($event, entity, config, params)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.DEVICE, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('customer.manage-customer-devices'),
          icon: 'devices_other',
          isEnabled: config.manageDevicesEnabled,
          onAction: ($event, entity) => this.manageDevices($event, entity, config, params)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.ENTITY_VIEW, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('customer.manage-customer-entity-views'),
          icon: 'view_quilt',
          isEnabled: config.manageEntityViewsEnabled,
          onAction: ($event, entity) => this.manageEntityViews($event, entity, config, params)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.EDGE, Operation.READ) && authState.edgesSupportEnabled) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('customer.manage-customer-edges'),
          icon: 'router',
          isEnabled: config.manageEdgesEnabled,
          onAction: ($event, entity) => this.manageEdges($event, entity, config, params)
        }
      );
    }

    if (this.userPermissionsService.hasGenericPermission(Resource.DASHBOARD, Operation.READ)) {
      config.cellActionDescriptors.push(
        {
          name: this.translate.instant('customer.manage-customer-dashboards'),
          icon: 'dashboard',
          isEnabled: config.manageDashboardsEnabled,
          onAction: ($event, entity) => this.manageDashboards($event, entity, config, params)
        }
      );
    }

    return this.groupConfigTableConfigService.prepareConfiguration(params, config);
  }

  private openCustomer($event: Event, customer: CustomerInfo, config: GroupEntityTableConfig<CustomerInfo>, params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      let url: UrlTree;
      if (params.customerId !== null) {
        url = this.router.createUrlTree(['customers', 'groups', params.entityGroupId,
          params.customerId, 'customers', 'groups', params.childEntityGroupId, customer.id.id]);
      } else {
        url = this.router.createUrlTree(['customers', 'groups', params.entityGroupId, customer.id.id]);
      }
      this.window.open(window.location.origin + url, '_blank');
    } else {
      const url = this.router.createUrlTree([customer.id.id], {relativeTo: config.getActivatedRoute()});
      this.router.navigateByUrl(url);
    }
  }

  manageUsers($event: Event, customer: CustomerInfo | ShortEntityView, config: GroupEntityTableConfig<CustomerInfo>,
              params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      params.hierarchyCallbacks.customerGroupsSelected(params.nodeId, customer.id.id, EntityType.USER);
    } else {
      this.navigateToChildCustomerPage(config, customer, '/users');
    }
  }

  manageCustomers($event: Event, customer: CustomerInfo | ShortEntityView, config: GroupEntityTableConfig<CustomerInfo>,
                  params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      params.hierarchyCallbacks.customerGroupsSelected(params.nodeId, customer.id.id, EntityType.CUSTOMER);
    } else {
      this.navigateToChildCustomerPage(config, customer, '/customers/all');
    }
  }

  manageAssets($event: Event, customer: CustomerInfo | ShortEntityView, config: GroupEntityTableConfig<CustomerInfo>,
               params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      params.hierarchyCallbacks.customerGroupsSelected(params.nodeId, customer.id.id, EntityType.ASSET);
    } else {
      this.navigateToChildCustomerPage(config, customer, '/entities/assets');
    }
  }

  manageDevices($event: Event, customer: CustomerInfo | ShortEntityView, config: GroupEntityTableConfig<CustomerInfo>,
                params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      params.hierarchyCallbacks.customerGroupsSelected(params.nodeId, customer.id.id, EntityType.DEVICE);
    } else {
      this.navigateToChildCustomerPage(config, customer, '/entities/devices');
    }
  }

  manageEntityViews($event: Event, customer: CustomerInfo | ShortEntityView, config: GroupEntityTableConfig<CustomerInfo>,
                    params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      params.hierarchyCallbacks.customerGroupsSelected(params.nodeId, customer.id.id, EntityType.ENTITY_VIEW);
    } else {
      this.navigateToChildCustomerPage(config, customer, '/entities/entityViews');
    }
  }

  manageEdges($event: Event, customer: CustomerInfo | ShortEntityView, config: GroupEntityTableConfig<CustomerInfo>,
              params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      params.hierarchyCallbacks.customerGroupsSelected(params.nodeId, customer.id.id, EntityType.EDGE);
    } else {
      this.navigateToChildCustomerPage(config, customer, '/edgeManagement/edges');
    }
  }

  manageDashboards($event: Event, customer: CustomerInfo | ShortEntityView, config: GroupEntityTableConfig<CustomerInfo>,
                   params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      params.hierarchyCallbacks.customerGroupsSelected(params.nodeId, customer.id.id, EntityType.DASHBOARD);
    } else {
      this.navigateToChildCustomerPage(config, customer, '/dashboards');
    }
  }

  private navigateToChildCustomerPage(config: GroupEntityTableConfig<CustomerInfo>,
                                      customer: CustomerInfo | ShortEntityView, page: string) {
    const targetGroups = config.groupParams.shared ? 'shared' : 'groups';
    this.router.navigateByUrl(`customers/${targetGroups}/${config.entityGroup.id.id}/${customer.id.id}${page}`);
  }

  manageOwnerAndGroups($event: Event, customer: CustomerInfo, config: GroupEntityTableConfig<CustomerInfo>) {
    this.homeDialogs.manageOwnerAndGroups($event, customer).subscribe(
      (res) => {
        if (res) {
          config.updateData();
        }
      }
    );
  }

  onCustomerAction(action: EntityAction<CustomerInfo>, config: GroupEntityTableConfig<CustomerInfo>, params: EntityGroupParams): boolean {
    switch (action.action) {
      case 'open':
        this.openCustomer(action.event, action.entity, config, params);
        return true;
      case 'manageUsers':
        this.manageUsers(action.event, action.entity, config, params);
        return true;
      case 'manageCustomers':
        this.manageCustomers(action.event, action.entity, config, params);
        return true;
      case 'manageAssets':
        this.manageAssets(action.event, action.entity, config, params);
        return true;
      case 'manageDevices':
        this.manageDevices(action.event, action.entity, config, params);
        return true;
      case 'manageEntityViews':
        this.manageEntityViews(action.event, action.entity, config, params);
        return true;
      case 'manageEdges':
        this.manageEdges(action.event, action.entity, config, params);
        return true;
      case 'manageDashboards':
        this.manageDashboards(action.event, action.entity, config, params);
        return true;
      case 'manageOwnerAndGroups':
        this.manageOwnerAndGroups(action.event, action.entity, config);
        return true;
    }
    return false;
  }

}
