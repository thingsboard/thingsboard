// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
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
import { forkJoin, Observable } from 'rxjs';

export interface ManageOwnerAndGroupsDialogData {
  groupEntity: GroupEntityInfo<EntityId>;
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

  groupEntity: GroupEntityInfo<EntityId>;
  entityType: EntityType;
  readonly = false;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ManageOwnerAndGroupsDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<ManageOwnerAndGroupsDialogComponent, boolean>,
              public fb: UntypedFormBuilder,
              private userPermissionsService: UserPermissionsService,
              private entityGroupService: EntityGroupService) {
    super(store, router, dialogRef);
    this.groupEntity = data.groupEntity;
    this.entityType = this.groupEntity.id.entityType as EntityType;
    if (!this.userPermissionsService.hasGenericPermissionByEntityGroupType(Operation.CHANGE_OWNER, this.entityType) &&
        !(this.userPermissionsService.hasGenericEntityGroupTypePermission(Operation.ADD_TO_GROUP, this.entityType) ||
          this.userPermissionsService.hasGenericEntityGroupTypePermission(Operation.REMOVE_FROM_GROUP, this.entityType))) {
      this.readonly = true;
    }
  }

  ngOnInit(): void {
    const ownerAndGroups: OwnerAndGroupsData = {
      owner: this.groupEntity.ownerName ? {
        id: this.groupEntity.ownerId,
        name: this.groupEntity.ownerName
      } : this.groupEntity.ownerId,
      groups: this.groupEntity.groups
    };
    this.ownerAndGroupsFormGroup = this.fb.group({
      ownerAndGroups: [ownerAndGroups, [Validators.required]]
    });
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
    const targetOwner = targetOwnerAndGroups.owner;
    let targetOwnerId: EntityId;
    if ((targetOwner as EntityInfoData).name) {
      targetOwnerId = (targetOwner as EntityInfoData).id;
    } else {
      targetOwnerId = targetOwner as EntityId;
    }
    if (entityIdEquals(targetOwnerId, this.groupEntity.ownerId)) {
      const existingGroupIds = this.groupEntity.groups.map(group => group.id);
      const newGroupIds = targetOwnerAndGroups.groups.map(group => group.id);
      if (!entityIdsEquals(existingGroupIds, newGroupIds)) {
        const addToGroupIds = newGroupIds.filter(groupId => !entityIdsContains(existingGroupIds, groupId));
        const removeFromGroupIds = existingGroupIds.filter(groupId => !entityIdsContains(newGroupIds, groupId));
        const tasks: Observable<any>[] = [];
        for (const groupId of addToGroupIds) {
          tasks.push(this.entityGroupService.addEntityToEntityGroup(groupId.id, this.groupEntity.id.id));
        }
        for (const groupId of removeFromGroupIds) {
          tasks.push(this.entityGroupService.removeEntityFromEntityGroup(groupId.id, this.groupEntity.id.id));
        }
        forkJoin(tasks).subscribe(() => {
          this.dialogRef.close(true);
        });
      } else {
        this.dialogRef.close(false);
      }
    } else {
      let entityGroupIds: string[] = null;
      if (targetOwnerAndGroups.groups?.length) {
        entityGroupIds = targetOwnerAndGroups.groups.map(group => group.id.id);
      }
      this.entityGroupService.changeEntityOwner(targetOwnerId, this.groupEntity.id, entityGroupIds).subscribe(() => {
        this.dialogRef.close(true);
      });
    }
  }
}
