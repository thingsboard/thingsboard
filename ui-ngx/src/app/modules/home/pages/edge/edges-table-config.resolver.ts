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
import { Observable, of } from 'rxjs';
import { select, Store } from '@ngrx/store';
import { getCurrentAuthUser, selectUserSettingsProperty } from '@core/auth/auth.selectors';
import { map, mergeMap, take, tap } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { Authority } from '@app/shared/models/authority.enum';
import { CustomerService } from '@core/http/customer.service';
import { Customer } from '@app/shared/models/customer.model';
import { BroadcastService } from '@core/services/broadcast.service';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { edgeCreationAllowed, EdgeInfo } from '@shared/models/edge.models';
import { EdgeService } from '@core/http/edge.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import {
  EdgeInstructionsDialogComponent,
  EdgeInstructionsDialogData
} from '@home/pages/edge/edge-instructions-dialog.component';
import { AllEntitiesTableConfigService } from '@home/components/entity/all-entities-table-config.service';
import { resolveGroupParams } from '@shared/models/entity-group.models';
import { GroupEntityTabsComponent } from '@home/components/group/group-entity-tabs.component';
import { EdgeComponent } from '@home/pages/edge/edge.component';
import { EdgeTableHeaderComponent } from '@home/pages/edge/edge-table-header.component';
import { AuthUser } from '@shared/models/user.model';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { CustomerId } from '@shared/models/id/customer-id';
import { UtilsService } from '@core/services/utils.service';
import { AddGroupEntityDialogComponent } from '@home/components/group/add-group-entity-dialog.component';
import { AddGroupEntityDialogData } from '@home/models/group/group-entity-component.models';
import {
  AgentAutoProvisionDialogData,
  AgentAutoProvisionDialogComponent
} from '@home/components/agent/dialog/agent-auto-provision-dialog.component';
import { AgentApplicationType } from '@shared/models/agent.models';

@Injectable()
export class EdgesTableConfigResolver  {

  constructor(private allEntitiesTableConfigService: AllEntitiesTableConfigService<EdgeInfo>,
              private userPermissionsService: UserPermissionsService,
              private store: Store<AppState>,
              private broadcast: BroadcastService,
              private edgeService: EdgeService,
              private customerService: CustomerService,
              private dialogService: DialogService,
              private homeDialogs: HomeDialogsService,
              private translate: TranslateService,
              private utils: UtilsService,
              private datePipe: DatePipe,
              private router: Router,
              private dialog: MatDialog) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<EdgeInfo>> {
    const groupParams = resolveGroupParams(route);
    const config = new EntityTableConfig<EdgeInfo>(groupParams);
    const authUser = getCurrentAuthUser(this.store);
    this.configDefaults(authUser, config);
    config.componentsData = {
      includeCustomers: true,
      edgeType: '',
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
          config.tableTitle = parentCustomer.title + ': ' + this.translate.instant('edge.edge-instances');
        } else {
          config.tableTitle = this.translate.instant('edge.edge-instances');
        }
        config.columns = this.configureColumns(authUser, config);
        this.configureEntityFunctions(config);
        config.cellActionDescriptors = this.configureCellActions(authUser, config);
        config.groupActionDescriptors = this.configureGroupActions(config);
        config.addActionDescriptors = this.configureAddActions(config);
        config.addEntity = () => { this.addEdge(config); return of(null); };
        return this.allEntitiesTableConfigService.prepareConfiguration(config);
      })
    );
  }

  configDefaults(authUser: AuthUser, config: EntityTableConfig<EdgeInfo>) {
    config.entityType = EntityType.EDGE;
    config.entityComponent = EdgeComponent;
    config.entityTabsComponent = GroupEntityTabsComponent<EdgeInfo>;
    config.entityTranslations = entityTypeTranslations.get(EntityType.EDGE);
    config.entityResources = entityTypeResources.get(EntityType.EDGE);
    config.addDialogStyle = {height: '1000px'};

    if (!edgeCreationAllowed(authUser)) {
      config.addEnabled = false;
    }

    config.entityTitle = (edge) => edge ?
      this.utils.customTranslation(edge.name, edge.name) : '';

    config.rowPointer = true;

    config.deleteEntityTitle = edge => this.translate.instant('edge.delete-edge-title', {edgeName: edge.name});
    config.deleteEntityContent = () => this.translate.instant('edge.delete-edge-text');
    config.deleteEntitiesTitle = count => this.translate.instant('edge.delete-edges-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('edge.delete-edges-text');

    config.loadEntity = id => this.edgeService.getEdgeInfo(id.id);
    config.saveEntity = edge => this.edgeService.saveEdge(edge).pipe(
      tap(() => {
        this.broadcast.broadcast('edgeSaved');
      }),
      mergeMap((savedEdge) => this.edgeService.getEdgeInfo(savedEdge.id.id)
      ));
    config.onEntityAction = action => this.onEdgeAction(action, config);
    config.headerComponent = EdgeTableHeaderComponent;
  }

  configureColumns(authUser: AuthUser, config: EntityTableConfig<EdgeInfo>): Array<EntityColumn<EdgeInfo>> {
    const columns: Array<EntityColumn<EdgeInfo>> = [
      new DateEntityTableColumn<EdgeInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<EdgeInfo>('name', 'edge.name', '20%', config.entityTitle),
      new EntityTableColumn<EdgeInfo>('type', 'edge.edge-type', '20%'),
      new EntityTableColumn<EdgeInfo>('label', 'edge.label', '15%')
    ];
    if (config.componentsData.includeCustomers) {
      const title = (authUser.authority === Authority.CUSTOMER_USER || config.customerId)
        ? 'entity.sub-customer-name' : 'entity.customer-name';
      columns.push(new EntityTableColumn<EdgeInfo>('ownerName', title, '20%'));
    }
    columns.push(
      new EntityChipsEntityTableColumn<EdgeInfo>( 'groups', 'entity.groups', '25%')
    );
    return columns;
  }

  configureEntityFunctions(config: EntityTableConfig<EdgeInfo>): void {
    if (config.customerId) {
      config.entitiesFetchFunction = pageLink =>
        this.edgeService.getCustomerEdgeInfos(config.componentsData.includeCustomers,
          config.customerId, pageLink, config.componentsData.edgeType);
    } else {
      config.entitiesFetchFunction = pageLink =>
        this.edgeService.getAllEdgeInfos(config.componentsData.includeCustomers, pageLink,
          config.componentsData.edgeType);
    }
    config.deleteEntity = id => this.edgeService.deleteEdge(id.id);
  }

  configureCellActions(authUser: AuthUser, config: EntityTableConfig<EdgeInfo>): Array<CellActionDescriptor<EdgeInfo>> {
    const actions: Array<CellActionDescriptor<EdgeInfo>> = [];
    if (this.userPermissionsService.hasGenericPermission(Resource.USER_GROUP, Operation.READ)) {
      actions.push(
        {
          name: this.translate.instant('edge.manage-edge-user-groups'),
          icon: 'account_circle',
          isEnabled: (entity) => true,
          onAction: ($event, entity) => this.manageUsers($event, entity, config)
        }
      );
    }
    if (this.userPermissionsService.hasGenericPermission(Resource.ASSET_GROUP, Operation.READ)) {
      actions.push(
        {
          name: this.translate.instant('edge.manage-edge-asset-groups'),
          icon: 'domain',
          isEnabled: (entity) => true,
          onAction: ($event, entity) => this.manageAssets($event, entity, config)
        }
      );
    }
    if (this.userPermissionsService.hasGenericPermission(Resource.DEVICE_GROUP, Operation.READ)) {
      actions.push(
        {
          name: this.translate.instant('edge.manage-edge-device-groups'),
          icon: 'devices_other',
          isEnabled: (entity) => true,
          onAction: ($event, entity) => this.manageDevices($event, entity, config)
        }
      );
    }
    if (this.userPermissionsService.hasGenericPermission(Resource.ENTITY_VIEW_GROUP, Operation.READ)) {
      actions.push(
        {
          name: this.translate.instant('edge.manage-edge-entity-view-groups'),
          icon: 'view_quilt',
          isEnabled: (entity) => true,
          onAction: ($event, entity) => this.manageEntityViews($event, entity, config)
        }
      );
    }
    if (this.userPermissionsService.hasGenericPermission(Resource.DASHBOARD_GROUP, Operation.READ)) {
      actions.push(
        {
          name: this.translate.instant('edge.manage-edge-dashboard-groups'),
          icon: 'dashboard',
          isEnabled: (entity) => true,
          onAction: ($event, entity) => this.manageDashboards($event, entity, config)
        }
      );
    }
    if (this.userPermissionsService.hasGenericPermission(Resource.SCHEDULER_EVENT, Operation.READ)) {
      actions.push(
        {
          name: this.translate.instant('edge.manage-edge-scheduler-events'),
          icon: 'schedule',
          isEnabled: (entity) => true,
          onAction: ($event, entity) => this.manageSchedulerEvents($event, entity, config)
        }
      );
    }
    if (this.userPermissionsService.hasGenericPermission(Resource.RULE_CHAIN, Operation.READ) &&
      authUser.authority === Authority.TENANT_ADMIN) {
      actions.push(
        {
          name: this.translate.instant('edge.manage-edge-rule-chains'),
          icon: 'settings_ethernet',
          isEnabled: () => true,
          onAction: ($event, entity) => this.manageRuleChains($event, entity, config)
        }
      );
    }
    if (this.userPermissionsService.hasGenericPermission(Resource.INTEGRATION, Operation.READ) &&
      authUser.authority === Authority.TENANT_ADMIN) {
      actions.push(
        {
          name: this.translate.instant('edge.manage-edge-integrations'),
          icon: 'input',
          isEnabled: () => true,
          onAction: ($event, entity) => this.manageIntegrations($event, entity, config)
        }
      );
    }
    return actions;
  }

  configureGroupActions(config: EntityTableConfig<EdgeInfo>): Array<GroupActionDescriptor<EdgeInfo>> {
    const actions: Array<GroupActionDescriptor<EdgeInfo>> = [];
    return actions;
  }

  configureAddActions(config: EntityTableConfig<EdgeInfo>): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    actions.push(
      {
        name: this.translate.instant('edge.add-edge-text'),
        icon: 'insert_drive_file',
        isEnabled: () => true,
        onAction: ($event) => config.getTable().addEntity($event)
      },
      {
        name: this.translate.instant('edge.auto-provision'),
        icon: 'auto_fix_high',
        isEnabled: () => true,
        onAction: ($event) => this.autoProvisionEdge($event, config)
      },
      {
        name: this.translate.instant('edge.import'),
        icon: 'file_upload',
        isEnabled: () => true,
        onAction: ($event) => this.importEdges($event, config)
      }
    );
    return actions;
  }

  autoProvisionEdge($event: Event, config: EntityTableConfig<EdgeInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AgentAutoProvisionDialogComponent, AgentAutoProvisionDialogData, boolean>(
      AgentAutoProvisionDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { appType: AgentApplicationType.EDGE }
      }
    ).afterClosed().subscribe(result => {
      if (result) {
        config.updateData();
      }
    });
  }

  importEdges($event: Event, config: EntityTableConfig<EdgeInfo>) {
    const customerId = config.customerId ? new CustomerId(config.customerId) : null;
    this.homeDialogs.importEntities(customerId, EntityType.EDGE, null).subscribe((res) => {
      if (res) {
        this.broadcast.broadcast('edgeSaved');
        config.updateData();
      }
    });
  }

  private openEdge($event: Event, edge: EdgeInfo, config: EntityTableConfig<EdgeInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([edge.id.id], {relativeTo: config.getActivatedRoute()});
    this.router.navigateByUrl(url);
  }

  manageUsers($event: Event, edge: EdgeInfo, config: EntityTableConfig<EdgeInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.navigateToChildEdgePage(config, edge, '/userGroups');
  }

  manageAssets($event: Event, edge: EdgeInfo, config: EntityTableConfig<EdgeInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.navigateToChildEdgePage(config, edge, '/assetGroups');
  }

  manageDevices($event: Event, edge: EdgeInfo, config: EntityTableConfig<EdgeInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.navigateToChildEdgePage(config, edge, '/deviceGroups');
  }

  manageEntityViews($event: Event, edge: EdgeInfo, config: EntityTableConfig<EdgeInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.navigateToChildEdgePage(config, edge, '/entityViewGroups');
  }

  manageDashboards($event: Event, edge: EdgeInfo, config: EntityTableConfig<EdgeInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.navigateToChildEdgePage(config, edge, '/dashboardGroups');
  }

  manageSchedulerEvents($event: Event, edge: EdgeInfo, config: EntityTableConfig<EdgeInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.navigateToChildEdgePage(config, edge, '/scheduler');
  }

  manageRuleChains($event: Event, edge: EdgeInfo, config: EntityTableConfig<EdgeInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.navigateToChildEdgePage(config, edge, '/ruleChains');
  }

  manageIntegrations($event: Event, edge: EdgeInfo, config: EntityTableConfig<EdgeInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.navigateToChildEdgePage(config, edge, '/integrations');
  }

  private navigateToChildEdgePage(config: EntityTableConfig<EdgeInfo>, edge: EdgeInfo, page: string) {
    let url = `edgeManagement/edges/all/${edge.id.id}${page}`;
    if (config.customerId) {
      if (config.groupParams.childEntityGroupId) {
        const targetGroups = config.groupParams.shared ? 'shared' : 'groups';
        url = `customers/${targetGroups}/${config.groupParams.entityGroupId}/${config.groupParams.customerId}/${url}`;
      } else {
        url = `customers/all/${config.customerId}/${url}`;
      }
    }
    this.router.navigateByUrl(url);
  }

  syncEdge($event, edge: EdgeInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.edgeService.syncEdge(edge.id.id).subscribe(
      () => {
        this.store.dispatch(new ActionNotificationShow(
          {
            message: this.translate.instant('edge.sync-process-started-successfully'),
            type: 'success',
            duration: 750,
            verticalPosition: 'bottom',
            horizontalPosition: 'right'
          }));
      }
    );
  }

  addEdge(config) {
    this.dialog.open<AddGroupEntityDialogComponent, AddGroupEntityDialogData<EdgeInfo>,
      EdgeInfo>(AddGroupEntityDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entitiesTableConfig: config
      }
    }).afterClosed().subscribe(
      (entity) => {
        if (entity) {
          this.store.pipe(select(selectUserSettingsProperty('notDisplayInstructionsAfterAddEdge'))).pipe(
            take(1)
          ).subscribe((settings: boolean) => {
            if (!settings) {
              this.openInstructions(null, entity, true, config);
            } else {
              config.updateData();
              config.entityAdded(entity);
            }
          });
        }
      }
    );
  }

  openInstructions($event: Event, edge: EdgeInfo, afterAdd = false, config: EntityTableConfig<EdgeInfo>, upgradeAvailable = false) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<EdgeInstructionsDialogComponent, EdgeInstructionsDialogData>
    (EdgeInstructionsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        edge,
        afterAdd,
        upgradeAvailable
      }
    }).afterClosed().subscribe(() => {
        if (afterAdd) {
          config.updateData();
        }
      }
    );
  }

  manageOwnerAndGroups($event: Event, edge: EdgeInfo, config: EntityTableConfig<EdgeInfo>) {
    this.homeDialogs.manageOwnerAndGroups($event, edge).subscribe(
      (res) => {
        if (res) {
          config.updateData();
        }
      }
    );
  }

  onEdgeAction(action: EntityAction<EdgeInfo>, config: EntityTableConfig<EdgeInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.openEdge(action.event, action.entity, config);
        return true;
      case 'manageUsers':
        this.manageUsers(action.event, action.entity, config);
        return true;
      case 'manageAssets':
        this.manageAssets(action.event, action.entity, config);
        return true;
      case 'manageDevices':
        this.manageDevices(action.event, action.entity, config);
        return true;
      case 'manageEntityViews':
        this.manageEntityViews(action.event, action.entity, config);
        return true;
      case 'manageDashboards':
        this.manageDashboards(action.event, action.entity, config);
        return true;
      case 'manageSchedulerEvents':
        this.manageSchedulerEvents(action.event, action.entity, config);
        return true;
      case 'manageRuleChains':
        this.manageRuleChains(action.event, action.entity, config);
        return true;
      case 'manageIntegrations':
        this.manageIntegrations(action.event, action.entity, config);
        return true;
      case 'syncEdge':
        this.syncEdge(action.event, action.entity);
        return true;
      case 'manageOwnerAndGroups':
        this.manageOwnerAndGroups(action.event, action.entity, config);
        return true;
      case 'openInstallInstructions':
        this.openInstructions(action.event, action.entity, null, config);
        return true;
      case 'openUpgradeInstructions':
        this.openInstructions(action.event, action.entity,false, config, true);
        return true;
    }
  }

}
