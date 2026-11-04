// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { booleanAttribute, Component, Input, ViewEncapsulation } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { GroupPermission } from '@shared/models/group-permission.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RoleType, roleTypeTranslationMap } from '@shared/models/security.models';
import { isDefinedAndNotNull } from '@core/utils';
import { RoleId } from '@shared/models/id/role-id';
import { EntityGroupId } from '@shared/models/id/entity-group-id';
import { EntityType } from '@shared/models/entity-type.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Component({
    selector: 'tb-user-groups-panel-row',
    templateUrl: './user-groups-panel-row.component.html',
    styleUrls: ['./user-groups-panel-row.component.scss'],
    encapsulation: ViewEncapsulation.None,
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: UserGroupsPanelRowComponent,
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: UserGroupsPanelRowComponent,
            multi: true
        }
    ],
    standalone: false
})
export class UserGroupsPanelRowComponent implements ControlValueAccessor, Validator {

  @Input({transform: booleanAttribute})
  disabled: boolean;

  groupPermissionForm = this.fb.group({
    roleId: this.fb.control<RoleId>(null, Validators.required),
    entityGroupId: this.fb.control<EntityGroupId>(null, Validators.required)
  });
  groupType = this.fb.control(RoleType.GENERIC);

  readonly entityType = EntityType;
  readonly roleType = RoleType;
  readonly roleTypes = Object.keys(RoleType) as RoleType[];
  readonly roleTypeTranslations = roleTypeTranslationMap;

  entityGroupOwnerId = this.userPermissionsService.getUserOwnerId()

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder,
              private userPermissionsService: UserPermissionsService) {
    this.groupPermissionForm.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((value) => {
      this.propagateChange(value);
    });

    this.groupType.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      if (value === RoleType.GENERIC) {
        this.groupPermissionForm.get('entityGroupId').disable({emitEvent: false});
      } else {
        this.groupPermissionForm.get('entityGroupId').enable({emitEvent: false});
      }
    })
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    if (isDisabled) {
      this.groupPermissionForm.disable({emitEvent: false});
      this.groupType.disable({emitEvent: false});
    } else {
      this.groupPermissionForm.enable({emitEvent: false});
      this.groupType.enable();
      if(isDefinedAndNotNull(this.disabled)) {
        setTimeout(() => {
          this.groupPermissionForm.updateValueAndValidity();
        }, 0);
      }
    }
    this.disabled = isDisabled;
  }

  validate(): ValidationErrors | null {
    if (!this.groupPermissionForm.valid) {
      return {
        invalidGroupPermissionForm: true
      };
    }
    return null;
  }

  writeValue(permission: GroupPermission) {
    if (isDefinedAndNotNull(permission.entityGroupId)) {
      this.groupType.setValue(RoleType.GROUP);
    } else {
      this.groupType.setValue(RoleType.GENERIC);
    }
    this.groupPermissionForm.patchValue(permission, {emitEvent: false})
  }
}
