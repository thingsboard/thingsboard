// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  checkBoxCell,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { EntityGroup, EntityGroupInfo, EntityGroupParams, entityGroupsTitle } from '@shared/models/entity-group.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { UtilsService } from '@core/services/utils.service';
import { Router } from '@angular/router';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { isDefinedAndNotNull } from '@core/utils';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { Operation, publicGroupTypes, Resource, sharableGroupTypes } from '@shared/models/security.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { EntityGroupComponent } from '@home/components/group/entity-group.component';
import { EntityGroupTabsComponent } from '@home/components/group/entity-group-tabs.component';
import { MatDialog } from '@angular/material/dialog';
import { EntityGroupWizardDialogResult } from '@home/components/wizard/entity-group-wizard-dialog.component';
import { AddEntityGroupsToEdgeDialogComponent } from '@home/dialogs/add-entity-groups-to-edge-dialog.component';
import { AddEntityGroupsToEdgeDialogData } from '@home/dialogs/add-entity-groups-to-edge-dialog.models';
import { EntityId } from '@shared/models/id/entity-id';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import { AuthUser } from '@shared/models/user.model';
import { edgeCreationAllowed } from '@shared/models/edge.models';

export class EntityGroupsTableConfig extends EntityTableConfig<EntityGroupInfo> {

  edgeId: string;
  groupType: EntityType;
  shared: boolean;

  constructor(private entityGroupService: EntityGroupService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private utils: UtilsService,
              private router: Router,
              private dialog: MatDialog,
              private homeDialogs: HomeDialogsService,
              private customTranslate: CustomTranslatePipe,
              private authUser: AuthUser,
              private params: EntityGroupParams) {
    super(params);

    if (params.hierarchyView) {
      this.pageMode = false;
    }
    this.edgeId = params.edgeId;
    if ((this.customerId || this.edgeId) && params.edgeEntitiesType) {
      this.groupType = params.edgeEntitiesType;
    } else if ((this.customerId || this.edgeId) && params.childGroupType) {
      this.groupType = params.childGroupType;
    } else {
      this.groupType = params.groupType;
    }
    this.shared = params.shared;

    this.entityType = EntityType.ENTITY_GROUP;
    this.entityComponent = EntityGroupComponent;
    this.entityTabsComponent = EntityGroupTabsComponent;
    this.entityTranslations = entityTypeTranslations.get(EntityType.ENTITY_GROUP);
    this.entityResources = entityTypeResources.get(EntityType.ENTITY_GROUP);

    this.rowPointer = true;

    this.hideDetailsTabsOnEdit = false;

    this.entityTitle = (entityGroup) => entityGroup ?
      this.utils.customTranslation(entityGroup.name, entityGroup.name) : '';

    this.columns.push(
      new DateEntityTableColumn<EntityGroupInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<EntityGroupInfo>('name', 'entity-group.name', '33%', this.entityTitle),
      new EntityTableColumn<EntityGroupInfo>('description', 'entity-group.description', '40%',
        (entityGroup) => this.customTranslate.transform(entityGroup?.additionalInfo?.description || ''),
        () => ({}), false)
    );
    if (publicGroupTypes.has(this.groupType)) {
      this.columns.push(
        new EntityTableColumn<EntityGroupInfo>('isPublic', 'entity-group.public', '60px',
          entityGroup =>
            checkBoxCell(entityGroup && entityGroup.additionalInfo ?
              entityGroup.additionalInfo.isPublic : false), () => ({}), false)
      );
    }

    this.deleteEntityTitle = entityGroup =>
      this.translate.instant('entity-group.delete-entity-group-title', { entityGroupName: entityGroup.name });
    this.deleteEntityContent = () => this.translate.instant('entity-group.delete-entity-group-text');
    this.deleteEntitiesTitle = count => this.translate.instant('entity-group.delete-entity-groups-title', {count});
    this.deleteEntitiesContent = () => this.translate.instant('entity-group.delete-entity-groups-text');

    this.entitiesFetchFunction = pageLink => {
      if (this.customerId && !this.isEdgeGroup()) {
        return this.entityGroupService.getEntityGroupsByOwnerIdAndPageLink(EntityType.CUSTOMER, this.customerId, this.groupType, pageLink);
      } else if (this.isEdgeGroup()) {
        return this.entityGroupService.getEdgeEntityGroups(pageLink, this.edgeId, this.groupType);
      } else if (this.shared) {
        return this.entityGroupService.getSharedEntityGroups(pageLink, this.groupType);
      } else {
        return this.entityGroupService.getEntityGroups(pageLink, this.groupType, false);
      }
    };

    this.loadEntity = id => this.groupType === EntityType.DEVICE ? this.entityGroupService.getDeviceEntityGroup(id.id) :
      this.entityGroupService.getEntityGroup(id.id);

    this.saveEntity = (entityGroup, originalEntityGroup) => {
      entityGroup.type = this.groupType;
      if (this.customerId) {
        entityGroup.ownerId = {
          entityType: EntityType.CUSTOMER,
          id: this.customerId
        };
      }
      let saveEntity$: Observable<EntityGroupInfo>;
      if (entityGroup.type === EntityType.DEVICE) {
        saveEntity$ = this.entityGroupService.saveDeviceEntityGroup(entityGroup, originalEntityGroup);
      } else {
        saveEntity$ = this.entityGroupService.saveEntityGroup(entityGroup);
      }
      return saveEntity$.pipe(
        tap(() => {
            this.notifyEntityGroupUpdated();
          }
        ));
    };

    this.deleteEntity = id => this.entityGroupService.deleteEntityGroup(id.id).pipe(
        tap(() => {
            this.notifyEntityGroupUpdated();
          }
        ));

    this.onEntityAction = action => this.onEntityGroupAction(action);

    this.handleRowClick = ($event, entityGroup) => {
      if (this.isDetailsOpen()) {
        this.toggleEntityDetails($event, entityGroup);
      } else {
        this.open($event, entityGroup);
      }
      return true;
    };

    this.deleteEnabled = (entityGroup) => entityGroup && !this.shared && !entityGroup.groupAll &&
      this.userPermissionsService.hasEntityGroupPermission(Operation.DELETE, entityGroup);
    this.detailsReadonly = (entityGroup) =>
      this.shared || !this.userPermissionsService.hasEntityGroupPermission(Operation.WRITE, entityGroup);
    this.entitySelectionEnabled = (entityGroup) => entityGroup && !entityGroup.groupAll &&
      this.userPermissionsService.hasEntityGroupPermission(Operation.DELETE, entityGroup);

    if (!this.userPermissionsService.hasGenericEntityGroupTypePermission(Operation.CREATE, this.groupType) || this.shared) {
      this.addEnabled = false;
    }
    if (this.groupType === EntityType.EDGE && !edgeCreationAllowed(this.authUser)) {
      this.addEnabled = false;
    }
    if (!this.userPermissionsService.hasGenericEntityGroupTypePermission(Operation.DELETE, this.groupType) || this.shared) {
      this.entitiesDeleteEnabled = false;
    }
    this.componentsData = {
      isGroupEntitiesView: false,
      shared: this.shared
    };
    this.updateActionCellDescriptors();
    this.tableTitle = this.translate.instant(entityGroupsTitle(this.groupType, this.shared));
    if (sharableGroupTypes.has(this.groupType) &&
      this.userPermissionsService.hasGenericPermission(Resource.GROUP_PERMISSION, Operation.CREATE)) {
        this.addEntity = () => this.entityGroupWizard();
    }
    if (this.isEdgeGroup()) {
      this.deleteEnabled = () => false;
      this.entitiesDeleteEnabled = false;
      this.addEnabled = false;
      this.componentsData.isEdgeGroup = true;
      if (this.userPermissionsService.hasGenericPermission(Resource.EDGE, Operation.WRITE)) {
        this.entitySelectionEnabled = () => true;
        this.componentsData.isUnassignEnabled = true;
        this.groupActionDescriptors.push(
          {
            name: this.translate.instant('edge.unassign-entity-groups-from-edge'),
            icon: 'assignment_return',
            isEnabled: true,
            onAction: ($event, entities) => {
              this.unassignEntityGroupsFromEdge($event, entities);
            }
          }
        );
        if (this.userPermissionsService.hasGenericPermission(Resource.CUSTOMER, Operation.READ)) {
          this.headerActionDescriptors.push({
              name: this.translate.instant('edge.assign-to-edge'),
              icon: 'add',
              isEnabled: () => true,
              onAction: ($event) => {
                this.assignEntityGroupsToEdge($event);
              }
            }
          );
        }
      }
    }
  }

  private updateActionCellDescriptors() {
    this.cellActionDescriptors.splice(0);
    if (sharableGroupTypes.has(this.groupType) &&
      this.userPermissionsService.hasGenericPermission(Resource.GROUP_PERMISSION, Operation.CREATE)) {
      this.cellActionDescriptors.push(
        {
          name: this.translate.instant('action.share'),
          icon: 'assignment_ind',
          isEnabled: (entity) => entity && this.userPermissionsService.hasEntityGroupPermission(Operation.SHARE_GROUP, entity),
          onAction: ($event, entity) => this.share($event, entity)
        }
      );
    }
    if (publicGroupTypes.has(this.groupType) && !this.shared) {
      this.cellActionDescriptors.push(
        {
          name: this.translate.instant('action.make-public'),
          icon: 'share',
          isEnabled: (entity) => entity
            && (!entity.additionalInfo || !entity.additionalInfo.isPublic)
            && this.userPermissionsService.isDirectlyOwnedGroup(entity)
            && this.userPermissionsService.hasEntityGroupPermission(Operation.WRITE, entity),
          onAction: ($event, entity) => this.makePublic($event, entity)
        },
        {
          name: this.translate.instant('action.make-private'),
          icon: 'reply',
          isEnabled: (entity) => entity
            && entity.additionalInfo && entity.additionalInfo.isPublic
            && this.userPermissionsService.isDirectlyOwnedGroup(entity)
            && this.userPermissionsService.hasEntityGroupPermission(Operation.WRITE, entity),
          onAction: ($event, entity) => this.makePrivate($event, entity)
        }
      );
    }
    if (this.isEdgeGroup()) {
      this.cellActionDescriptors.push(
        {
          name: this.translate.instant('edge.unassign-entity-group-from-edge'),
          icon: 'assignment_return',
          isEnabled: () => this.userPermissionsService.hasGenericPermission(Resource.EDGE, Operation.WRITE),
          onAction: ($event, entity) => this.unassignEntityGroupFromEdge($event, entity)
        }
      );
    }
    this.cellActionDescriptors.push(
      {
        name: this.translate.instant('entity-group.entity-group-details'),
        icon: 'edit',
        isEnabled: () => true,
        onAction: ($event, entity) => this.toggleEntityDetails($event, entity)
      }
    );
  }

  private entityGroupWizard(): Observable<EntityGroupInfo> {
    let ownerId: EntityId = null;
    if (this.customerId) {
      ownerId = {
        entityType: EntityType.CUSTOMER,
        id: this.customerId
      };
    }
    return this.homeDialogs.createEntityGroup(this.groupType, '', ownerId).pipe(
      map((result) => {
        if (result) {
          this.notifyEntityGroupUpdated();
        }
        return result?.entityGroup;
      }
    ));
  }

  private assignEntityGroupsToEdge($event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    let ownerId = this.userPermissionsService.getUserOwnerId();
    if (this.params.customerId) {
      ownerId = {
        id: this.params.customerId,
        entityType: EntityType.CUSTOMER
      };
    }
    this.dialog.open<AddEntityGroupsToEdgeDialogComponent,
      AddEntityGroupsToEdgeDialogData,
      EntityGroupWizardDialogResult>(AddEntityGroupsToEdgeDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        ownerId,
        groupType: this.groupType,
        edgeId: this.params.edgeId,
        addEntityGroupsToEdgeTitle: 'edge.assign-to-edge-title',
        confirmSelectTitle: 'action.assign',
        notFoundText: 'entity-group.no-entity-groups-matching',
        requiredText: 'entity-group.target-entity-group-required'
      }
    }).afterClosed().subscribe(
      (result) => {
          if (result) {
            this.notifyEntityGroupUpdated();
            this.updateData();
          }
        }
    );
  }

  private share($event: Event, entityGroup: EntityGroupInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const isWriteAllowed = this.userPermissionsService.hasGroupEntityPermission(Operation.WRITE, entityGroup);
    this.homeDialogs.shareEntityGroup($event, entityGroup, isWriteAllowed).subscribe((res) => {
      if (res) {
        this.onGroupUpdated();
      }
    });
  }

  private makePublic($event: Event, entityGroup: EntityGroupInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.homeDialogs.makeEntityGroupPublic($event, entityGroup)
      .subscribe((res) => {
        if (res) {
          this.onGroupUpdated();
        }
      });
  }

  private makePrivate($event: Event, entityGroup: EntityGroupInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.homeDialogs.makeEntityGroupPrivate($event, entityGroup)
      .subscribe((res) => {
        if (res) {
          this.onGroupUpdated();
        }
      });
  }

  onGroupUpdated(closeDetails = false) {
    this.notifyEntityGroupUpdated();
    if (this.componentsData.isGroupEntitiesView) {
      this.componentsData.reloadEntityGroup();
    } else {
      this.updateData(closeDetails);
    }
  }

  private notifyEntityGroupUpdated() {
    if (!this.componentsData.isGroupEntitiesView && this.params.hierarchyView) {
      this.params.hierarchyCallbacks.refreshEntityGroups(this.params.internalId);
    }
  }

  private open($event: Event, entityGroup: EntityGroupInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.params.hierarchyView) {
      this.params.hierarchyCallbacks.groupSelected(this.params.nodeId, entityGroup.id.id);
    } else {
      const url = this.router.createUrlTree([entityGroup.id.id], {relativeTo: this.getActivatedRoute()});
      this.router.navigateByUrl(url);
    }
  }

  private unassignEntityGroupFromEdge($event: Event, entityGroup: EntityGroup) {
    if ($event) {
      $event.stopPropagation();
    }
    this.homeDialogs.unassignEntityGroupFromEdge($event, entityGroup, this.edgeId).subscribe(
      (res) => {
        if (res) {
          this.onGroupUpdated(true);
        }
      }
    );
  }

  private unassignEntityGroupsFromEdge($event: Event, entityGroups: Array<EntityGroup>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.homeDialogs.unassignEntityGroupsFromEdge($event, entityGroups, this.edgeId).subscribe(
      (res) => {
        if (res) {
          this.onGroupUpdated();
        }
      }
    );
  }

  private onEntityGroupAction(action: EntityAction<EntityGroupInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.open(action.event, action.entity);
        return true;
      case 'share':
        this.share(action.event, action.entity);
        return true;
      case 'makePublic':
        this.makePublic(action.event, action.entity);
        return true;
      case 'makePrivate':
        this.makePrivate(action.event, action.entity);
        return true;
      case 'unassign':
        this.unassignEntityGroupFromEdge(action.event, action.entity);
        return true;
    }
    return false;
  }

  private isEdgeGroup(): boolean {
    return isDefinedAndNotNull(this.params.edgeId) && (this.params.groupType === EntityType.EDGE ||
      (this.params.groupType === EntityType.CUSTOMER && this.params.childGroupType === EntityType.EDGE));
  }
}
