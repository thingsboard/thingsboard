// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { GroupPermission } from '@shared/models/group-permission.models';

@Component({
    selector: 'tb-registration-permissions',
    templateUrl: './registration-permissions.component.html',
    styleUrls: ['./registration-permissions.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => RegistrationPermissionsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class RegistrationPermissionsComponent extends PageComponent implements ControlValueAccessor, OnInit {

  @Input() disabled: boolean;

  registrationPermissions: Array<GroupPermission>;

  private propagateChange = null;

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(permissions: Array<GroupPermission>): void {
    this.registrationPermissions = permissions || [];
  }

  registrationPermissionsChanged() {
    this.propagateChange(this.registrationPermissions);
  }

}
