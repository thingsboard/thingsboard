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

import { Component, EventEmitter, forwardRef, Input, OnInit, Output } from "@angular/core";
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray, FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR, Validators
} from "@angular/forms";
import { CAMEL_CASE_REGEXP, ResourceLwM2M } from '@home/components/profile/device/lwm2m/profile-config.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { deepClone } from '@core/utils';

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

  @Input() i: number;
  @Input() y: number;
  @Input() objId: number;
  @Input() resourceFormGroup : FormGroup;
  @Input() disabled : boolean;
  @Output() changeValueCheckBox = new EventEmitter<{}>()
  @Output() changeValueKeyName = new EventEmitter<{}>()

  constructor(private store: Store<AppState>,
              private fb: FormBuilder) {

  }

  ngOnInit(): void {
    this.setDisabledState(this.disabled);
  }

  registerOnChange(fn: any): void {
  }

  registerOnTouched(fn: any): void {
  }

  writeValue(value: ResourceLwM2M[]): void {

  }

  resourceLwm2mFormArray(instance: AbstractControl): FormArray {
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
    if (isDisabled) {
      this.resourceFormGroup.disable({emitEvent: false});
    } else {
      this.resourceFormGroup.enable({emitEvent: false});
    }
  }

  updateValueKeyName (event: any, z: number): void {
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
}
