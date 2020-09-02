///
/// Copyright © 2016-2020 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { TenantProfileData } from '@shared/models/tenant.model';

@Component({
  selector: 'tb-tenant-profile-data',
  templateUrl: './tenant-profile-data.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => TenantProfileDataComponent),
    multi: true
  }]
})
export class TenantProfileDataComponent implements ControlValueAccessor, OnInit {

  tenantProfileDataFormGroup: FormGroup;

  modelValue: TenantProfileData | null;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: FormBuilder) {
    this.tenantProfileDataFormGroup = this.fb.group({
      tenantProfileData: [null, Validators.required]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.tenantProfileDataFormGroup.get('tenantProfileData').valueChanges.subscribe(
      tenantProfileData => {
        this.updateView(this.tenantProfileDataFormGroup.valid ? tenantProfileData : null);
      }
    );
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: TenantProfileData | null): void {
    this.modelValue = value;
    this.tenantProfileDataFormGroup.get('tenantProfileData').patchValue(value, {emitEvent: false});
  }

  updateView(value: TenantProfileData | null) {
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }
}
