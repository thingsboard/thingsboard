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
import { EntityType } from '@shared/models/entity-type.models';
import { mergeMap, tap } from 'rxjs/operators';
import { BroadcastService } from '@core/services/broadcast.service';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { MatDialog } from '@angular/material/dialog';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityGroupParams } from '@shared/models/entity-group.models';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { CustomerId } from '@shared/models/id/customer-id';
import { GroupConfigTableConfigService } from '@home/components/group/group-config-table-config.service';
import { AssetInfo } from '@shared/models/asset.models';
import { AssetService } from '@core/http/asset.service';
import { AssetComponent } from '@home/pages/asset/asset.component';
import { Operation } from '@shared/models/security.models';
import { Router, UrlTree } from '@angular/router';
import { WINDOW } from '@core/services/window.service';

@Injectable()
export class AssetGroupConfigFactory implements EntityGroupStateConfigFactory<AssetInfo> {

  constructor(private groupConfigTableConfigService: GroupConfigTableConfigService<AssetInfo>,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private utils: UtilsService,
              private dialog: MatDialog,
              private homeDialogs: HomeDialogsService,
              private assetService: AssetService,
              private router: Router,
              private broadcast: BroadcastService,
              @Inject(WINDOW) private window: Window) {
  }

  createConfig(params: EntityGroupParams, entityGroup: EntityGroupStateInfo<AssetInfo>): Observable<GroupEntityTableConfig<AssetInfo>> {
    const config = new GroupEntityTableConfig<AssetInfo>(entityGroup, params);

    config.entityComponent = AssetComponent;
    config.addDialogStyle = {height: '620px'};

    config.entityTitle = (asset) => asset ?
      this.utils.customTranslation(asset.name, asset.name) : '';

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
    config.deleteEntity = id => this.assetService.deleteAsset(id.id);

    config.onEntityAction = action => this.onAssetAction(action, config, params);

    if (this.userPermissionsService.hasGroupEntityPermission(Operation.CREATE, config.entityGroup)) {
      config.headerActionDescriptors.push(
        {
          name: this.translate.instant('asset.import'),
          icon: 'file_upload',
          isEnabled: () => true,
          onAction: ($event) => this.importAssets($event, config)
        }
      );
    }
    return this.groupConfigTableConfigService.prepareConfiguration(params, config);
  }

  importAssets($event: Event, config: GroupEntityTableConfig<AssetInfo>) {
    const entityGroup = config.entityGroup;
    const entityGroupId = !entityGroup.groupAll ? entityGroup.id.id : null;
    let customerId: CustomerId = null;
    if (entityGroup.ownerId.entityType === EntityType.CUSTOMER) {
      customerId = entityGroup.ownerId as CustomerId;
    }
    this.homeDialogs.importEntities(customerId, EntityType.ASSET, entityGroupId).subscribe((res) => {
      if (res) {
        this.broadcast.broadcast('assetSaved');
        config.updateData();
      }
    });
  }

  private openAsset($event: Event, asset: AssetInfo, config: GroupEntityTableConfig<AssetInfo>, params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      let url: UrlTree;
      if (params.groupType === EntityType.EDGE) {
        url = this.router.createUrlTree(['customerGroups', params.entityGroupId, params.customerId,
          'edgeGroups', params.childEntityGroupId, params.edgeId, 'assetGroups', params.edgeEntitiesGroupId, asset.id.id]);
      } else {
        url = this.router.createUrlTree(['customers', 'groups', params.entityGroupId,
          params.customerId, 'entities', 'assets', 'groups', params.childEntityGroupId, asset.id.id]);
      }
      this.window.open(window.location.origin + url, '_blank');
    } else {
      const url = this.router.createUrlTree([asset.id.id], {relativeTo: config.getActivatedRoute()});
      this.router.navigateByUrl(url);
    }
  }

  manageOwnerAndGroups($event: Event, asset: AssetInfo, config: GroupEntityTableConfig<AssetInfo>) {
    this.homeDialogs.manageOwnerAndGroups($event, asset).subscribe(
      (res) => {
        if (res) {
          config.updateData();
        }
      }
    );
  }

  onAssetAction(action: EntityAction<AssetInfo>, config: GroupEntityTableConfig<AssetInfo>, params: EntityGroupParams): boolean {
    switch (action.action) {
      case 'open':
        this.openAsset(action.event, action.entity, config, params);
        return true;
      case 'manageOwnerAndGroups':
        this.manageOwnerAndGroups(action.event, action.entity, config);
        return true;
    }
    return false;
  }

}
