// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { ShareGroupRequest } from '@shared/models/entity-group.models';
import { EntityType } from '@shared/models/entity-type.models';
import { RoleId } from '@shared/models/id/role-id';
import { RoleType } from '@shared/models/security.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-share-entity-group',
    templateUrl: './share-entity-group.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ShareEntityGroupComponent),
            multi: true
        }],
    standalone: false
})
export class ShareEntityGroupComponent implements ControlValueAccessor, OnInit {

  @Input()
  @coerceBoolean()
  isWriteAllowed = true;

  entityType = EntityType;

  roleType = RoleType;

  shareEntityGroupFormGroup: UntypedFormGroup;

  @Input()
  disabled: boolean;

  private shareGroupRequest: ShareGroupRequest = null;
  private propagateChange = null;
  private propagateChangePending = false;

  constructor(private store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.propagateChangePending) {
      this.propagateChangePending = false;
      setTimeout(() => {
        this.propagateChange(this.shareGroupRequest);
      }, 0);
    }
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.shareEntityGroupFormGroup = this.fb.group({
      ownerId: [null, Validators.required],
      allUserGroup: [null],
      userGroupId: [null],
      permissionType: [null],
      roleIds: [null]
    });
    this.shareEntityGroupFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.shareEntityGroupFormGroup.get('allUserGroup').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
    this.shareEntityGroupFormGroup.get('permissionType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.shareEntityGroupFormGroup.disable({emitEvent: false});
    } else {
      this.shareEntityGroupFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: ShareGroupRequest | null): void {
    if (value) {
      const roleIds = value.roleIds ? value.roleIds.map(id => id.id) : [];
      (value as any).roleIds = roleIds;
      this.shareEntityGroupFormGroup.patchValue(value, {emitEvent: false});
      const permissionType = value.roleIds && value.roleIds.length ? 2 : (value.readElseWrite ? 0 : 1);
      this.shareEntityGroupFormGroup.get('permissionType').patchValue(permissionType, {emitEvent: false});
    } else {
      this.shareEntityGroupFormGroup.patchValue({}, {emitEvent: false});
    }
    this.updateValidators();
    this.updateModel();
  }

  private updateValidators() {
    const allUserGroup: boolean = this.shareEntityGroupFormGroup.get('allUserGroup').value;
    if (allUserGroup) {
      this.shareEntityGroupFormGroup.get('userGroupId').clearValidators();
    } else {
      this.shareEntityGroupFormGroup.get('userGroupId').setValidators(Validators.required);
    }
    const permissionType: number = this.shareEntityGroupFormGroup.get('permissionType').value;
    if (permissionType === 2) {
      this.shareEntityGroupFormGroup.get('roleIds').setValidators(Validators.required);
    } else {
      this.shareEntityGroupFormGroup.get('roleIds').clearValidators();
    }
    this.shareEntityGroupFormGroup.get('userGroupId').updateValueAndValidity({emitEvent: false});
    this.shareEntityGroupFormGroup.get('roleIds').updateValueAndValidity({emitEvent: false});
  }

  private updateModel() {
    this.shareGroupRequest = null;
    if (this.shareEntityGroupFormGroup.valid) {
      this.shareGroupRequest = this.shareEntityGroupFormGroup.getRawValue();
      const permissionType = this.shareEntityGroupFormGroup.get('permissionType').value;
      this.shareGroupRequest.readElseWrite = permissionType === 0;
      delete (this.shareGroupRequest as any).permissionType;
      const roleIds: string[] = this.shareEntityGroupFormGroup.get('roleIds').value;
      if (roleIds && roleIds.length) {
        this.shareGroupRequest.roleIds = roleIds.map(id => new RoleId(id));
      }
    }
    if (this.propagateChange) {
      this.propagateChange(this.shareGroupRequest);
    } else {
      this.propagateChangePending = true;
    }
  }
}
