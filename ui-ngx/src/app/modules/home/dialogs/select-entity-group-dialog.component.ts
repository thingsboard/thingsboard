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
import { EntityId } from '@shared/models/id/entity-id';
import { Observable } from 'rxjs';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { EntityType } from '@shared/models/entity-type.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation } from '@shared/models/security.models';
import { EntityGroup, EntityGroupInfo } from '@shared/models/entity-group.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface SelectEntityGroupDialogResult {
  groupId: string;
  group?: EntityGroupInfo;
  isNew: boolean;
}

export interface SelectEntityGroupDialogData {
  ownerId: EntityId;
  targetGroupType: EntityType;
  selectEntityGroupTitle: string;
  confirmSelectTitle: string;
  placeholderText: string;
  notFoundText: string;
  requiredText: string;
  excludeGroupIds: Array<string>;
  onEntityGroupSelected: (result: SelectEntityGroupDialogResult) => Observable<boolean>;
}

@Component({
    selector: 'tb-select-entity-group-dialog',
    templateUrl: './select-entity-group-dialog.component.html',
    providers: [{ provide: ErrorStateMatcher, useExisting: SelectEntityGroupDialogComponent }],
    styleUrls: ['./select-entity-group-dialog.component.scss'],
    standalone: false
})
export class SelectEntityGroupDialogComponent extends
  DialogComponent<SelectEntityGroupDialogComponent, SelectEntityGroupDialogResult> implements OnInit, ErrorStateMatcher {

  selectEntityGroupFormGroup: UntypedFormGroup;

  submitted = false;

  ownerId: EntityId;
  targetGroupType: EntityType;
  selectEntityGroupTitle: string;
  confirmSelectTitle: string;
  placeholderText: string;
  notFoundText: string;
  requiredText: string;
  excludeGroupIds: Array<string>;
  onEntityGroupSelected: (result: SelectEntityGroupDialogResult) => Observable<boolean>;

  createEnabled: boolean;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected userPermissionsService: UserPermissionsService,
              protected entityGroupService: EntityGroupService,
              @Inject(MAT_DIALOG_DATA) public data: SelectEntityGroupDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<SelectEntityGroupDialogComponent, SelectEntityGroupDialogResult>,
              public fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store, router, dialogRef);
    this.ownerId = data.ownerId;
    this.targetGroupType = data.targetGroupType;
    this.selectEntityGroupTitle = data.selectEntityGroupTitle;
    this.confirmSelectTitle = data.confirmSelectTitle;
    this.placeholderText = data.placeholderText;
    this.notFoundText = data.notFoundText;
    this.requiredText = data.requiredText;
    this.excludeGroupIds = data.excludeGroupIds;
    this.onEntityGroupSelected = data.onEntityGroupSelected;

    this.createEnabled = this.userPermissionsService.hasGenericEntityGroupTypePermission(Operation.CREATE, this.targetGroupType);
  }

  ngOnInit(): void {
    this.selectEntityGroupFormGroup = this.fb.group({
      addToGroupType: [0],
      targetEntityGroupId: [null, [Validators.required]],
      newEntityGroupName: [null, [Validators.required, Validators.maxLength(255)]]
    });
    this.updateDisabledState();
    if (this.createEnabled) {
      this.selectEntityGroupFormGroup.get('addToGroupType').valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(
        () => {
          this.updateDisabledState();
        }
      );
    }
  }

  private updateDisabledState() {
    if (!this.createEnabled) {
      this.selectEntityGroupFormGroup.get('newEntityGroupName').disable();
    } else {
      const addToGroupType: number = this.selectEntityGroupFormGroup.get('addToGroupType').value;
      if (addToGroupType === 0) {
        this.selectEntityGroupFormGroup.get('targetEntityGroupId').enable();
        this.selectEntityGroupFormGroup.get('newEntityGroupName').disable();
      } else {
        this.selectEntityGroupFormGroup.get('targetEntityGroupId').disable();
        this.selectEntityGroupFormGroup.get('newEntityGroupName').enable();
      }
    }
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  selectEntityGroup(): void {
    this.submitted = true;
    const addToGroupType: number = this.selectEntityGroupFormGroup.get('addToGroupType').value;
    if (addToGroupType === 1) {
      const newEntityGroupName: string = this.selectEntityGroupFormGroup.get('newEntityGroupName').value.trim();
      const newEntityGroup: EntityGroup = {
        name: newEntityGroupName,
        type: this.targetGroupType,
        ownerId: this.ownerId
      };
      this.entityGroupService.saveEntityGroup(newEntityGroup).subscribe((entityGroup) => {
        this.groupSelected({groupId: entityGroup.id.id, group: entityGroup, isNew: true});
      });
    } else {
      const targetEntityGroupId: string = this.selectEntityGroupFormGroup.get('targetEntityGroupId').value;
      this.groupSelected({groupId: targetEntityGroupId, isNew: false});
    }
  }

  private groupSelected(result: SelectEntityGroupDialogResult) {
    if (this.onEntityGroupSelected) {
      this.onEntityGroupSelected(result).subscribe((res) => {
        if (res) {
          this.dialogRef.close(result);
        }
      });
    } else {
      this.dialogRef.close(result);
    }
  }
}
