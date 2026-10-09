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
import { Store } from '@ngrx/store';
import { getCurrentAuthState, getCurrentAuthUser } from '@core/auth/auth.selectors';
import { map, mergeMap } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { Authority } from '@app/shared/models/authority.enum';
import { CustomerService } from '@core/http/customer.service';
import { Customer } from '@app/shared/models/customer.model';
import { Dashboard, DashboardInfo } from '@app/shared/models/dashboard.models';
import { DashboardService } from '@app/core/http/dashboard.service';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { UtilsService } from '@core/services/utils.service';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { DashboardFormComponent } from '@home/pages/dashboard/dashboard-form.component';
import { CustomerId } from '@shared/models/id/customer-id';
import { AuthUser } from '@shared/models/user.model';
import { DashboardTableHeaderComponent } from '@home/pages/dashboard/dashboard-table-header.component';
import { resolveGroupParams } from '@shared/models/entity-group.models';
import { AllEntitiesTableConfigService } from '@home/components/entity/all-entities-table-config.service';
import { GroupEntityTabsComponent } from '@home/components/group/group-entity-tabs.component';
import { Widget } from '@shared/models/widget.models';
import { EntityAliases } from '@shared/models/alias.models';
import {
  EntityAliasesDialogComponent,
  EntityAliasesDialogData
} from '@home/components/alias/entity-aliases-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import {
  DashboardInfoDialogData,
  ImportDashboardFileDialogComponent
} from "@home/pages/dashboard/import-dashboard-file-dialog.component";
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';

@Injectable()
export class DashboardsTableConfigResolver {

  constructor(private allEntitiesTableConfigService: AllEntitiesTableConfigService<DashboardInfo>,
              private store: Store<AppState>,
              private userPermissionsService: UserPermissionsService,
              private dashboardService: DashboardService,
              private customerService: CustomerService,
              private homeDialogs: HomeDialogsService,
              private importExport: ImportExportService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private utils: UtilsService,
              private dialog: MatDialog,
              private dashboardUtils: DashboardUtilsService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<DashboardInfo>> {
    const groupParams = resolveGroupParams(route);
    const config = new EntityTableConfig<DashboardInfo>(groupParams);
    this.configDefaults(config);
    const authUser = getCurrentAuthUser(this.store);
    config.componentsData = {
      includeCustomers: true,
      includeCustomersChanged: (includeCustomers: boolean) => {
        config.componentsData.includeCustomers = includeCustomers;
        config.columns = this.configureColumns(authUser, config);
        config.getTable().columnsUpdated();
        config.getTable().resetSortAndFilter(true);
      }
    };
    config.handleRowClick = ($event, dashboard) => {
      if (config.isDetailsOpen()) {
        config.toggleEntityDetails($event, dashboard);
      } else {
        this.openDashboard($event, dashboard, config);
      }
      return true;
    };
    return (config.customerId ?
      this.customerService.getCustomer(config.customerId) : of(null as Customer)).pipe(
      map((parentCustomer) => {
        if (parentCustomer) {
          config.tableTitle = parentCustomer.title + ': ' + this.translate.instant('dashboard.dashboards');
        } else {
          config.tableTitle = this.translate.instant('dashboard.dashboards');
        }
        config.columns = this.configureColumns(authUser, config);
        this.configureEntityFunctions(config);
        config.cellActionDescriptors = this.configureCellActions(config);
        config.groupActionDescriptors = this.configureGroupActions(config);
        config.addActionDescriptors = this.configureAddActions(config);
        return this.allEntitiesTableConfigService.prepareConfiguration(config);
      })
    );
  }

  configDefaults(config: EntityTableConfig<DashboardInfo>) {
    config.entityType = EntityType.DASHBOARD;
    config.entityComponent = DashboardFormComponent;
    config.entityTabsComponent = GroupEntityTabsComponent<Dashboard>;
    config.entityTranslations = entityTypeTranslations.get(EntityType.DASHBOARD);
    config.entityResources = entityTypeResources.get(EntityType.DASHBOARD);
    config.addDialogStyle = {height: '800px'};
    config.addDialogOwnerAndGroupWizard = false;

    config.entityTitle = (dashboard) => dashboard ?
      this.utils.customTranslation(dashboard.title, dashboard.title) : '';

    config.rowPointer = true;

    config.deleteEntityTitle = dashboard =>
      this.translate.instant('dashboard.delete-dashboard-title', {dashboardTitle: dashboard.title});
    config.deleteEntityContent = () => this.translate.instant('dashboard.delete-dashboard-text');
    config.deleteEntitiesTitle = count => this.translate.instant('dashboard.delete-dashboards-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('dashboard.delete-dashboards-text');

    config.loadEntity = id => this.dashboardService.getDashboardInfo(id.id);
    config.saveEntity = dashboard => this.dashboardService.saveDashboard(dashboard).pipe(
      mergeMap((savedDashboard) => this.dashboardService.getDashboardInfo(savedDashboard.id.id))
    );
    config.onEntityAction = action => this.onDashboardAction(action, config);
    config.headerComponent = DashboardTableHeaderComponent;
    config.entityAdded = dashboard => {
      this.openDashboard(null, dashboard, config);
    };

    if (getCurrentAuthState(this.store).aiEnabled &&
      this.userPermissionsService.hasGenericPermission(Resource.AI, Operation.ALL) &&
      this.userPermissionsService.hasGenericPermission(Resource.DASHBOARD, Operation.CREATE)) {
      config.headerButtonDescriptors.push({
        name: this.translate.instant('ai-assistant.configure-with-ai'),
        icon: 'mdi:creation',
        isEnabled: () => true,
        onAction: ($event) => this.generateWithAi($event, config)
      });
    }
  }

  configureColumns(authUser: AuthUser, config: EntityTableConfig<DashboardInfo>): Array<EntityColumn<DashboardInfo>> {
    const columns: Array<EntityColumn<DashboardInfo>> = [
      new DateEntityTableColumn<DashboardInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<DashboardInfo>('title', 'dashboard.title',
        config.componentsData.includeCustomers ? '30%' : '60%', config.entityTitle)
    ];
    if (config.componentsData.includeCustomers) {
      const title = (authUser.authority === Authority.CUSTOMER_USER || config.customerId)
        ? 'entity.sub-customer-name' : 'entity.customer-name';
      columns.push(new EntityTableColumn<DashboardInfo>('ownerName', title, '30%'));
    }
    columns.push(
      new EntityChipsEntityTableColumn<DashboardInfo>('groups', 'entity.groups', '40%')
    );
    return columns;
  }

  configureEntityFunctions(config: EntityTableConfig<DashboardInfo>): void {
    if (config.customerId) {
      config.entitiesFetchFunction = pageLink =>
        this.dashboardService.getCustomerDashboards(config.componentsData.includeCustomers,
          config.customerId, pageLink);
    } else {
      config.entitiesFetchFunction = pageLink =>
        this.dashboardService.getAllDashboards(config.componentsData.includeCustomers, pageLink);
    }
    config.deleteEntity = id => this.dashboardService.deleteDashboard(id.id);
  }

  configureCellActions(config: EntityTableConfig<DashboardInfo>): Array<CellActionDescriptor<DashboardInfo>> {
    const actions: Array<CellActionDescriptor<DashboardInfo>> = [];
    actions.push(
      {
        name: this.translate.instant('dashboard.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => this.exportDashboard($event, entity)
      },
    );
    actions.push(
      {
        name: this.translate.instant('dashboard.dashboard-details'),
        icon: 'edit',
        isEnabled: () => true,
        onAction: ($event, entity) => config.toggleEntityDetails($event, entity)
      }
    );
    return actions;
  }

  configureGroupActions(_config: EntityTableConfig<DashboardInfo>): Array<GroupActionDescriptor<DashboardInfo>> {
    const actions: Array<GroupActionDescriptor<DashboardInfo>> = [];
    return actions;
  }

  configureAddActions(config: EntityTableConfig<DashboardInfo>): Array<HeaderActionDescriptor> {
    return [
      {
        name: this.translate.instant('dashboard.create-new-dashboard'),
        icon: 'insert_drive_file',
        isEnabled: () => true,
        onAction: ($event) => config.getTable().addEntity($event)
      },
      {
        name: this.translate.instant('dashboard.import'),
        icon: 'file_upload',
        isEnabled: () => true,
        onAction: ($event) => this.importDashboard($event, config)
      }
    ];
  }

  generateWithAi($event: Event, config: EntityTableConfig<DashboardInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    const dashboard = this.dashboardUtils.validateAndUpdateDashboard({title: 'New Dashboard'} as Dashboard);
    this.dashboardService.saveDashboard(dashboard).subscribe((savedDashboard) => {
      const url = this.router.createUrlTree([savedDashboard.id.id], {
        relativeTo: config.getTable().route,
        queryParams: {action: 'aiAssistant'}
      });
      this.router.navigateByUrl(url);
    });
  }

  openDashboard($event: Event, dashboard: DashboardInfo, config: EntityTableConfig<DashboardInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([dashboard.id.id], {relativeTo: config.getTable().route});
    this.router.navigateByUrl(url);
  }

  importDashboard(_$event: Event, config: EntityTableConfig<DashboardInfo>) {
    const customerId = config.customerId ? new CustomerId(config.customerId) : null;
    this.importExport.importDashboard(customerId, this.editMissingAliases.bind(this)).subscribe(
      (dashboard) => {
        if (dashboard) {
          config.updateData();
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

  importDashboardFile($event: Event, dashboard: DashboardInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    return this.dialog.open<ImportDashboardFileDialogComponent, DashboardInfoDialogData,
      boolean>(ImportDashboardFileDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        dashboard
      }
    }).afterClosed();
  }

  manageOwnerAndGroups($event: Event, dashboard: DashboardInfo, config: EntityTableConfig<DashboardInfo>) {
    this.homeDialogs.manageOwnerAndGroups($event, dashboard).subscribe(
      (res) => {
        if (res) {
          config.updateData();
        }
      }
    );
  }

  onDashboardAction(action: EntityAction<DashboardInfo>, config: EntityTableConfig<DashboardInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.openDashboard(action.event, action.entity, config);
        return true;
      case 'export':
        this.exportDashboard(action.event, action.entity);
        return true;
      case 'import':
        this.importDashboardFile(action.event, action.entity);
        return true;
      case 'manageOwnerAndGroups':
        this.manageOwnerAndGroups(action.event, action.entity, config);
        return true;
    }
    return false;
  }

  // saveAndAssignDashboard(dashboard: DashboardSetup): Observable<Dashboard> {
  //   const {assignedCustomerIds, ...dashboardToCreate} = dashboard;
  //
  //   return this.dashboardService.saveDashboard(dashboardToCreate as Dashboard).pipe(
  //     mergeMap((createdDashboard) => {
  //       if (assignedCustomerIds?.length) {
  //         return this.dashboardService.addDashboardCustomers(createdDashboard.id.id, assignedCustomerIds);
  //       }
  //       return of(createdDashboard);
  //     })
  //   );
  // }

}
