///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { ActivatedRouteSnapshot, Router } from '@angular/router';
import {
  CellActionDescriptor,
  checkBoxCell,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig,
  GroupActionDescriptor,
  HeaderActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { forkJoin, Observable, of } from 'rxjs';
import { select, Store } from '@ngrx/store';
import { selectAuthUser } from '@core/auth/auth.selectors';
import { map, mergeMap, take, tap } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { Authority } from '@app/shared/models/authority.enum';
import { CustomerService } from '@core/http/customer.service';
import { Customer } from '@app/shared/models/customer.model';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import {
  AddEntitiesToCustomerDialogComponent,
  AddEntitiesToCustomerDialogData
} from '../../dialogs/add-entities-to-customer-dialog.component';
import {
  Dashboard,
  DashboardInfo,
  DashboardSetup,
  getDashboardAssignedCustomersText,
  isCurrentPublicDashboardCustomer,
  isPublicDashboard
} from '@app/shared/models/dashboard.models';
import { DashboardService } from '@app/core/http/dashboard.service';
import { DashboardFormComponent } from '@modules/home/pages/dashboard/dashboard-form.component';
import {
  ManageDashboardCustomersActionType,
  ManageDashboardCustomersDialogComponent,
  ManageDashboardCustomersDialogData
} from './manage-dashboard-customers-dialog.component';
import {
  MakeDashboardPublicDialogComponent,
  MakeDashboardPublicDialogData
} from '@modules/home/pages/dashboard/make-dashboard-public-dialog.component';
import { DashboardTabsComponent } from '@home/pages/dashboard/dashboard-tabs.component';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { EdgeService } from '@core/http/edge.service';
import {
  AddEntitiesToEdgeDialogComponent,
  AddEntitiesToEdgeDialogData
} from '@home/dialogs/add-entities-to-edge-dialog.component';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { Widget } from '@shared/models/widget.models';
import { EntityAliases } from '@shared/models/alias.models';
import {
  EntityAliasesDialogComponent,
  EntityAliasesDialogData
} from '@home/components/alias/entity-aliases-dialog.component';

@Injectable()
export class DashboardsTableConfigResolver  {

  private readonly config: EntityTableConfig<DashboardInfo | Dashboard> = new EntityTableConfig<DashboardInfo | Dashboard>();

  constructor(private store: Store<AppState>,
              private dashboardService: DashboardService,
              private customerService: CustomerService,
              private edgeService: EdgeService,
              private dialogService: DialogService,
              private homeDialogs: HomeDialogsService,
              private importExport: ImportExportService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private dialog: MatDialog) {

    this.config.entityType = EntityType.DASHBOARD;
    this.config.entityComponent = DashboardFormComponent;
    this.config.entityTabsComponent = DashboardTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.DASHBOARD);
    this.config.entityResources = entityTypeResources.get(EntityType.DASHBOARD);

    this.config.rowPointer = true;

    this.config.deleteEntityTitle = dashboard =>
      this.translate.instant('dashboard.delete-dashboard-title', {dashboardTitle: dashboard.title});
    this.config.deleteEntityContent = () => this.translate.instant('dashboard.delete-dashboard-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('dashboard.delete-dashboards-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('dashboard.delete-dashboards-text');

    this.config.loadEntity = id => this.dashboardService.getDashboard(id.id);
    this.config.saveEntity = dashboard => this.saveAndAssignDashboard(dashboard as DashboardSetup);
    this.config.onEntityAction = action => this.onDashboardAction(action);
    this.config.detailsReadonly = () => (this.config.componentsData.dashboardScope === 'customer_user' ||
      this.config.componentsData.dashboardScope === 'edge_customer_user');

    this.config.handleRowClick = ($event, dashboard) => {
      if (this.config.isDetailsOpen()) {
        this.config.toggleEntityDetails($event, dashboard);
      } else {
        this.openDashboard($event, dashboard);
      }
      return true;
    };

    this.config.entityAdded = dashboard => {
      this.openDashboard(null, dashboard);
    };
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<DashboardInfo | Dashboard>> {
    const routeParams = route.params;
    this.config.componentsData = {
      dashboardScope: route.data.dashboardsType,
      customerId: routeParams.customerId,
      edgeId: routeParams.edgeId
    };
    return this.store.pipe(select(selectAuthUser), take(1)).pipe(
      tap((authUser) => {
        if (authUser.authority === Authority.CUSTOMER_USER) {
          if (route.data.dashboardsType === 'edge') {
            this.config.componentsData.dashboardScope = 'edge_customer_user';
          } else {
            this.config.componentsData.dashboardScope = 'customer_user';
          }
          this.config.componentsData.customerId = authUser.customerId;
        }
      }),
      mergeMap(() =>
        this.config.componentsData.customerId ?
          this.customerService.getCustomer(this.config.componentsData.customerId) : of(null as Customer)
      ),
      map((parentCustomer) => {
        if (parentCustomer) {
          if (parentCustomer.additionalInfo && parentCustomer.additionalInfo.isPublic) {
            this.config.tableTitle = this.translate.instant('customer.public-dashboards');
          } else {
            this.config.tableTitle = parentCustomer.title + ': ' + this.translate.instant('dashboard.dashboards');
          }
        } else if (this.config.componentsData.dashboardScope === 'edge') {
          this.edgeService.getEdge(this.config.componentsData.edgeId).subscribe(
            edge => this.config.tableTitle = edge.name + ': ' + this.translate.instant('dashboard.dashboards')
          );
        } else {
          this.config.tableTitle = this.translate.instant('dashboard.dashboards');
        }
        this.config.columns = this.configureColumns(this.config.componentsData.dashboardScope);
        this.configureEntityFunctions(this.config.componentsData.dashboardScope);
        this.config.cellActionDescriptors = this.configureCellActions(this.config.componentsData.dashboardScope);
        this.config.groupActionDescriptors = this.configureGroupActions(this.config.componentsData.dashboardScope);
        this.config.addActionDescriptors = this.configureAddActions(this.config.componentsData.dashboardScope);
        this.config.addEnabled = !(this.config.componentsData.dashboardScope === 'customer_user' ||
          this.config.componentsData.dashboardScope === 'edge_customer_user');
        this.config.entitiesDeleteEnabled = this.config.componentsData.dashboardScope === 'tenant';
        this.config.deleteEnabled = () => this.config.componentsData.dashboardScope === 'tenant';
        return this.config;
      })
    );
  }

  configureColumns(dashboardScope: string): Array<EntityTableColumn<DashboardInfo>> {
    const columns: Array<EntityTableColumn<DashboardInfo>> = [
      new DateEntityTableColumn<DashboardInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<DashboardInfo>('title', 'dashboard.title', '50%')
    ];
    if (dashboardScope === 'tenant') {
      columns.push(
        new EntityTableColumn<DashboardInfo>('customersTitle', 'dashboard.assignedToCustomers',
          '50%', entity => getDashboardAssignedCustomersText(entity), () => ({}), false),
        new EntityTableColumn<DashboardInfo>('dashboardIsPublic', 'dashboard.public', '60px',
          entity => checkBoxCell(isPublicDashboard(entity)), () => ({}), false),
      );
    }
    return columns;
  }

  configureEntityFunctions(dashboardScope: string): void {
    if (dashboardScope === 'tenant') {
      this.config.entitiesFetchFunction = pageLink =>
        this.dashboardService.getTenantDashboards(pageLink);
      this.config.deleteEntity = id => this.dashboardService.deleteDashboard(id.id);
    } else if (dashboardScope === 'edge' || dashboardScope === 'edge_customer_user') {
      this.config.entitiesFetchFunction = pageLink =>
        this.dashboardService.getEdgeDashboards(this.config.componentsData.edgeId, pageLink, this.config.componentsData.dashboardsType);
    } else {
      this.config.entitiesFetchFunction = pageLink =>
        this.dashboardService.getCustomerDashboards(this.config.componentsData.customerId, pageLink);
      this.config.deleteEntity = id =>
        this.dashboardService.unassignDashboardFromCustomer(this.config.componentsData.customerId, id.id);
    }
  }

  configureCellActions(dashboardScope: string): Array<CellActionDescriptor<DashboardInfo>> {
    const actions: Array<CellActionDescriptor<DashboardInfo>> = [];
    if (dashboardScope === 'tenant') {
      actions.push(
        {
          name: this.translate.instant('dashboard.export'),
          icon: 'file_download',
          isEnabled: () => true,
          onAction: ($event, entity) => this.exportDashboard($event, entity)
        },
        {
          name: this.translate.instant('dashboard.make-public'),
          icon: 'share',
          isEnabled: (entity) => !isPublicDashboard(entity),
          onAction: ($event, entity) => this.makePublic($event, entity)
        },
        {
          name: this.translate.instant('dashboard.make-private'),
          icon: 'reply',
          isEnabled: (entity) => isPublicDashboard(entity),
          onAction: ($event, entity) => this.makePrivate($event, entity)
        },
        {
          name: this.translate.instant('dashboard.manage-assigned-customers'),
          icon: 'assignment_ind',
          isEnabled: () => true,
          onAction: ($event, entity) => this.manageAssignedCustomers($event, entity)
        }
      );
    }
    if (dashboardScope === 'customer') {
      actions.push(
        {
          name: this.translate.instant('dashboard.export'),
          icon: 'file_download',
          isEnabled: () => true,
          onAction: ($event, entity) => this.exportDashboard($event, entity)
        },
        {
          name: this.translate.instant('dashboard.make-private'),
          icon: 'reply',
          isEnabled: (entity) => isCurrentPublicDashboardCustomer(entity, this.config.componentsData.customerId),
          onAction: ($event, entity) => this.makePrivate($event, entity)
        },
        {
          name: this.translate.instant('dashboard.unassign-from-customer'),
          icon: 'assignment_return',
          isEnabled: (entity) => !isCurrentPublicDashboardCustomer(entity, this.config.componentsData.customerId),
          onAction: ($event, entity) => this.unassignFromCustomer($event, entity, this.config.componentsData.customerId)
        }
      );
    }
    if (dashboardScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('dashboard.export'),
          icon: 'file_download',
          isEnabled: () => true,
          onAction: ($event, entity) => this.exportDashboard($event, entity)
        },
        {
          name: this.translate.instant('edge.unassign-from-edge'),
          icon: 'assignment_return',
          isEnabled: () => true,
          onAction: ($event, entity) => this.unassignFromEdge($event, entity)
        }
      );
    }
    actions.push(
      {
        name: this.translate.instant('dashboard.dashboard-details'),
        icon: 'edit',
        isEnabled: () => true,
        onAction: ($event, entity) => this.config.toggleEntityDetails($event, entity)
      }
    );
    return actions;
  }

  configureGroupActions(dashboardScope: string): Array<GroupActionDescriptor<DashboardInfo>> {
    const actions: Array<GroupActionDescriptor<DashboardInfo>> = [];
    if (dashboardScope === 'tenant') {
      actions.push(
        {
          name: this.translate.instant('dashboard.assign-dashboards'),
          icon: 'assignment_ind',
          isEnabled: true,
          onAction: ($event, entities) => this.assignDashboardsToCustomers($event, entities.map((entity) => entity.id.id))
        }
      );
      actions.push(
        {
          name: this.translate.instant('dashboard.unassign-dashboards'),
          icon: 'assignment_return',
          isEnabled: true,
          onAction: ($event, entities) => this.unassignDashboardsFromCustomers($event, entities.map((entity) => entity.id.id))
        }
      );
    }
    if (dashboardScope === 'customer') {
      actions.push(
        {
          name: this.translate.instant('dashboard.unassign-dashboards'),
          icon: 'assignment_return',
          isEnabled: true,
          onAction: ($event, entities) =>
            this.unassignDashboardsFromCustomer($event, entities.map((entity) => entity.id.id), this.config.componentsData.customerId)
        }
      );
    }
    if (dashboardScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('dashboard.unassign-dashboards'),
          icon: 'assignment_return',
          isEnabled: true,
          onAction: ($event, entities) => this.unassignDashboardsFromEdge($event, entities)
        }
      );
    }
    return actions;
  }

  configureAddActions(dashboardScope: string): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    if (dashboardScope === 'tenant') {
      actions.push(
        {
          name: this.translate.instant('dashboard.create-new-dashboard'),
          icon: 'insert_drive_file',
          isEnabled: () => true,
          onAction: ($event) => this.config.getTable().addEntity($event)
        },
        {
          name: this.translate.instant('dashboard.import'),
          icon: 'file_upload',
          isEnabled: () => true,
          onAction: ($event) => this.importDashboard($event)
        }
      );
    }
    if (dashboardScope === 'customer') {
      actions.push(
        {
          name: this.translate.instant('dashboard.assign-new-dashboard'),
          icon: 'add',
          isEnabled: () => true,
          onAction: ($event) => this.addDashboardsToCustomer($event)
        }
      );
    }
    if (dashboardScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('dashboard.assign-new-dashboard'),
          icon: 'add',
          isEnabled: () => true,
          onAction: ($event) => this.addDashboardsToEdge($event)
        }
      );
    }
    return actions;
  }

  openDashboard($event: Event, dashboard: DashboardInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.config.componentsData.dashboardScope === 'customer') {
      this.router.navigateByUrl(`customers/${this.config.componentsData.customerId}/dashboards/${dashboard.id.id}`);
    } else if (this.config.componentsData.dashboardScope === 'edge') {
      this.router.navigateByUrl(`edgeInstances/${this.config.componentsData.edgeId}/dashboards/${dashboard.id.id}`);
    } else {
      this.router.navigateByUrl(`dashboards/${dashboard.id.id}`);
    }
  }

  importDashboard(_$event: Event) {
    this.importExport.importDashboard(this.editMissingAliases.bind(this)).subscribe(
      (dashboard) => {
        if (dashboard) {
          this.config.updateData();
        }
      }
    );
  }

  private editMissingAliases(widgets: Array<Widget>, isSingleWidget: boolean,
                             customTitle: string, missingEntityAliases: EntityAliases): Observable<EntityAliases> {
    return this.dialog.open<EntityAliasesDialogComponent, EntityAliasesDialogData,
      EntityAliases>(EntityAliasesDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entityAliases: missingEntityAliases,
        widgets,
        customTitle,
        isSingleWidget,
        disableAdd: true
      }
    }).afterClosed().pipe(
      map((updatedEntityAliases) => {
          if (updatedEntityAliases) {
            return updatedEntityAliases;
          } else {
            throw new Error('Unable to resolve missing entity aliases!');
          }
        }
      ));
  }

  exportDashboard($event: Event, dashboard: DashboardInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.exportDashboard(dashboard.id.id);
  }

  addDashboardsToCustomer($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AddEntitiesToCustomerDialogComponent, AddEntitiesToCustomerDialogData,
      boolean>(AddEntitiesToCustomerDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        customerId: this.config.componentsData.customerId,
        entityType: EntityType.DASHBOARD
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.config.updateData();
        }
      });
  }

  makePublic($event: Event, dashboard: DashboardInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dashboardService.makeDashboardPublic(dashboard.id.id).subscribe(
      (publicDashboard) => {
        this.dialog.open<MakeDashboardPublicDialogComponent, MakeDashboardPublicDialogData>
        (MakeDashboardPublicDialogComponent, {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {
            dashboard: publicDashboard
          }
        }).afterClosed()
          .subscribe(() => {
            this.config.updateData();
          });
      }
    );
  }

  makePrivate($event: Event, dashboard: DashboardInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('dashboard.make-private-dashboard-title', {dashboardTitle: dashboard.title}),
      this.translate.instant('dashboard.make-private-dashboard-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.dashboardService.makeDashboardPrivate(dashboard.id.id).subscribe(
            () => {
              this.config.updateData();
            }
          );
        }
      }
    );
  }

  manageAssignedCustomers($event: Event, dashboard: DashboardInfo) {
    const assignedCustomersIds = dashboard.assignedCustomers ?
      dashboard.assignedCustomers.map(customerInfo => customerInfo.customerId.id) : [];
    this.showManageAssignedCustomersDialog($event, [dashboard.id.id], 'manage', assignedCustomersIds);
  }

  assignDashboardsToCustomers($event: Event, dashboardIds: Array<string>) {
    this.showManageAssignedCustomersDialog($event, dashboardIds, 'assign');
  }

  unassignDashboardsFromCustomers($event: Event, dashboardIds: Array<string>) {
    this.showManageAssignedCustomersDialog($event, dashboardIds, 'unassign');
  }

  showManageAssignedCustomersDialog($event: Event, dashboardIds: Array<string>,
                                    actionType: ManageDashboardCustomersActionType,
                                    assignedCustomersIds?: Array<string>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<ManageDashboardCustomersDialogComponent, ManageDashboardCustomersDialogData,
      boolean>(ManageDashboardCustomersDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        dashboardIds,
        actionType,
        assignedCustomersIds
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.config.updateData();
        }
      });
  }

  unassignFromCustomer($event: Event, dashboard: DashboardInfo, customerId: string) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('dashboard.unassign-dashboard-title', {dashboardTitle: dashboard.title}),
      this.translate.instant('dashboard.unassign-dashboard-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.dashboardService.unassignDashboardFromCustomer(customerId, dashboard.id.id).subscribe(
            () => {
              this.config.updateData(this.config.componentsData.dashboardScope !== 'tenant');
            }
          );
        }
      }
    );
  }

  unassignDashboardsFromCustomer($event: Event, dashboardIds: Array<string>, customerId: string) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('dashboard.unassign-dashboards-title', {count: dashboardIds.length}),
      this.translate.instant('dashboard.unassign-dashboards-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          const tasks: Observable<any>[] = [];
          dashboardIds.forEach(
            (dashboardId) => {
              tasks.push(this.dashboardService.unassignDashboardFromCustomer(customerId, dashboardId));
            }
          );
          forkJoin(tasks).subscribe(
            () => {
              this.config.updateData();
            }
          );
        }
      }
    );
  }

  onDashboardAction(action: EntityAction<DashboardInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.openDashboard(action.event, action.entity);
        return true;
      case 'export':
        this.exportDashboard(action.event, action.entity);
        return true;
      case 'makePublic':
        this.makePublic(action.event, action.entity);
        return true;
      case 'makePrivate':
        this.makePrivate(action.event, action.entity);
        return true;
      case 'manageAssignedCustomers':
        this.manageAssignedCustomers(action.event, action.entity);
        return true;
      case 'unassignFromCustomer':
        this.unassignFromCustomer(action.event, action.entity, this.config.componentsData.customerId);
        return true;
      case 'unassignFromEdge':
        this.unassignFromEdge(action.event, action.entity);
        return true;
    }
    return false;
  }

  addDashboardsToEdge($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AddEntitiesToEdgeDialogComponent, AddEntitiesToEdgeDialogData,
      boolean>(AddEntitiesToEdgeDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        edgeId: this.config.componentsData.edgeId,
        entityType: EntityType.DASHBOARD
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.config.updateData();
        }
      });
  }

  unassignFromEdge($event: Event, dashboard: DashboardInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('dashboard.unassign-dashboard-title', {dashboardTitle: dashboard.title}),
      this.translate.instant('dashboard.unassign-dashboard-from-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.dashboardService.unassignDashboardFromEdge(this.config.componentsData.edgeId, dashboard.id.id).subscribe(
            () => {
              this.config.updateData(this.config.componentsData.dashboardScope !== 'tenant');
            }
          );
        }
      }
    );
  }

  unassignDashboardsFromEdge($event: Event, dashboards: Array<DashboardInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('dashboard.unassign-dashboards-from-edge-title', {count: dashboards.length}),
      this.translate.instant('dashboard.unassign-dashboards-from-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          const tasks: Observable<any>[] = [];
          dashboards.forEach(
            (dashboard) => {
              tasks.push(this.dashboardService.unassignDashboardFromEdge(this.config.componentsData.edgeId, dashboard.id.id));
            }
          );
          forkJoin(tasks).subscribe(
            () => {
              this.config.updateData();
            }
          );
        }
      }
    );
  }

  saveAndAssignDashboard(dashboard: DashboardSetup): Observable<Dashboard> {
    const {assignedCustomerIds, ...dashboardToCreate} = dashboard;

    return this.dashboardService.saveDashboard(dashboardToCreate as Dashboard).pipe(
      mergeMap((createdDashboard) => {
        if (assignedCustomerIds?.length) {
          return this.dashboardService.addDashboardCustomers(createdDashboard.id.id, assignedCustomerIds);
        }
        return of(createdDashboard);
      })
    );
  }

}
