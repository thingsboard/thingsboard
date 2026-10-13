// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { EntityType } from '@shared/models/entity-type.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { TranslateService } from '@ngx-translate/core';
import { EntityInfoData } from '@shared/models/entity.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation } from '@shared/models/security.models';
import { EntityId } from '@app/shared/models/id/entity-id';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { CreateEntityGroupFunction } from '@shared/components/group/entity-group-list.component';
import { map } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatFormFieldAppearance } from '@angular/material/form-field';

export interface OwnerAndGroupsData {
  owner?: EntityId | EntityInfoData;
  groups?: EntityInfoData[];
}

@Component({
    selector: 'tb-owner-and-groups',
    templateUrl: './owner-and-groups.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => OwnerAndGroupsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class OwnerAndGroupsComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  entityType: EntityType;

  @Input()
  defaultOwnerId: EntityId | null;

  @Input()
  skipDefaultPermissionCheck = false;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  ownerAndGroupsFormGroup: UntypedFormGroup;

  modelValue: OwnerAndGroupsData | null;

  currentUser = getCurrentAuthUser(this.store);

  private propagateChange = (v: any) => { };

  private ownerDisabled = false;
  private groupsDisabled = false;

  createGroupFunction: CreateEntityGroupFunction;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: UntypedFormBuilder,
              private userPermissionsService: UserPermissionsService,
              private homeDialogs: HomeDialogsService,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    if (!this.skipDefaultPermissionCheck) {
      this.ownerDisabled = !this.userPermissionsService.hasGenericPermissionByEntityGroupType(Operation.CHANGE_OWNER, this.entityType);
      this.groupsDisabled = !this.userPermissionsService.hasGenericEntityGroupTypePermission(Operation.ADD_TO_GROUP, this.entityType) ||
                            !this.userPermissionsService.hasGenericEntityGroupTypePermission(Operation.REMOVE_FROM_GROUP, this.entityType);
    }
    if (this.userPermissionsService.hasGenericEntityGroupTypePermission(Operation.CREATE, this.entityType)) {
      this.createGroupFunction = (groupType, groupName, ownerId) =>
        this.homeDialogs.createEntityGroup(groupType, groupName, ownerId).pipe(
        map((result) => {
          if (result && result.entityGroup) {
            return {
              id: result.entityGroup.id,
              name: result.entityGroup.name
            };
          } else {
            return null;
          }
        }
      ));
    }

    this.ownerAndGroupsFormGroup = this.fb.group({
      owner: this.fb.control({value: null,
        disabled: this.ownerDisabled},
        [Validators.required]),
      groups: this.fb.control({value: null,
        disabled: this.groupsDisabled
      }, [])
    });
    this.ownerAndGroupsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value: OwnerAndGroupsData) => {
      if (!value.owner) {
        this.ownerAndGroupsFormGroup.get('groups').disable({emitEvent: false});
      } else if (!this.groupsDisabled) {
        this.ownerAndGroupsFormGroup.get('groups').enable({emitEvent: false});
      }
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.ownerAndGroupsFormGroup.disable({emitEvent: false});
    } else {
      this.ownerAndGroupsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: OwnerAndGroupsData | undefined): void {
    this.modelValue = value;
    this.ownerAndGroupsFormGroup.patchValue(value || {}, {emitEvent: false});
  }

  ownerId(): EntityId | null {
    const ownerValue: EntityId | EntityInfoData = this.ownerAndGroupsFormGroup.get('owner').value;
    if (ownerValue) {
      return (ownerValue as EntityInfoData).name ? (ownerValue as EntityInfoData).id : ownerValue as EntityId;
    } else {
      return null;
    }
  }

  private updateModel() {
    this.modelValue = this.ownerAndGroupsFormGroup.value || {};
    if (this.ownerAndGroupsFormGroup.valid) {
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

}
