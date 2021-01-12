///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { ActivatedRouteSnapshot, Resolve, Router } from '@angular/router';
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
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { BroadcastService } from '@core/services/broadcast.service';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import {
  AssignToCustomerDialogComponent,
  AssignToCustomerDialogData
} from '@modules/home/dialogs/assign-to-customer-dialog.component';
import {
  AddEntitiesToCustomerDialogComponent,
  AddEntitiesToCustomerDialogData
} from '../../dialogs/add-entities-to-customer-dialog.component';
import { EntityView, EntityViewInfo } from '@app/shared/models/entity-view.models';
import { EntityViewService } from '@core/http/entity-view.service';
import { EntityViewComponent } from '@modules/home/pages/entity-view/entity-view.component';
import { EntityViewTableHeaderComponent } from '@modules/home/pages/entity-view/entity-view-table-header.component';
import { EntityViewId } from '@shared/models/id/entity-view-id';
import { EntityViewTabsComponent } from '@home/pages/entity-view/entity-view-tabs.component';
import { EdgeService } from '@core/http/edge.service';
import {
  AddEntitiesToEdgeDialogComponent,
  AddEntitiesToEdgeDialogData
} from '@home/dialogs/add-entities-to-edge-dialog.component';

@Injectable()
export class EntityViewsTableConfigResolver implements Resolve<EntityTableConfig<EntityViewInfo>> {

  private readonly config: EntityTableConfig<EntityViewInfo> = new EntityTableConfig<EntityViewInfo>();

  private customerId: string;

  constructor(private store: Store<AppState>,
              private broadcast: BroadcastService,
              private entityViewService: EntityViewService,
              private customerService: CustomerService,
              private edgeService: EdgeService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private dialog: MatDialog) {

    this.config.entityType = EntityType.ENTITY_VIEW;
    this.config.entityComponent = EntityViewComponent;
    this.config.entityTabsComponent = EntityViewTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.ENTITY_VIEW);
    this.config.entityResources = entityTypeResources.get(EntityType.ENTITY_VIEW);

    this.config.addDialogStyle = {maxWidth: '800px'};

    this.config.deleteEntityTitle = entityView =>
      this.translate.instant('entity-view.delete-entity-view-title', { entityViewName: entityView.name });
    this.config.deleteEntityContent = () => this.translate.instant('entity-view.delete-entity-view-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('entity-view.delete-entity-views-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('entity-view.delete-entity-views-text');

    this.config.loadEntity = id => this.entityViewService.getEntityViewInfo(id.id);
    this.config.saveEntity = entityView => {
      return this.entityViewService.saveEntityView(entityView).pipe(
        tap(() => {
          this.broadcast.broadcast('entityViewSaved');
        }),
        mergeMap((savedEntityView) => this.entityViewService.getEntityViewInfo(savedEntityView.id.id)
        ));
    };
    this.config.onEntityAction = action => this.onEntityViewAction(action);
    this.config.detailsReadonly = () => this.config.componentsData.entityViewScope === 'customer_user';

    this.config.headerComponent = EntityViewTableHeaderComponent;

  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<EntityViewInfo>> {
    const routeParams = route.params;
    this.config.componentsData = {
      entityViewScope: route.data.entityViewsType,
      entityViewType: '',
      edgeId: routeParams.edgeId
    };
    this.customerId = routeParams.customerId;
    return this.store.pipe(select(selectAuthUser), take(1)).pipe(
      tap((authUser) => {
        if (authUser.authority === Authority.CUSTOMER_USER) {
          if (route.data.entityViewsType === 'edge') {
            this.config.componentsData.entityViewScope = 'edge_customer_user';
          } else {
            this.config.componentsData.entityViewScope = 'customer_user';
          }
          this.customerId = authUser.customerId;
        }
      }),
      mergeMap(() =>
        this.customerId ? this.customerService.getCustomer(this.customerId) : of(null as Customer)
      ),
      map((parentCustomer) => {
        if (parentCustomer) {
          if (parentCustomer.additionalInfo && parentCustomer.additionalInfo.isPublic) {
            this.config.tableTitle = this.translate.instant('customer.public-entity-views');
          } else {
            this.config.tableTitle = parentCustomer.title + ': ' + this.translate.instant('entity-view.entity-views');
          }
        } else if (this.config.componentsData.entityViewScope === 'edge') {
          this.edgeService.getEdge(this.config.componentsData.edgeId).subscribe(
            edge => this.config.tableTitle = edge.name + ': ' + this.translate.instant('entity-view.entity-views')
          );
        }
        else {
          this.config.tableTitle = this.translate.instant('entity-view.entity-views');
        }
        this.config.columns = this.configureColumns(this.config.componentsData.entityViewScope);
        this.configureEntityFunctions(this.config.componentsData.entityViewScope);
        this.config.cellActionDescriptors = this.configureCellActions(this.config.componentsData.entityViewScope);
        this.config.groupActionDescriptors = this.configureGroupActions(this.config.componentsData.entityViewScope);
        this.config.addActionDescriptors = this.configureAddActions(this.config.componentsData.entityViewScope);
        this.config.addEnabled = this.config.componentsData.entityViewScope !== 'customer_user';
        this.config.entitiesDeleteEnabled = this.config.componentsData.entityViewScope === 'tenant';
        this.config.deleteEnabled = () => this.config.componentsData.entityViewScope === 'tenant';
        return this.config;
      })
    );
  }

  configureColumns(entityViewScope: string): Array<EntityTableColumn<EntityViewInfo>> {
    const columns: Array<EntityTableColumn<EntityViewInfo>> = [
      new DateEntityTableColumn<EntityViewInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<EntityViewInfo>('name', 'entity-view.name', '33%'),
      new EntityTableColumn<EntityViewInfo>('type', 'entity-view.entity-view-type', '33%'),
    ];
    if (entityViewScope === 'tenant') {
      columns.push(
        new EntityTableColumn<EntityViewInfo>('customerTitle', 'customer.customer', '33%'),
        new EntityTableColumn<EntityViewInfo>('customerIsPublic', 'entity-view.public', '60px',
          entity => {
            return checkBoxCell(entity.customerIsPublic);
          }, () => ({}), false),
      );
    }
    return columns;
  }

  configureEntityFunctions(entityViewScope: string): void {
    if (entityViewScope === 'tenant') {
      this.config.entitiesFetchFunction = pageLink =>
        this.entityViewService.getTenantEntityViewInfos(pageLink, this.config.componentsData.entityViewType);
      this.config.deleteEntity = id => this.entityViewService.deleteEntityView(id.id);
    } else if (entityViewScope === 'edge' || entityViewScope === 'edge_customer_user') {
      this.config.entitiesFetchFunction = pageLink =>
        this.entityViewService.getEdgeEntityViews(this.config.componentsData.edgeId, pageLink, this.config.componentsData.entityViewType);
    } else {
      this.config.entitiesFetchFunction = pageLink =>
        this.entityViewService.getCustomerEntityViewInfos(this.customerId, pageLink, this.config.componentsData.entityViewType);
      this.config.deleteEntity = id => this.entityViewService.unassignEntityViewFromCustomer(id.id);
    }
  }

  configureCellActions(entityViewScope: string): Array<CellActionDescriptor<EntityViewInfo>> {
    const actions: Array<CellActionDescriptor<EntityViewInfo>> = [];
    if (entityViewScope === 'tenant') {
      actions.push(
        {
          name: this.translate.instant('entity-view.make-public'),
          icon: 'share',
          isEnabled: (entity) => (!entity.customerId || entity.customerId.id === NULL_UUID),
          onAction: ($event, entity) => this.makePublic($event, entity)
        },
        {
          name: this.translate.instant('entity-view.assign-to-customer'),
          icon: 'assignment_ind',
          isEnabled: (entity) => (!entity.customerId || entity.customerId.id === NULL_UUID),
          onAction: ($event, entity) => this.assignToCustomer($event, [entity.id])
        },
        {
          name: this.translate.instant('entity-view.unassign-from-customer'),
          icon: 'assignment_return',
          isEnabled: (entity) => (entity.customerId && entity.customerId.id !== NULL_UUID && !entity.customerIsPublic),
          onAction: ($event, entity) => this.unassignFromCustomer($event, entity)
        },
        {
          name: this.translate.instant('entity-view.make-private'),
          icon: 'reply',
          isEnabled: (entity) => (entity.customerId && entity.customerId.id !== NULL_UUID && entity.customerIsPublic),
          onAction: ($event, entity) => this.unassignFromCustomer($event, entity)
        }
      );
    }
    if (entityViewScope === 'customer') {
      actions.push(
        {
          name: this.translate.instant('entity-view.unassign-from-customer'),
          icon: 'assignment_return',
          isEnabled: (entity) => (entity.customerId && entity.customerId.id !== NULL_UUID && !entity.customerIsPublic),
          onAction: ($event, entity) => this.unassignFromCustomer($event, entity)
        },
        {
          name: this.translate.instant('entity-view.make-private'),
          icon: 'reply',
          isEnabled: (entity) => (entity.customerId && entity.customerId.id !== NULL_UUID && entity.customerIsPublic),
          onAction: ($event, entity) => this.unassignFromCustomer($event, entity)
        }
      );
    }
    if (entityViewScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('edge.unassign-from-edge'),
          icon: 'assignment_return',
          isEnabled: (entity) => true,
          onAction: ($event, entity) => this.unassignFromEdge($event, entity)
        }
      );
    }
    return actions;
  }

  configureGroupActions(entityViewScope: string): Array<GroupActionDescriptor<EntityViewInfo>> {
    const actions: Array<GroupActionDescriptor<EntityViewInfo>> = [];
    if (entityViewScope === 'tenant') {
      actions.push(
        {
          name: this.translate.instant('entity-view.assign-entity-views'),
          icon: 'assignment_ind',
          isEnabled: true,
          onAction: ($event, entities) => this.assignToCustomer($event, entities.map((entity) => entity.id))
        }
      );
    }
    if (entityViewScope === 'customer') {
      actions.push(
        {
          name: this.translate.instant('entity-view.unassign-entity-views'),
          icon: 'assignment_return',
          isEnabled: true,
          onAction: ($event, entities) => this.unassignEntityViewsFromCustomer($event, entities)
        }
      );
    }
    if (entityViewScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('entity-view.unassign-entity-views-from-edge'),
          icon: 'assignment_return',
          isEnabled: true,
          onAction: ($event, entities) => this.unassignEntityViewsFromEdge($event, entities)
        }
      );
    }
    return actions;
  }

  configureAddActions(entityViewScope: string): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    if (entityViewScope === 'customer') {
      actions.push(
        {
          name: this.translate.instant('entity-view.assign-new-entity-view'),
          icon: 'add',
          isEnabled: () => true,
          onAction: ($event) => this.addEntityViewsToCustomer($event)
        }
      );
    }
    if (entityViewScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('entity-view.assign-new-entity-view'),
          icon: 'add',
          isEnabled: () => true,
          onAction: ($event) => this.addEntityViewsToEdge($event)
        }
      );
    }
    return actions;
  }

  addEntityViewsToCustomer($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AddEntitiesToCustomerDialogComponent, AddEntitiesToCustomerDialogData,
      boolean>(AddEntitiesToCustomerDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        customerId: this.customerId,
        entityType: EntityType.ENTITY_VIEW
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.config.table.updateData();
        }
      });
  }

  makePublic($event: Event, entityView: EntityView) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('entity-view.make-public-entity-view-title', {entityViewName: entityView.name}),
      this.translate.instant('entity-view.make-public-entity-view-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.entityViewService.makeEntityViewPublic(entityView.id.id).subscribe(
            () => {
              this.config.table.updateData();
            }
          );
        }
      }
    );
  }

  assignToCustomer($event: Event, entityViewIds: Array<EntityViewId>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AssignToCustomerDialogComponent, AssignToCustomerDialogData,
      boolean>(AssignToCustomerDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entityIds: entityViewIds,
        entityType: EntityType.ENTITY_VIEW
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.config.table.updateData();
        }
      });
  }

  unassignFromCustomer($event: Event, entityView: EntityViewInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const isPublic = entityView.customerIsPublic;
    let title;
    let content;
    if (isPublic) {
      title = this.translate.instant('entity-view.make-private-entity-view-title', {entityViewName: entityView.name});
      content = this.translate.instant('entity-view.make-private-entity-view-text');
    } else {
      title = this.translate.instant('entity-view.unassign-entity-view-title', {entityViewName: entityView.name});
      content = this.translate.instant('entity-view.unassign-entity-view-text');
    }
    this.dialogService.confirm(
      title,
      content,
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.entityViewService.unassignEntityViewFromCustomer(entityView.id.id).subscribe(
            () => {
              this.config.table.updateData();
            }
          );
        }
      }
    );
  }

  unassignEntityViewsFromCustomer($event: Event, entityViews: Array<EntityViewInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('entity-view.unassign-entity-views-title', {count: entityViews.length}),
      this.translate.instant('entity-view.unassign-entity-views-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          const tasks: Observable<any>[] = [];
          entityViews.forEach(
            (entityView) => {
              tasks.push(this.entityViewService.unassignEntityViewFromCustomer(entityView.id.id));
            }
          );
          forkJoin(tasks).subscribe(
            () => {
              this.config.table.updateData();
            }
          );
        }
      }
    );
  }

  onEntityViewAction(action: EntityAction<EntityViewInfo>): boolean {
    switch (action.action) {
      case 'makePublic':
        this.makePublic(action.event, action.entity);
        return true;
      case 'assignToCustomer':
        this.assignToCustomer(action.event, [action.entity.id]);
        return true;
      case 'unassignFromCustomer':
        this.unassignFromCustomer(action.event, action.entity);
        return true;
      case 'unassignFromEdge':
        this.unassignFromEdge(action.event, action.entity);
        return true;
    }
    return false;
  }

  addEntityViewsToEdge($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AddEntitiesToEdgeDialogComponent, AddEntitiesToEdgeDialogData,
      boolean>(AddEntitiesToEdgeDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        edgeId: this.config.componentsData.edgeId,
        entityType: EntityType.ENTITY_VIEW
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.config.table.updateData();
        }
      });
  }

  unassignFromEdge($event: Event, entityView: EntityViewInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('entity-view.unassign-entity-view-from-edge-title', {entityViewName: entityView.name}),
      this.translate.instant('entity-view.unassign-entity-view-from-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.entityViewService.unassignEntityViewFromEdge(this.config.componentsData.edgeId, entityView.id.id).subscribe(
            () => {
              this.config.table.updateData();
            }
          );
        }
      }
    );
  }

  unassignEntityViewsFromEdge($event: Event, entityViews: Array<EntityViewInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('entity-view.unassign-entity-views-from-edge-title', {count: entityViews.length}),
      this.translate.instant('entity-view.unassign-entity-views-from-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          const tasks: Observable<any>[] = [];
          entityViews.forEach(
            (entityView) => {
              tasks.push(this.entityViewService.unassignEntityViewFromEdge(this.config.componentsData.edgeId, entityView.id.id));
            }
          );
          forkJoin(tasks).subscribe(
            () => {
              this.config.table.updateData();
            }
          );
        }
      }
    );
  }

}
