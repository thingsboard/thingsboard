///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import {Component, forwardRef, Input} from '@angular/core';
import {ControlValueAccessor, FormArray, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators} from '@angular/forms';
import {ResourceLwM2M} from '@home/components/profile/device/lwm2m/lwm2m-profile-config.models';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import _ from 'lodash';
import {coerceBooleanProperty} from '@angular/cdk/coercion';

@Component({
  selector: 'tb-profile-lwm2m-observe-attr-telemetry-resource',
  templateUrl: './lwm2m-observe-attr-telemetry-resource.component.html',
  styleUrls: ['./lwm2m-attributes.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mObserveAttrTelemetryResourceComponent),
      multi: true
    }
  ]
})

export class Lwm2mObserveAttrTelemetryResourceComponent implements ControlValueAccessor {

  private requiredValue: boolean;

  resourceFormGroup: FormGroup;
  disabled = false;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
    }
  }

  constructor(private store: Store<AppState>,
              private fb: FormBuilder) {
    this.resourceFormGroup = this.fb.group({
      resources: this.fb.array([])
    });
    this.resourceFormGroup.valueChanges.subscribe(value => {
      if (!this.disabled) {
        this.propagateChangeState(value.resources);
      }
    });
  }

  registerOnTouched(fn: any): void {
  }

  writeValue(value: ResourceLwM2M[]): void {
    this.createResourceLwM2M(value);
  }

  get resourceFormArray(): FormArray{
    return this.resourceFormGroup.get('resources') as FormArray;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.resourceFormGroup.disable();
    } else {
      this.resourceFormGroup.enable();
    }
  }

  updateValueKeyName = (event: Event, index: number): void => {
    this.resourceFormArray.at(index).patchValue({keyName: _.camelCase((event.target as HTMLInputElement).value)});
  }

  updateAttributeLwm2m = (event: Event, index: number): void => {
    this.resourceFormArray.at(index).patchValue({attributeLwm2m: event});
  }

  getNameResourceLwm2m = (resourceLwM2M: ResourceLwM2M): string => {
    return  '<' + resourceLwM2M.id +'> ' + resourceLwM2M.name;
  }

  createResourceLwM2M(resourcesLwM2M: ResourceLwM2M[]): void {
    if (resourcesLwM2M.length === this.resourceFormArray.length) {
      this.resourceFormArray.patchValue(resourcesLwM2M, {emitEvent: false});
    } else {
      this.resourceFormArray.clear();
      resourcesLwM2M.forEach(resourceLwM2M => {
        this.resourceFormArray.push(this.fb.group( {
          id: resourceLwM2M.id,
          name: resourceLwM2M.name,
          observe: resourceLwM2M.observe,
          attribute: resourceLwM2M.attribute,
          telemetry: resourceLwM2M.telemetry,
          keyName: [resourceLwM2M.keyName, Validators.required],
          attributeLwm2m: [resourceLwM2M.attributeLwm2m]
        }));
      });
    }
  }

  private propagateChange = (v: any) => { };

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  private propagateChangeState = (value: any): void => {
    if (value && this.resourceFormGroup.valid) {
      this.propagateChange(value);
    } else {
      this.propagateChange(null);
    }
  }

  trackByParams = (index: number): number => {
    return index;
  }

  updateObserve = (index: number):  void =>{
    if (this.resourceFormArray.at(index).value.attribute === false && this.resourceFormArray.at(index).value.telemetry === false) {
      this.resourceFormArray.at(index).patchValue({observe: false});
    }
  }
}
