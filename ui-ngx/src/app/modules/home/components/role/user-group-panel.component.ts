// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { booleanAttribute, Component, Input } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { GroupPermission } from '@shared/models/group-permission.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-user-group-panel',
    templateUrl: './user-group-panel.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: UserGroupPanelComponent,
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: UserGroupPanelComponent,
            multi: true
        }
    ],
    standalone: false
})
export class UserGroupPanelComponent implements ControlValueAccessor, Validator {

  @Input({transform: booleanAttribute})
  disabled: boolean;

  groupPermissionsForm = this.fb.array<GroupPermission>([]);

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder) {
    this.groupPermissionsForm.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      this.propagateChange(value);
    })
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.groupPermissionsForm.disable({emitEvent: false});
    } else {
      this.groupPermissionsForm.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    if (this.groupPermissionsForm.status !== 'DISABLED' && !this.groupPermissionsForm.valid) {
      return {
        invalidGroupPermissionsForm: true
      };
    }
    return null;
  }

  writeValue(groupPermissions: GroupPermission[]) {
    if (this.groupPermissionsForm.length === groupPermissions?.length) {
      this.groupPermissionsForm.patchValue(groupPermissions, {emitEvent: false});
    } else {
      this.groupPermissionsForm.clear({emitEvent: false});
      groupPermissions.forEach(item => {
        this.groupPermissionsForm.push(this.fb.control(item), {emitEvent: false})
      })
    }
  }

  addPermission($event: Event) {
    $event?.stopPropagation();
    this.groupPermissionsForm.push(this.fb.control({
      roleId: null
    }), {emitEvent: false});
  }

  removePermission(index: number) {
    this.groupPermissionsForm.removeAt(index);
  }
}
