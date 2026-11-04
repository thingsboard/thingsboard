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
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { map, mergeMap, tap } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { Authority } from '@app/shared/models/authority.enum';
import { CustomerService } from '@core/http/customer.service';
import { Customer } from '@app/shared/models/customer.model';
import { BroadcastService } from '@core/services/broadcast.service';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { EntityViewInfo } from '@app/shared/models/entity-view.models';
import { EntityViewService } from '@core/http/entity-view.service';
import { EntityViewTableHeaderComponent } from '@modules/home/pages/entity-view/entity-view-table-header.component';
import { EdgeService } from '@core/http/edge.service';
import { UtilsService } from '@core/services/utils.service';
import { AllEntitiesTableConfigService } from '@home/components/entity/all-entities-table-config.service';
import { resolveGroupParams } from '@shared/models/entity-group.models';
import { GroupEntityTabsComponent } from '@home/components/group/group-entity-tabs.component';
import { EntityViewComponent } from '@home/pages/entity-view/entity-view.component';
import { AuthUser } from '@shared/models/user.model';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';

@Injectable()
export class EntityViewsTableConfigResolver  {

  constructor(private allEntitiesTableConfigService: AllEntitiesTableConfigService<EntityViewInfo>,
              private store: Store<AppState>,
              private broadcast: BroadcastService,
              private entityViewService: EntityViewService,
              private customerService: CustomerService,
              private edgeService: EdgeService,
              private dialogService: DialogService,
              private homeDialogs: HomeDialogsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private utils: UtilsService,
              private router: Router,
              private dialog: MatDialog) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<EntityViewInfo>> {
    const groupParams = resolveGroupParams(route);
    const config = new EntityTableConfig<EntityViewInfo>(groupParams);
    this.configDefaults(config);
    const authUser = getCurrentAuthUser(this.store);
    config.componentsData = {
      includeCustomers: true,
      entityViewType: '',
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
          config.tableTitle = parentCustomer.title + ': ' + this.translate.instant('entity-view.entity-views');
        } else {
          config.tableTitle = this.translate.instant('entity-view.entity-views');
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

  configDefaults(config: EntityTableConfig<EntityViewInfo>) {
    config.entityType = EntityType.ENTITY_VIEW;
    config.entityComponent = EntityViewComponent;
    config.entityTabsComponent = GroupEntityTabsComponent<EntityViewInfo>;
    config.entityTranslations = entityTypeTranslations.get(EntityType.ENTITY_VIEW);
    config.entityResources = entityTypeResources.get(EntityType.ENTITY_VIEW);

    config.addDialogStyle = {maxWidth: '800px', height: '1060px'};

    config.entityTitle = (entityView) => entityView ?
      this.utils.customTranslation(entityView.name, entityView.name) : '';

    config.rowPointer = true;

    config.deleteEntityTitle = entityView =>
      this.translate.instant('entity-view.delete-entity-view-title', {entityViewName: entityView.name});
    config.deleteEntityContent = () => this.translate.instant('entity-view.delete-entity-view-text');
    config.deleteEntitiesTitle = count => this.translate.instant('entity-view.delete-entity-views-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('entity-view.delete-entity-views-text');

    config.loadEntity = id => this.entityViewService.getEntityViewInfo(id.id);
    config.saveEntity = entityView => this.entityViewService.saveEntityView(entityView).pipe(
        tap(() => {
          this.broadcast.broadcast('entityViewSaved');
        }),
      mergeMap((savedEntityView) => this.entityViewService.getEntityViewInfo(savedEntityView.id.id)
      ));
    config.onEntityAction = action => this.onEntityViewAction(action, config);
    config.headerComponent = EntityViewTableHeaderComponent;
  }

  configureColumns(authUser: AuthUser, config: EntityTableConfig<EntityViewInfo>): Array<EntityColumn<EntityViewInfo>> {
    const columns: Array<EntityColumn<EntityViewInfo>> = [
      new DateEntityTableColumn<EntityViewInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<EntityViewInfo>('name', 'entity-view.name', '25%', config.entityTitle),
      new EntityTableColumn<EntityViewInfo>('type', 'entity-view.entity-view-type', '20%'),
    ];
    if (config.componentsData.includeCustomers) {
      const title = (authUser.authority === Authority.CUSTOMER_USER || config.customerId)
        ? 'entity.sub-customer-name' : 'entity.customer-name';
      columns.push(new EntityTableColumn<EntityViewInfo>('ownerName', title, '25%'));
    }
    columns.push(
      new EntityChipsEntityTableColumn<EntityViewInfo>( 'groups', 'entity.groups', '30%')
    );
    return columns;
  }

  configureEntityFunctions(config: EntityTableConfig<EntityViewInfo>): void {
    if (config.customerId) {
      config.entitiesFetchFunction = pageLink =>
        this.entityViewService.getCustomerEntityViewInfos(config.componentsData.includeCustomers,
          config.customerId, pageLink, config.componentsData.entityViewType);
    } else {
      config.entitiesFetchFunction = pageLink =>
        this.entityViewService.getAllEntityViewInfos(config.componentsData.includeCustomers, pageLink,
          config.componentsData.entityViewType);
    }
    config.deleteEntity = id => this.entityViewService.deleteEntityView(id.id);
  }

  configureCellActions(config: EntityTableConfig<EntityViewInfo>): Array<CellActionDescriptor<EntityViewInfo>> {
    const actions: Array<CellActionDescriptor<EntityViewInfo>> = [];
    return actions;
  }

  configureGroupActions(config: EntityTableConfig<EntityViewInfo>): Array<GroupActionDescriptor<EntityViewInfo>> {
    const actions: Array<GroupActionDescriptor<EntityViewInfo>> = [];
    return actions;
  }

  configureAddActions(config: EntityTableConfig<EntityViewInfo>): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    return actions;
  }

  private openEntityView($event: Event, entityView: EntityViewInfo, config: EntityTableConfig<EntityViewInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([entityView.id.id], {relativeTo: config.getActivatedRoute()});
    this.router.navigateByUrl(url);
  }

  manageOwnerAndGroups($event: Event, entityView: EntityViewInfo, config: EntityTableConfig<EntityViewInfo>) {
    this.homeDialogs.manageOwnerAndGroups($event, entityView).subscribe(
      (res) => {
        if (res) {
          config.updateData();
        }
      }
    );
  }

  onEntityViewAction(action: EntityAction<EntityViewInfo>, config: EntityTableConfig<EntityViewInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.openEntityView(action.event, action.entity, config);
        return true;
      case 'manageOwnerAndGroups':
        this.manageOwnerAndGroups(action.event, action.entity, config);
        return true;
    }
    return false;
  }
}
