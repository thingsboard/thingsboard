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
import { filter, mergeMap, tap } from 'rxjs/operators';
import { BroadcastService } from '@core/services/broadcast.service';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { MatDialog } from '@angular/material/dialog';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityGroupParams } from '@shared/models/entity-group.models';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { CustomerId } from '@shared/models/id/customer-id';
import { GroupConfigTableConfigService } from '@home/components/group/group-config-table-config.service';
import { Operation } from '@shared/models/security.models';
import { edgeCreationAllowed, EdgeInfo } from '@shared/models/edge.models';
import { EdgeService } from '@core/http/edge.service';
import { EdgeProfileService } from '@core/http/edge-profile.service';
import { DialogService } from '@core/services/dialog.service';
import { confirmEdgeProfileChange } from '@home/pages/edge/edge-profile-change';
import { EdgeComponent } from '@home/pages/edge/edge.component';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AuthUser } from '@shared/models/user.model';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { WINDOW } from '@core/services/window.service';
import {
  EdgeInstructionsDialogComponent,
  EdgeInstructionsDialogData
} from '@home/pages/edge/edge-instructions-dialog.component';

@Injectable()
export class EdgeGroupConfigFactory implements EntityGroupStateConfigFactory<EdgeInfo> {

  constructor(private groupConfigTableConfigService: GroupConfigTableConfigService<EdgeInfo>,
              private userPermissionsService: UserPermissionsService,
              private store: Store<AppState>,
              private translate: TranslateService,
              private utils: UtilsService,
              private dialog: MatDialog,
              private homeDialogs: HomeDialogsService,
              private edgeService: EdgeService,
              private edgeProfileService: EdgeProfileService,
              private dialogService: DialogService,
              private broadcast: BroadcastService,
              private router: Router,
              @Inject(WINDOW) private window: Window) {
  }

  createConfig(params: EntityGroupParams, entityGroup: EntityGroupStateInfo<EdgeInfo>): Observable<GroupEntityTableConfig<EdgeInfo>> {
    const config = new GroupEntityTableConfig<EdgeInfo>(entityGroup, params);

    const authUser: AuthUser = getCurrentAuthUser(this.store);

    config.entityComponent = EdgeComponent;
    config.addDialogStyle = {height: '1000px'};

    config.entityTitle = (edge) => edge ?
      this.utils.customTranslation(edge.name, edge.name) : '';

    config.deleteEntityTitle = edge => this.translate.instant('edge.delete-edge-title', { edgeName: edge.name });
    config.deleteEntityContent = () => this.translate.instant('edge.delete-edge-text');
    config.deleteEntitiesTitle = count => this.translate.instant('edge.delete-edges-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('edge.delete-edges-text');

    config.loadEntity = id => this.edgeService.getEdgeInfo(id.id);
    config.saveEntity = (edge, originalEdge) => confirmEdgeProfileChange(edge, originalEdge, this.edgeProfileService,
      this.dialogService, this.translate).pipe(
      filter(Boolean),
      mergeMap(() => this.edgeService.saveEdge(edge)),
        tap(() => {
          this.broadcast.broadcast('edgeSaved');
        }),
      mergeMap((savedEdge) => this.edgeService.getEdgeInfo(savedEdge.id.id)
      ));
    config.deleteEntity = id => this.edgeService.deleteEdge(id.id);

    config.onEntityAction = action => this.onEdgeAction(action, config, params);

    if (params.hierarchyView) {
      config.entityAdded = edge => {
        params.hierarchyCallbacks.edgeAdded(params.nodeId, edge);
      };
      config.entityUpdated = edge => {
        params.hierarchyCallbacks.edgeUpdated(edge);
      };
      config.entitiesDeleted = edgeIds => {
        params.hierarchyCallbacks.edgesDeleted(edgeIds.map(id => id.id));
      };
    }

    if (edgeCreationAllowed(authUser) && this.userPermissionsService.hasGroupEntityPermission(Operation.CREATE, config.entityGroup)) {
      config.headerActionDescriptors.push(
        {
          name: this.translate.instant('edge.import'),
          icon: 'file_upload',
          isEnabled: () => true,
          onAction: ($event) => this.importEdges($event, config)
        }
      );
    }

    return this.groupConfigTableConfigService.prepareConfiguration(params, config).pipe(
      tap((preparedConfig) => {
        if (!edgeCreationAllowed(authUser)) {
          preparedConfig.addEnabled = false;
        }
      })
    );
  }

  importEdges($event: Event, config: GroupEntityTableConfig<EdgeInfo>) {
    const entityGroup = config.entityGroup;
    const entityGroupId = !entityGroup.groupAll ? entityGroup.id.id : null;
    let customerId: CustomerId = null;
    if (entityGroup.ownerId.entityType === EntityType.CUSTOMER) {
      customerId = entityGroup.ownerId as CustomerId;
    }
    this.homeDialogs.importEntities(customerId, EntityType.EDGE, entityGroupId).subscribe((res) => {
      if (res) {
        this.broadcast.broadcast('edgeSaved');
        config.updateData();
      }
    });
  }

  onEdgeAction(action: EntityAction<EdgeInfo>, config: GroupEntityTableConfig<EdgeInfo>, params: EntityGroupParams): boolean {
    switch (action.action) {
      case 'open':
        this.openEdge(action.event, action.entity, config, params);
        return true;
      case 'syncEdge':
        this.syncEdge(action.event, action.entity);
        return true;
      case 'openInstallInstructions':
        this.openInstructions(action.event, action.entity);
        return true;
      case 'openUpgradeInstructions':
        this.openInstructions(action.event, action.entity, true);
        return true;
      case 'manageOwnerAndGroups':
        this.manageOwnerAndGroups(action.event, action.entity, config);
        return true;
    }
    return false;
  }

  private openEdge($event: Event, edge: EdgeInfo,  config: GroupEntityTableConfig<EdgeInfo>, params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      const url = this.router.createUrlTree(['customers', 'groups', params.entityGroupId,
          params.customerId, 'edgeManagement', 'edges', 'groups', params.childEntityGroupId, edge.id.id]);
      this.window.open(window.location.origin + url, '_blank');
    } else {
      const url = this.router.createUrlTree([edge.id.id], {relativeTo: config.getActivatedRoute()});
      this.router.navigateByUrl(url);
    }
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

  openInstructions($event: Event, edge: EdgeInfo, upgradeAvailable: boolean = false) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<EdgeInstructionsDialogComponent, EdgeInstructionsDialogData>
    (EdgeInstructionsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        edge,
        afterAdd: false,
        upgradeAvailable
      }
    }).afterClosed().subscribe();
  }

  manageOwnerAndGroups($event: Event, edge: EdgeInfo, config: GroupEntityTableConfig<EdgeInfo>) {
    this.homeDialogs.manageOwnerAndGroups($event, edge).subscribe(
      (res) => {
        if (res) {
          config.updateData();
        }
      }
    );
  }

}
