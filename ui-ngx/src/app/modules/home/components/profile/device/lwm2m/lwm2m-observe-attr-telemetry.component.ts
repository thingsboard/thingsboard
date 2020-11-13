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


import { Component, EventEmitter, forwardRef, Inject, Input, OnInit, Output } from "@angular/core";
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR,
  Validators
} from "@angular/forms";
import { Store } from "@ngrx/store";
import { AppState } from "@core/core.state";
import { coerceBooleanProperty } from "@angular/cdk/coercion";
import {
  ATTR,
  Instance,
  KEY_NAME,
  ObjectLwM2M,
  OBSERVE,
  ResourceLwM2M,
  TELEMETRY
} from "./profile-config.models";
import { isNotNullOrUndefined } from 'codelyzer/util/isNotNullOrUndefined';
import { isUndefined } from '@core/utils';

@Component({
  selector: 'tb-profile-lwm2m-observe-attr-telemetry',
  templateUrl: './lwm2m-observe-attr-telemetry.component.html',
  styleUrls: ['./lwm2m-observe-attr-telemetry.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mObserveAttrTelemetryComponent),
      multi: true
    }
  ]
})
export class Lwm2mObserveAttrTelemetryComponent implements ControlValueAccessor, OnInit, Validators {

  valuePrev = null as any;
  observeAttrTelemetryFormGroup: FormGroup;
  observe = OBSERVE as string;
  attribute = ATTR as string;
  telemetry = TELEMETRY as string;
  keyName = KEY_NAME as string;
  indeterminateObserve: boolean[][];
  indeterminateAttr: boolean[][];
  indeterminateTelemetry: boolean[][];
  indeterminate: {};
  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
      this.updateValidators();
    }
  }

  @Input()
  disabled: boolean;

  constructor(private store: Store<AppState>,
              private fb: FormBuilder) {
    this.observeAttrTelemetryFormGroup = this.fb.group({
      clientLwM2M: this.fb.array([], this.required ? [Validators.required] : [])
    });
    this.observeAttrTelemetryFormGroup.valueChanges.subscribe(value => {
      if (isUndefined(this.disabled)  || !this.disabled) {
        this.propagateChangeState(value);
      }
    });
  }

  ngOnInit(): void {
  }

  private propagateChange = (v: any) => {
  };


  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  private propagateChangeState(value: any): void {
    if (value) {
      if (this.valuePrev === null) {
        this.valuePrev = "init";
      } else if (this.valuePrev === "init") {
        this.valuePrev = value;
      } else if (JSON.stringify(value) !== JSON.stringify(this.valuePrev)) {
        this.valuePrev = value;
        if (this.observeAttrTelemetryFormGroup.valid) {
          this.propagateChange(value);

        } else {
          this.propagateChange(null);
        }
      }
    }

  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.valuePrev = null;
    if (isDisabled) {
      this.observeAttrTelemetryFormGroup.disable();
    } else {
      this.observeAttrTelemetryFormGroup.enable();
    }
  }

  getDisabledState(): boolean {
    return this.disabled;
  }

  writeValue(value: any): void {
    this.buildClientObjectsLwM2M(value.clientLwM2M);
  }

  private buildClientObjectsLwM2M(objectsLwM2M: ObjectLwM2M []): void {
    this.observeAttrTelemetryFormGroup.setControl('clientLwM2M',
      this.createObjectsLwM2M(objectsLwM2M)
    );
  }

  createObjectsLwM2M(objectsLwM2MJson: ObjectLwM2M []): FormArray {
    this.indeterminateObserve = [];
    this.indeterminateAttr = [];
    this.indeterminateTelemetry = [];
    this.indeterminate = {
      [this.observe]: this.indeterminateObserve,
      [this.attribute]: this.indeterminateAttr,
      [this.telemetry]: this.indeterminateTelemetry
    }
    return this.fb.array(objectsLwM2MJson.map((objectLwM2M, index) => {
      this.indeterminateObserve[index] = [];
      this.indeterminateAttr[index] = [];
      this.indeterminateTelemetry[index] = [];
      return this.fb.group({
        id: objectLwM2M.id,
        name: objectLwM2M.name,
        instances: this.createInstanceLwM2M(objectLwM2M.instances, index)
      })
    }))
  }

  createInstanceLwM2M(instanceLwM2MJson: Instance [], parentIndex?: number): FormArray {
    return this.fb.array(instanceLwM2MJson.map((instanceLwM2M, index) => {
      this.indeterminateObserve[parentIndex][index] = false;
      this.indeterminateAttr[parentIndex][index] = false;
      this.indeterminateTelemetry[parentIndex][index] = true;
      return this.fb.group({
        id: instanceLwM2M.id,
        [this.observe]: {value: false, disabled: this.disabled},
        [this.attribute]: {value: false, disabled: this.disabled},
        [this.telemetry]: {value: false, disabled: this.disabled},
        resources: {value: instanceLwM2M.resources, disabled: this.disabled}
      })
    }))
  }

  clientLwM2MFormArray(formGroup: FormGroup): FormArray {
    return formGroup.get('clientLwM2M') as FormArray;
  }

  instancesLwm2mFormArray(objectLwM2M: AbstractControl): FormArray {
    return objectLwM2M.get('instances') as FormArray;
  }

  changeInstanceResourcesCheckBox(value: boolean, instance: AbstractControl, type: string): void {
    let resources = instance.get('resources').value as ResourceLwM2M []
    resources.forEach(resource => resource[type] = value);
    instance.get('resources').patchValue(resources);
    this.propagateChange(this.observeAttrTelemetryFormGroup.value);
  }

  updateValidators() {
    this.observeAttrTelemetryFormGroup.get('clientLwM2M').setValidators(this.required ? [Validators.required] : []);
    this.observeAttrTelemetryFormGroup.get('clientLwM2M').updateValueAndValidity();
  }

  trackByParams(index: number): number {
    return index;
  }

  getIndeterminate(instance: AbstractControl, type: string) {
    const resources = instance.get('resources').value as ResourceLwM2M [];
    if (isNotNullOrUndefined(resources)) {
      const isType = (element) => element[type] === true;
      let checkedResource = resources.filter(isType);
      if (checkedResource.length === 0) return false;
      else if (checkedResource.length === resources.length) {
        instance.patchValue({[type]: true});
        return false;
      } else return true;
    }
    return false;
  }


  getChecked(instance: AbstractControl, type: string) {
    const resources = instance.get('resources').value as ResourceLwM2M [];
    if (isNotNullOrUndefined(resources)) {
      return resources.some(resource => resource[type]);
    }
    return false;
  }

  getExpended(objectLwM2M: AbstractControl) {
    return this.instancesLwm2mFormArray(objectLwM2M).length === 1;
  }
}
