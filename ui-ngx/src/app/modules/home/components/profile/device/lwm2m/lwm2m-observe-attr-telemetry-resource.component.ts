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

import { Component, EventEmitter, forwardRef, Input, OnInit, Output, ViewChild } from "@angular/core";
import {
  ControlValueAccessor,
  FormArray, FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR, Validators
} from "@angular/forms";
import {
  CAMEL_CASE_REGEXP,
  ResourceLwM2M
} from '@home/components/profile/device/lwm2m/profile-config.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { deepClone } from '@core/utils';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

@Component({
  selector: 'tb-profile-lwm2m-observe-attr-telemetry-resource',
  templateUrl: './lwm2m-observe-attr-telemetry-resource.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mObserveAttrTelemetryResourceComponent),
      multi: true
    }
  ]
})

export class Lwm2mObserveAttrTelemetryResourceComponent implements ControlValueAccessor, OnInit, Validators {

  resourceFormGroup : FormGroup;

  @Input() i: number;
  @Input() y: number;
  @Input() objId: number;
  @Input() disabled: boolean;
  @Output() changeValueCheckBox = new EventEmitter<{}>()
  @Output() changeValueKeyName = new EventEmitter<{}>()
  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
      console.warn("required: " + value);
    }
  }
  constructor(private store: Store<AppState>,
              private fb: FormBuilder) {
    this.resourceFormGroup = this.fb.group({'resources': this.fb.array([])});
    this.resourceFormGroup.valueChanges.subscribe(value => {
      // if (this.disabled) {
        this.propagateChangeState(value.resources);
      // }
    });
  }

  ngOnInit(): void {
  }


  registerOnTouched(fn: any): void {
  }

  writeValue(value: ResourceLwM2M[]): void {
    this.createResourceLwM2M(value);
  }

  get resourceFormArray(): FormArray{
    return this.resourceFormGroup.get('resources') as FormArray;
  }

  resourceLwm2mFormArray(instance: FormGroup): FormArray {
    return instance.get('resources') as FormArray;
  }

  changeInstanceResourcesCheckBox(value: boolean, restInd: number, nameFrom?: string): void {
    this.changeValueCheckBox.emit({
      value: value,
      objInd: this.i,
      instInd: this.y,
      resInd: restInd,
      nameFrom: nameFrom
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.resourceFormGroup.disable();
    } else {
      this.resourceFormGroup.enable();
    }
  }

  getDisabledState(): boolean {
    return this.disabled;
  }

  updateValueKeyName (event: any, z: number): void {
    debugger
    let newVal = this.keysToCamel(deepClone(event.target.value));
    let insId = this.resourceFormGroup.value.id;
    let path = "/"+ this.objId + "/" + insId + "/" + z;
    this.changeValueKeyName.emit({
      path: path,
      value: newVal
    });
    event.target.value =  newVal;
  }

  keysToCamel(o: any): string {
    let val = o.split(" ");
    let playStore = [];
    val.forEach(function (item, k){
      item = item.replace(CAMEL_CASE_REGEXP, '');
      item = (k===0)? item.charAt(0).toLowerCase() + item.substr(1) : item.charAt(0).toUpperCase() + item.substr(1)
      playStore.push(item);
    });
    return playStore.join('');
  }

  createResourceLwM2M(resourcesLwM2MJson: ResourceLwM2M []): void {
    if(resourcesLwM2MJson.length === this.resourceFormArray.length) {
      this.resourceFormArray.patchValue(resourcesLwM2MJson, {emitEvent: false})
    } else {
      this.resourceFormArray.clear();
      resourcesLwM2MJson.forEach(resourceLwM2M => {
        this.resourceFormArray.push(this.fb.group({
          id: resourceLwM2M.id,
          name: resourceLwM2M.name,
          observe: resourceLwM2M.observe,
          attribute: resourceLwM2M.attribute,
          telemetry: resourceLwM2M.telemetry,
          keyName: resourceLwM2M.keyName
        }));
      })
    }
  }

  private propagateChange = (v: any) => {
  };


  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  private propagateChangeState(value: any): void {
    if (value) {
      this.propagateChange(value);
    } else {
      this.propagateChange(null);
    }
  }

  trackByParams(index: number): number {
    return index;
  }
}
