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
import { MatCheckboxChange } from "@angular/material/checkbox";
import { coerceBooleanProperty } from "@angular/cdk/coercion";
import { ATTR, Instance, KEY_NAME, ObjectLwM2M, OBSERVE, ResourceLwM2M, TELEMETRY } from "./profile-config.models";

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

  @Output() valueKeyNameChange = new EventEmitter<{}>();

  constructor(private store: Store<AppState>,
              private fb: FormBuilder) {
    this.observeAttrTelemetryFormGroup = this.fb.group({
      clientLwM2M: this.fb.array([], this.required ? [Validators.required] : [])
    });
    this.observeAttrTelemetryFormGroup.valueChanges.subscribe(value => {
      if (this.disabled !== undefined &&!this.disabled) {
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
      this.observeAttrTelemetryFormGroup.disable({emitEvent: false});
    } else {
      this.observeAttrTelemetryFormGroup.enable({emitEvent: false});
    }
  }

  getDisabledState(): boolean {
    return this.disabled;
  }

  writeValue(value: any): void {
    this.buildClientObjectsLwM2M(value.clientLwM2M);
    this.initInstancesCheckBoxes();
  }

  initInstancesCheckBoxes(): void {
    (this.observeAttrTelemetryFormGroup.get('clientLwM2M') as FormArray).controls.forEach((object, objInd) => (
      (object.get('instances') as FormArray).controls.forEach((instance, instInd) => ({
          function: this.initInstancesCheckBox(objInd, instInd)
        })
      )));
  }

  initInstancesCheckBox(objInd?: number, instInd?: number): void {
    this.changeInstanceCheckBox(objInd, instInd, this.observe);
    this.changeInstanceCheckBox(objInd, instInd, this.attribute);
    this.changeInstanceCheckBox(objInd, instInd, this.telemetry);
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

  createInstanceLwM2M(instanceLwM2MJson: Instance [], parentIndex: number): FormArray {
    return this.fb.array(instanceLwM2MJson.map((instanceLwM2M, index) => {
      this.indeterminateObserve[parentIndex][index] = false;
      this.indeterminateAttr[parentIndex][index] = false;
      this.indeterminateTelemetry[parentIndex][index] = true;
      return this.fb.group({
        id: instanceLwM2M.id,
        [this.observe]: false,
        [this.attribute]: false,
        [this.telemetry]: false,
        resources: this.createResourceLwM2M(instanceLwM2M.resources)
      })
    }))
  }

  createResourceLwM2M(resourcesLwM2MJson: ResourceLwM2M []): FormArray {
    return this.fb.array(resourcesLwM2MJson.map(resourceLwM2M => {
      return this.fb.group({
        id: resourceLwM2M.id,
        name: resourceLwM2M.name,
        [this.observe]: resourceLwM2M.observe,
        [this.attribute]: resourceLwM2M.attribute,
        [this.telemetry]: resourceLwM2M.telemetry,
        [this.keyName]: resourceLwM2M.keyName
      })
    }))
  }

  clientLwM2MFormArray(formGroup: FormGroup): FormArray {
    return formGroup.get('clientLwM2M') as FormArray;
  }

  instanceLwm2mFormArray(objectLwM2M: AbstractControl): FormArray {
    return objectLwM2M.get('instances') as FormArray;
  }

  changeInstanceResourcesCheckBox(value: MatCheckboxChange, objInd: number, instInd: number, nameFrom?: string): void {
    let instance = ((this.observeAttrTelemetryFormGroup.get('clientLwM2M') as FormArray).at(objInd).get('instances') as FormArray).at(instInd);
    let resources = instance.get('resources');
    (resources as FormArray).controls.map(resource => resource.patchValue({[nameFrom]: value.checked}));
    this.indeterminate[nameFrom][objInd][instInd] = false;
    this.observeAttrTelemetryFormGroup.markAsDirty();
  }

  changeInstanceCheckBox(objInd?: number, instInd?: number, nameParameter?: string): void {
    let instance = ((this.observeAttrTelemetryFormGroup.get('clientLwM2M') as FormArray).at(objInd).get('instances') as FormArray).at(instInd);
    let indeterm = (instance.get('resources') as FormArray).controls.some(resource => {
      return resource.get(nameParameter).value === true;
    });
    let isAllObserve = (instance.get('resources') as FormArray).controls.some(resource => {
      return resource.get(nameParameter).value === false;
    });
    if (!isAllObserve && indeterm) {
      instance.patchValue({[nameParameter]: true});
      indeterm = false;
    } else if (isAllObserve) {
      instance.patchValue({[nameParameter]: false});
    }
    this.indeterminate[nameParameter][objInd][instInd] = indeterm;
  }

  changeResourceCheckBox($event: unknown): void {
    this.changeInstanceCheckBox($event['objInd'], $event['instInd'], $event['nameFrom']);
  }

  changeResourceKeyName($event: unknown): void {
    this.valueKeyNameChange.emit($event);
  }

  updateValidators() {
    this.observeAttrTelemetryFormGroup.get('clientLwM2M').setValidators(this.required ? [Validators.required] : []);
    this.observeAttrTelemetryFormGroup.get('clientLwM2M').updateValueAndValidity();
  }
}
