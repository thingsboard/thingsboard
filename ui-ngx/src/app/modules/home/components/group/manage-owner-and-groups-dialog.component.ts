// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  FormGroupDirective,
  NgForm,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { EntityId, entityIdEquals, entityIdsContains, entityIdsEquals } from '@shared/models/id/entity-id';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { GroupEntityInfo } from '@shared/models/base-data';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation } from '@shared/models/security.models';
import { EntityType } from '@shared/models/entity-type.models';
import { OwnerAndGroupsData } from '@home/components/group/owner-and-groups.component';
import { EntityInfoData } from '@shared/models/entity.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { from, Observable, of } from 'rxjs';
import { catchError, mergeAll, toArray } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

const REQUEST_CONCURRENCY = 5;

export interface ManageOwnerAndGroupsDialogData {
  groupEntities: GroupEntityInfo<EntityId> | GroupEntityInfo<EntityId>[];
}

@Component({
    selector: 'tb-manage-owner-and-groups-dialog',
    templateUrl: './manage-owner-and-groups-dialog.component.html',
    providers: [{ provide: ErrorStateMatcher, useExisting: ManageOwnerAndGroupsDialogComponent }],
    styleUrls: [],
    standalone: false
})
export class ManageOwnerAndGroupsDialogComponent extends
  DialogComponent<ManageOwnerAndGroupsDialogComponent, boolean> implements OnInit, ErrorStateMatcher {

  ownerAndGroupsFormGroup: UntypedFormGroup;

  entities: GroupEntityInfo<EntityId>[];
  entityType: EntityType;
  readonly = false;
  mixedSelection = false;
  multiSelection = false;

  submitted = false;

  private groupsInitiallyUnknown = false;
  private groupsTouchedByUser = false;
  private awaitingGroupsReset = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ManageOwnerAndGroupsDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<ManageOwnerAndGroupsDialogComponent, boolean>,
              public fb: UntypedFormBuilder,
              private userPermissionsService: UserPermissionsService,
              private entityGroupService: EntityGroupService,
              private destroyRef: DestroyRef) {
    super(store, router, dialogRef);
    this.entities = Array.isArray(data.groupEntities) ? data.groupEntities : [data.groupEntities];
    this.entityType = this.entities[0].id.entityType as EntityType;
    if (!this.userPermissionsService.hasGenericPermissionByEntityGroupType(Operation.CHANGE_OWNER, this.entityType) &&
        !(this.userPermissionsService.hasGenericEntityGroupTypePermission(Operation.ADD_TO_GROUP, this.entityType) ||
          this.userPermissionsService.hasGenericEntityGroupTypePermission(Operation.REMOVE_FROM_GROUP, this.entityType))) {
      this.readonly = true;
    }
  }

  ngOnInit(): void {
    const firstEntity = this.entities[0];
    const isSameOwner = this.allSameOwner(this.entities);
    const isSameGroups = this.allSameGroups(this.entities);
    this.multiSelection = this.entities.length > 1;
    this.mixedSelection = this.multiSelection && (!isSameOwner || !isSameGroups);
    this.groupsInitiallyUnknown = this.multiSelection && !isSameGroups;

    const ownerAndGroups: OwnerAndGroupsData = {
      owner: isSameOwner ? this.toOwner(firstEntity) : null,
      groups: isSameGroups ? firstEntity.groups : null,
    };

    this.ownerAndGroupsFormGroup = this.fb.group({
      ownerAndGroups: [ownerAndGroups, [Validators.required]]
    });

    let previousValue = ownerAndGroups;
    this.ownerAndGroupsFormGroup.get('ownerAndGroups').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value: OwnerAndGroupsData) => {
      const currentValue: OwnerAndGroupsData = value ?? {owner: null, groups: null};
      const ownerChanged = !entityIdEquals(this.resolveOwnerId(currentValue.owner), this.resolveOwnerId(previousValue.owner));
      if (ownerChanged) {
        this.awaitingGroupsReset = true;
        this.groupsTouchedByUser = false;
      } else if (this.awaitingGroupsReset) {
        this.awaitingGroupsReset = false;
      } else if (!entityIdsEquals((currentValue.groups ?? []).map(group => group.id),
                                  (previousValue.groups ?? []).map(group => group.id))) {
        this.groupsTouchedByUser = true;
      }
      previousValue = currentValue;
    });
  }

  private toOwner(entity: GroupEntityInfo<EntityId>): EntityInfoData | EntityId {
    return entity.ownerName ? { id: entity.ownerId, name: entity.ownerName } : entity.ownerId;
  }

  private resolveOwnerId(owner: EntityInfoData | EntityId | null): EntityId | null {
    if (!owner) {
      return null;
    }
    return (owner as EntityInfoData).name ? (owner as EntityInfoData).id : owner as EntityId;
  }

  private allSameOwner(entities: GroupEntityInfo<EntityId>[]): boolean {
    const firstOwnerId = entities[0].ownerId?.id;
    return entities.every(entity => entity.ownerId?.id === firstOwnerId);
  }

  private allSameGroups(entities: GroupEntityInfo<EntityId>[]): boolean {
    const firstGroupIds = (entities[0].groups ?? []).map(group => group.id);
    return entities.every(entity => entityIdsEquals((entity.groups ?? []).map(group => group.id), firstGroupIds));
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  update(): void {
    this.submitted = true;
    const targetOwnerAndGroups: OwnerAndGroupsData = this.ownerAndGroupsFormGroup.get('ownerAndGroups').value;
    const targetOwnerId = this.resolveOwnerId(targetOwnerAndGroups.owner);
    const targetGroupIds = targetOwnerAndGroups.groups?.map(group => group.id) ?? null;
    const ignoreErrors = this.entities.length > 1;
    const tasks = this.buildTasks(targetOwnerId, targetGroupIds, ignoreErrors);
    if (!tasks.length) {
      this.dialogRef.close(false);
      return;
    }
    const guardedTasks = ignoreErrors ? tasks.map(task => task.pipe(catchError(() => of(null)))) : tasks;
    from(guardedTasks).pipe(
      mergeAll(REQUEST_CONCURRENCY),
      toArray()
    ).subscribe({
      next: () => this.dialogRef.close(true),
      error: () => this.dialogRef.close(false)
    });
  }

  private buildTasks(targetOwnerId: EntityId, targetGroupIds: EntityId[] | null, ignoreErrors: boolean): Observable<unknown>[] {
    const tasks: Observable<unknown>[] = [];
    const addEntityIdsByGroup = new Map<string, { groupId: EntityId; entityIds: string[] }>();
    const removeEntityIdsByGroup = new Map<string, { groupId: EntityId; entityIds: string[] }>();

    for (const entity of this.entities) {
      if (entityIdEquals(targetOwnerId, entity.ownerId)) {
        if (this.groupsInitiallyUnknown && !this.groupsTouchedByUser) {
          continue;
        }
        const existingGroupIds = (entity.groups ?? []).map(group => group.id);
        const newGroupIds = targetGroupIds ?? [];
        if (entityIdsEquals(existingGroupIds, newGroupIds)) {
          continue;
        }
        const addToGroupIds = newGroupIds.filter(groupId => !entityIdsContains(existingGroupIds, groupId));
        const removeFromGroupIds = existingGroupIds.filter(groupId => !entityIdsContains(newGroupIds, groupId));
        addToGroupIds.forEach(groupId => this.addEntityIdToGroupBucket(addEntityIdsByGroup, groupId, entity.id.id));
        removeFromGroupIds.forEach(groupId => this.addEntityIdToGroupBucket(removeEntityIdsByGroup, groupId, entity.id.id));
      } else {
        const entityGroupIds = targetGroupIds?.length ? targetGroupIds.map(groupId => groupId.id) : null;
        tasks.push(this.entityGroupService.changeEntityOwner(targetOwnerId, entity.id, entityGroupIds, {ignoreErrors}));
      }
    }
    addEntityIdsByGroup.forEach(({groupId, entityIds}) =>
      tasks.push(this.entityGroupService.addEntitiesToEntityGroup(groupId.id, entityIds, {ignoreErrors})));
    removeEntityIdsByGroup.forEach(({groupId, entityIds}) =>
      tasks.push(this.entityGroupService.removeEntitiesFromEntityGroup(groupId.id, entityIds, {ignoreErrors})));
    return tasks;
  }

  private addEntityIdToGroupBucket(bucket: Map<string, { groupId: EntityId; entityIds: string[] }>,
                                    groupId: EntityId, entityId: string): void {
    const bucketEntry = bucket.get(groupId.id);
    if (bucketEntry) {
      bucketEntry.entityIds.push(entityId);
    } else {
      bucket.set(groupId.id, {groupId, entityIds: [entityId]});
    }
  }
}
