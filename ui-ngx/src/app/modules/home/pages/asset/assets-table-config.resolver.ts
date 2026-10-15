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
import { AssetInfo } from '@app/shared/models/asset.models';
import { AssetService } from '@app/core/http/asset.service';
import { AssetTableHeaderComponent } from '@modules/home/pages/asset/asset-table-header.component';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { UtilsService } from '@core/services/utils.service';
import { AllEntitiesTableConfigService } from '@home/components/entity/all-entities-table-config.service';
import { resolveGroupParams } from '@shared/models/entity-group.models';
import { GroupEntityTabsComponent } from '@home/components/group/group-entity-tabs.component';
import { AssetComponent } from '@home/pages/asset/asset.component';
import { AuthUser } from '@shared/models/user.model';
import { CustomerId } from '@shared/models/id/customer-id';

@Injectable()
export class AssetsTableConfigResolver  {

  constructor(private allEntitiesTableConfigService: AllEntitiesTableConfigService<AssetInfo>,
              private store: Store<AppState>,
              private broadcast: BroadcastService,
              private assetService: AssetService,
              private customerService: CustomerService,
              private dialogService: DialogService,
              private homeDialogs: HomeDialogsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private utils: UtilsService,
              private router: Router,
              private dialog: MatDialog) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<AssetInfo>> {
    const groupParams = resolveGroupParams(route);
    const config = new EntityTableConfig<AssetInfo>(groupParams);
    this.configDefaults(config);
    const authUser = getCurrentAuthUser(this.store);
    config.componentsData = {
      includeCustomers: true,
      assetProfileId: null,
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
          config.tableTitle = parentCustomer.title + ': ' + this.translate.instant('asset.assets');
        } else {
          config.tableTitle = this.translate.instant('asset.assets');
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

  configDefaults(config: EntityTableConfig<AssetInfo>) {
    config.entityType = EntityType.ASSET;
    config.entityComponent = AssetComponent;
    config.entityTabsComponent = GroupEntityTabsComponent<AssetInfo>;
    config.entityTranslations = entityTypeTranslations.get(EntityType.ASSET);
    config.entityResources = entityTypeResources.get(EntityType.ASSET);
    config.addDialogStyle = {height: '740px'};
    config.addDialogOwnerAndGroupWizard = false;

    config.entityTitle = (asset) => asset ?
      this.utils.customTranslation(asset.name, asset.name) : '';

    config.rowPointer = true;

    config.deleteEntityTitle = asset => this.translate.instant('asset.delete-asset-title', { assetName: asset.name });
    config.deleteEntityContent = () => this.translate.instant('asset.delete-asset-text');
    config.deleteEntitiesTitle = count => this.translate.instant('asset.delete-assets-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('asset.delete-assets-text');

    config.loadEntity = id => this.assetService.getAssetInfo(id.id);
    config.saveEntity = asset => this.assetService.saveAsset(asset).pipe(
      tap(() => {
        this.broadcast.broadcast('assetSaved');
      }),
      mergeMap((savedAsset) => this.assetService.getAssetInfo(savedAsset.id.id)
      ));
    config.onEntityAction = action => this.onAssetAction(action, config);
    config.headerComponent = AssetTableHeaderComponent;
  }

  configureColumns(authUser: AuthUser, config: EntityTableConfig<AssetInfo>): Array<EntityColumn<AssetInfo>> {
    const columns: Array<EntityColumn<AssetInfo>> = [
      new DateEntityTableColumn<AssetInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<AssetInfo>('name', 'asset.name', '20%', config.entityTitle),
      new EntityTableColumn<AssetInfo>('type', 'asset-profile.asset-profile', '20%'),
      new EntityTableColumn<AssetInfo>('label', 'asset.label', '15%'),
    ];
    if (config.componentsData.includeCustomers) {
      const title = (authUser.authority === Authority.CUSTOMER_USER || config.customerId)
        ? 'entity.sub-customer-name' : 'entity.customer-name';
      columns.push(new EntityTableColumn<AssetInfo>('ownerName', title, '20%'));
    }
    columns.push(
      new EntityChipsEntityTableColumn<AssetInfo>( 'groups', 'entity.groups', '25%')
    );
    return columns;
  }

  configureEntityFunctions(config: EntityTableConfig<AssetInfo>): void {
    if (config.customerId) {
      config.entitiesFetchFunction = pageLink =>
        this.assetService.getCustomerAssetInfos(config.componentsData.includeCustomers,
          config.customerId, pageLink, config.componentsData.assetProfileId !== null ?
            config.componentsData.assetProfileId.id : '');
    } else {
      config.entitiesFetchFunction = pageLink =>
        this.assetService.getAllAssetInfos(config.componentsData.includeCustomers, pageLink,
          config.componentsData.assetProfileId !== null ?
            config.componentsData.assetProfileId.id : '');
    }
    config.deleteEntity = id => this.assetService.deleteAsset(id.id);
  }

  configureCellActions(config: EntityTableConfig<AssetInfo>): Array<CellActionDescriptor<AssetInfo>> {
    const actions: Array<CellActionDescriptor<AssetInfo>> = [];
    return actions;
  }

  configureGroupActions(config: EntityTableConfig<AssetInfo>): Array<GroupActionDescriptor<AssetInfo>> {
    const actions: Array<GroupActionDescriptor<AssetInfo>> = [];
    return actions;
  }

  configureAddActions(config: EntityTableConfig<AssetInfo>): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    actions.push(
      {
        name: this.translate.instant('asset.add-asset-text'),
        icon: 'insert_drive_file',
        isEnabled: () => true,
        onAction: ($event) => config.getTable().addEntity($event)
      },
      {
        name: this.translate.instant('asset.import'),
        icon: 'file_upload',
        isEnabled: () => true,
        onAction: ($event) => this.importAssets($event, config)
      }
    );
    return actions;
  }

  private openAsset($event: Event, asset: AssetInfo, config: EntityTableConfig<AssetInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([asset.id.id], {relativeTo: config.getActivatedRoute()});
    this.router.navigateByUrl(url);
  }

  importAssets($event: Event, config: EntityTableConfig<AssetInfo>) {
    const customerId = config.customerId ? new CustomerId(config.customerId) : null;
    this.homeDialogs.importEntities(customerId, EntityType.ASSET, null).subscribe((res) => {
      if (res) {
        this.broadcast.broadcast('assetSaved');
        config.updateData();
      }
    });
  }

  manageOwnerAndGroups($event: Event, asset: AssetInfo, config: EntityTableConfig<AssetInfo>) {
    this.homeDialogs.manageOwnerAndGroups($event, asset).subscribe(
      (res) => {
        if (res) {
          config.updateData();
        }
      }
    );
  }

  onAssetAction(action: EntityAction<AssetInfo>, config: EntityTableConfig<AssetInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.openAsset(action.event, action.entity, config);
        return true;
      case 'manageOwnerAndGroups':
        this.manageOwnerAndGroups(action.event, action.entity, config);
        return true;
    }
    return false;
  }
}
