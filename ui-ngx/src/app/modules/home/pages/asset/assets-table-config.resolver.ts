///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import {Injectable} from '@angular/core';

import {ActivatedRouteSnapshot, Resolve, Router} from '@angular/router';
import {
  CellActionDescriptor,
  checkBoxCell,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig,
  GroupActionDescriptor,
  HeaderActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import {TranslateService} from '@ngx-translate/core';
import {DatePipe} from '@angular/common';
import {EntityType, entityTypeResources, entityTypeTranslations} from '@shared/models/entity-type.models';
import {EntityAction} from '@home/models/entity/entity-component.models';
import {forkJoin, Observable, of} from 'rxjs';
import {select, Store} from '@ngrx/store';
import {selectAuthUser} from '@core/auth/auth.selectors';
import {map, mergeMap, take, tap} from 'rxjs/operators';
import {AppState} from '@core/core.state';
import {Authority} from '@app/shared/models/authority.enum';
import {CustomerService} from '@core/http/customer.service';
import {Customer} from '@app/shared/models/customer.model';
import {NULL_UUID} from '@shared/models/id/has-uuid';
import {BroadcastService} from '@core/services/broadcast.service';
import {MatDialog} from '@angular/material/dialog';
import {DialogService} from '@core/services/dialog.service';
import {
  AssignToCustomerDialogComponent,
  AssignToCustomerDialogData
} from '@modules/home/dialogs/assign-to-customer-dialog.component';
import {
  AddEntitiesToCustomerDialogComponent,
  AddEntitiesToCustomerDialogData
} from '../../dialogs/add-entities-to-customer-dialog.component';
import {Asset, AssetInfo} from '@app/shared/models/asset.models';
import {AssetService} from '@app/core/http/asset.service';
import {AssetComponent} from '@modules/home/pages/asset/asset.component';
import {AssetTableHeaderComponent} from '@modules/home/pages/asset/asset-table-header.component';
import {AssetId} from '@app/shared/models/id/asset-id';
import {AssetTabsComponent} from '@home/pages/asset/asset-tabs.component';
import {HomeDialogsService} from '@home/dialogs/home-dialogs.service';
import {DeviceInfo} from '@shared/models/device.models';
import {EdgeService} from '@core/http/edge.service';
import {
  AddEntitiesToEdgeDialogComponent,
  AddEntitiesToEdgeDialogData
} from '@home/dialogs/add-entities-to-edge-dialog.component';

@Injectable()
export class AssetsTableConfigResolver implements Resolve<EntityTableConfig<AssetInfo>> {

  private readonly config: EntityTableConfig<AssetInfo> = new EntityTableConfig<AssetInfo>();

  private customerId: string;

  constructor(private store: Store<AppState>,
              private broadcast: BroadcastService,
              private assetService: AssetService,
              private customerService: CustomerService,
              private edgeService: EdgeService,
              private dialogService: DialogService,
              private homeDialogs: HomeDialogsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private dialog: MatDialog) {

    this.config.entityType = EntityType.ASSET;
    this.config.entityComponent = AssetComponent;
    this.config.entityTabsComponent = AssetTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.ASSET);
    this.config.entityResources = entityTypeResources.get(EntityType.ASSET);

    this.config.deleteEntityTitle = asset => this.translate.instant('asset.delete-asset-title', { assetName: asset.name });
    this.config.deleteEntityContent = () => this.translate.instant('asset.delete-asset-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('asset.delete-assets-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('asset.delete-assets-text');

    this.config.loadEntity = id => this.assetService.getAssetInfo(id.id);
    this.config.saveEntity = asset => {
      return this.assetService.saveAsset(asset).pipe(
        tap(() => {
          this.broadcast.broadcast('assetSaved');
        }),
        mergeMap((savedAsset) => this.assetService.getAssetInfo(savedAsset.id.id)
        ));
    };
    this.config.onEntityAction = action => this.onAssetAction(action);
    this.config.detailsReadonly = () => (this.config.componentsData.assetScope === 'customer_user' || this.config.componentsData.assetScope === 'edge_customer_user');

    this.config.headerComponent = AssetTableHeaderComponent;

  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<AssetInfo>> {
    const routeParams = route.params;
    this.config.componentsData = {
      assetScope: route.data.assetsType,
      assetType: '',
      edgeId: routeParams.edgeId
    };
    this.customerId = routeParams.customerId;
    return this.store.pipe(select(selectAuthUser), take(1)).pipe(
      tap((authUser) => {
        if (authUser.authority === Authority.CUSTOMER_USER) {
          if (route.data.assetsType === 'edge') {
            this.config.componentsData.assetScope = 'edge_customer_user';
          } else {
            this.config.componentsData.assetScope = 'customer_user';
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
            this.config.tableTitle = this.translate.instant('customer.public-assets');
          } else {
            this.config.tableTitle = parentCustomer.title + ': ' + this.translate.instant('asset.assets');
          }
        } else if (this.config.componentsData.assetScope === 'edge') {
          this.edgeService.getEdge(this.config.componentsData.edgeId).subscribe(
            edge => this.config.tableTitle = edge.name + ': ' + this.translate.instant('asset.assets')
          );
        } else {
          this.config.tableTitle = this.translate.instant('asset.assets');
        }
        this.config.columns = this.configureColumns(this.config.componentsData.assetScope);
        this.configureEntityFunctions(this.config.componentsData.assetScope);
        this.config.cellActionDescriptors = this.configureCellActions(this.config.componentsData.assetScope);
        this.config.groupActionDescriptors = this.configureGroupActions(this.config.componentsData.assetScope);
        this.config.addActionDescriptors = this.configureAddActions(this.config.componentsData.assetScope);
        this.config.addEnabled = !(this.config.componentsData.assetScope === 'customer_user' || this.config.componentsData.assetScope === 'edge_customer_user');
        this.config.entitiesDeleteEnabled = this.config.componentsData.assetScope === 'tenant';
        this.config.deleteEnabled = () => this.config.componentsData.assetScope === 'tenant';
        return this.config;
      })
    );
  }

  configureColumns(assetScope: string): Array<EntityTableColumn<AssetInfo>> {
    const columns: Array<EntityTableColumn<AssetInfo>> = [
      new DateEntityTableColumn<AssetInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<AssetInfo>('name', 'asset.name', '25%'),
      new EntityTableColumn<AssetInfo>('type', 'asset.asset-type', '25%'),
      new EntityTableColumn<DeviceInfo>('label', 'asset.label', '25%'),
    ];
    if (assetScope === 'tenant') {
      columns.push(
        new EntityTableColumn<AssetInfo>('customerTitle', 'customer.customer', '25%'),
        new EntityTableColumn<AssetInfo>('customerIsPublic', 'asset.public', '60px',
          entity => {
            return checkBoxCell(entity.customerIsPublic);
          }, () => ({}), false),
      );
    }
    return columns;
  }

  configureEntityFunctions(assetScope: string): void {
    if (assetScope === 'tenant') {
      this.config.entitiesFetchFunction = pageLink =>
        this.assetService.getTenantAssetInfos(pageLink, this.config.componentsData.assetType);
      this.config.deleteEntity = id => this.assetService.deleteAsset(id.id);
    } else if (assetScope === 'edge' || assetScope === 'edge_customer_user') {
      this.config.entitiesFetchFunction = pageLink =>
        this.assetService.getEdgeAssets(this.config.componentsData.edgeId, pageLink, this.config.componentsData.assetType);
    } else {
      this.config.entitiesFetchFunction = pageLink =>
        this.assetService.getCustomerAssetInfos(this.customerId, pageLink, this.config.componentsData.assetType);
      this.config.deleteEntity = id => this.assetService.unassignAssetFromCustomer(id.id);
    }
  }

  configureCellActions(assetScope: string): Array<CellActionDescriptor<AssetInfo>> {
    const actions: Array<CellActionDescriptor<AssetInfo>> = [];
    if (assetScope === 'tenant') {
      actions.push(
        {
          name: this.translate.instant('asset.make-public'),
          icon: 'share',
          isEnabled: (entity) => (!entity.customerId || entity.customerId.id === NULL_UUID),
          onAction: ($event, entity) => this.makePublic($event, entity)
        },
        {
          name: this.translate.instant('asset.assign-to-customer'),
          icon: 'assignment_ind',
          isEnabled: (entity) => (!entity.customerId || entity.customerId.id === NULL_UUID),
          onAction: ($event, entity) => this.assignToCustomer($event, [entity.id])
        },
        {
          name: this.translate.instant('asset.unassign-from-customer'),
          icon: 'assignment_return',
          isEnabled: (entity) => (entity.customerId && entity.customerId.id !== NULL_UUID && !entity.customerIsPublic),
          onAction: ($event, entity) => this.unassignFromCustomer($event, entity)
        },
        {
          name: this.translate.instant('asset.make-private'),
          icon: 'reply',
          isEnabled: (entity) => (entity.customerId && entity.customerId.id !== NULL_UUID && entity.customerIsPublic),
          onAction: ($event, entity) => this.unassignFromCustomer($event, entity)
        }
      );
    }
    if (assetScope === 'customer') {
      actions.push(
        {
          name: this.translate.instant('asset.unassign-from-customer'),
          icon: 'assignment_return',
          isEnabled: (entity) => (entity.customerId && entity.customerId.id !== NULL_UUID && !entity.customerIsPublic),
          onAction: ($event, entity) => this.unassignFromCustomer($event, entity)
        },
        {
          name: this.translate.instant('asset.make-private'),
          icon: 'reply',
          isEnabled: (entity) => (entity.customerId && entity.customerId.id !== NULL_UUID && entity.customerIsPublic),
          onAction: ($event, entity) => this.unassignFromCustomer($event, entity)
        }
      );
    }
    if (assetScope === 'edge') {
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

  configureGroupActions(assetScope: string): Array<GroupActionDescriptor<AssetInfo>> {
    const actions: Array<GroupActionDescriptor<AssetInfo>> = [];
    if (assetScope === 'tenant') {
      actions.push(
        {
          name: this.translate.instant('asset.assign-assets'),
          icon: 'assignment_ind',
          isEnabled: true,
          onAction: ($event, entities) => this.assignToCustomer($event, entities.map((entity) => entity.id))
        }
      );
    }
    if (assetScope === 'customer') {
      actions.push(
        {
          name: this.translate.instant('asset.unassign-assets'),
          icon: 'assignment_return',
          isEnabled: true,
          onAction: ($event, entities) => this.unassignAssetsFromCustomer($event, entities)
        }
      );
    }
    if (assetScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('asset.unassign-assets-from-edge'),
          icon: 'assignment_return',
          isEnabled: true,
          onAction: ($event, entities) => this.unassignAssetsFromEdge($event, entities)
        }
      );
    }
    return actions;
  }

  configureAddActions(assetScope: string): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    if (assetScope === 'tenant') {
      actions.push(
        {
          name: this.translate.instant('asset.add-asset-text'),
          icon: 'insert_drive_file',
          isEnabled: () => true,
          onAction: ($event) => this.config.table.addEntity($event)
        },
        {
          name: this.translate.instant('asset.import'),
          icon: 'file_upload',
          isEnabled: () => true,
          onAction: ($event) => this.importAssets($event)
        }
      );
    }
    if (assetScope === 'customer') {
      actions.push(
        {
          name: this.translate.instant('asset.assign-new-asset'),
          icon: 'add',
          isEnabled: () => true,
          onAction: ($event) => this.addAssetsToCustomer($event)
        }
      );
    }
    if (assetScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('asset.assign-new-asset'),
          icon: 'add',
          isEnabled: () => true,
          onAction: ($event) => this.addAssetsToEdge($event)
        }
      );
    }
    return actions;
  }

  importAssets($event: Event) {
    this.homeDialogs.importEntities(EntityType.ASSET).subscribe((res) => {
      if (res) {
        this.broadcast.broadcast('assetSaved');
        this.config.table.updateData();
      }
    });
  }

  addAssetsToCustomer($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AddEntitiesToCustomerDialogComponent, AddEntitiesToCustomerDialogData,
      boolean>(AddEntitiesToCustomerDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        customerId: this.customerId,
        entityType: EntityType.ASSET
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.config.table.updateData();
        }
      });
  }

  makePublic($event: Event, asset: Asset) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('asset.make-public-asset-title', {assetName: asset.name}),
      this.translate.instant('asset.make-public-asset-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.assetService.makeAssetPublic(asset.id.id).subscribe(
            () => {
              this.config.table.updateData();
            }
          );
        }
      }
    );
  }

  assignToCustomer($event: Event, assetIds: Array<AssetId>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AssignToCustomerDialogComponent, AssignToCustomerDialogData,
      boolean>(AssignToCustomerDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entityIds: assetIds,
        entityType: EntityType.ASSET
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.config.table.updateData();
        }
      });
  }

  unassignFromCustomer($event: Event, asset: AssetInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const isPublic = asset.customerIsPublic;
    let title;
    let content;
    if (isPublic) {
      title = this.translate.instant('asset.make-private-asset-title', {assetName: asset.name});
      content = this.translate.instant('asset.make-private-asset-text');
    } else {
      title = this.translate.instant('asset.unassign-asset-title', {assetName: asset.name});
      content = this.translate.instant('asset.unassign-asset-text');
    }
    this.dialogService.confirm(
      title,
      content,
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.assetService.unassignAssetFromCustomer(asset.id.id).subscribe(
            () => {
              this.config.table.updateData();
            }
          );
        }
      }
    );
  }

  unassignAssetsFromCustomer($event: Event, assets: Array<AssetInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('asset.unassign-assets-title', {count: assets.length}),
      this.translate.instant('asset.unassign-assets-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          const tasks: Observable<any>[] = [];
          assets.forEach(
            (asset) => {
              tasks.push(this.assetService.unassignAssetFromCustomer(asset.id.id));
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

  onAssetAction(action: EntityAction<AssetInfo>): boolean {
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

  addAssetsToEdge($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AddEntitiesToEdgeDialogComponent, AddEntitiesToEdgeDialogData,
      boolean>(AddEntitiesToEdgeDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        edgeId: this.config.componentsData.edgeId,
        entityType: EntityType.ASSET
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.config.table.updateData();
        }
      });
  }

  unassignFromEdge($event: Event, asset: AssetInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('asset.unassign-asset-from-edge-title', {assetName: asset.name}),
      this.translate.instant('asset.unassign-asset-from-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.assetService.unassignAssetFromEdge(this.config.componentsData.edgeId, asset.id.id).subscribe(
            () => {
              this.config.table.updateData();
            }
          );
        }
      }
    );
  }

  unassignAssetsFromEdge($event: Event, assets: Array<AssetInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('asset.unassign-assets-from-edge-title', {count: assets.length}),
      this.translate.instant('asset.unassign-assets-from-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          const tasks: Observable<any>[] = [];
          assets.forEach(
            (asset) => {
              tasks.push(this.assetService.unassignAssetFromEdge(this.config.componentsData.edgeId, asset.id.id));
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
