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

import {PageComponent} from "@shared/components/page.component";
import {Component, forwardRef, Inject, Input, OnInit, Output} from "@angular/core";
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALUE_ACCESSOR,
  Validators
} from "@angular/forms";
import {Store} from "@ngrx/store";
import {AppState} from "@core/core.state";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {
  DeviceCredentialsDialogLwm2mData,
  ObjectLwM2M,
  Instance, ResourceLwM2M
} from "@home/pages/device/lwm2m/security-config.models";
import {MatCheckboxChange} from "@angular/material/checkbox";


@Component({
  selector: 'tb-security-config-observe-attr-lwm2m',
  templateUrl: './security-config-observe-attr.component.html',
  styleUrls: ['./security-config-observe-attr.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SecurityConfigObserveAttrComponent),
      multi: true
    }
  ]
})
export class SecurityConfigObserveAttrComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input() observeFormGroup: FormGroup;
  observeValue: ObjectLwM2M[];
  instance: FormArray;
  clientLwM2M: FormArray;
  isObserve = 'isObserv' as string;
  isAttr = 'isAttr' as string;
  isTelemetry = 'isTelemetry' as string;
  indeterminateObserve: boolean[][];
  indeterminateAttr: boolean[][];
  indeterminateTelemetry: boolean[][];
  indeterminate: {};

  constructor(protected store: Store<AppState>,
              @Inject(MAT_DIALOG_DATA) public data: DeviceCredentialsDialogLwm2mData,
              public dialogRef: MatDialogRef<SecurityConfigObserveAttrComponent, object>,
              public fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
  }

  registerOnChange(fn: any): void {
  }

  registerOnTouched(fn: any): void {
  }

  writeValue(value: ObjectLwM2M[]): void {
    this.observeValue = value;
    if (this.observeValue && this.observeValue.length > 0) {
      this.buildClientObjectsLwM2M(this.observeValue)
      this.initInstancesCheckBoxs();
    }
  }

  initInstancesCheckBoxs(): void {
    let objects = this.observeFormGroup.get('clientLwM2M') as FormArray;
    objects.controls.forEach((object, objInd) => (
      (object.get('instance') as FormArray).controls.forEach((instance, instInd) => ({
          function: this.initInstancesCheckBox(objInd, instInd)
        })
      )));
  }

  initInstancesCheckBox(objInd?: number, instInd?: number): void {
    this.changeInstanceCheckBox(objInd, instInd, this.isObserve);
    this.changeInstanceCheckBox(objInd, instInd, this.isAttr);
    this.changeInstanceCheckBox(objInd, instInd, this.isTelemetry);
  }

  private buildClientObjectsLwM2M(objectsLwM2M: ObjectLwM2M []): void {
    this.observeFormGroup.addControl('clientLwM2M',
      this.createObjectsLwM2M(objectsLwM2M)
    );
  }

  createObjectsLwM2M(objectsLwM2MJson: ObjectLwM2M []): FormArray {
    this.indeterminateObserve = [];
    this.indeterminateAttr = [];
    this.indeterminateTelemetry= [];
    this.indeterminate = {
      [this.isObserve] : this.indeterminateObserve,
      [this.isAttr] : this.indeterminateAttr,
      [this.isTelemetry] : this.indeterminateTelemetry
    }
    return this.fb.array(objectsLwM2MJson.map((objectLwM2M, index) => {
      this.indeterminateObserve[index] = [];
      this.indeterminateAttr[index] = [];
      this.indeterminateTelemetry[index] = [];
      return this.fb.group({
        id: objectLwM2M.id,
        name: objectLwM2M.name,
        instance: this.createInstanceLwM2M(objectLwM2M.instance, index)
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
        [this.isObserve]: false,
        [this.isAttr]: false,
        [this.isTelemetry]: true,
        resource: this.createResourceLwM2M(instanceLwM2M.resource)
      })
    }))
  }

  createResourceLwM2M(resourcesLwM2MJson: ResourceLwM2M []): FormArray {
    return this.fb.array(resourcesLwM2MJson.map(resourceLwM2M => {
      return this.fb.group({
        id: resourceLwM2M.id,
        [this.isObserve]: resourceLwM2M.isObserv,
        [this.isAttr]: resourceLwM2M.isAttr,
        [this.isTelemetry]: !resourceLwM2M.isAttr,
        name: resourceLwM2M.name
      })
    }))
  }

  clientLwM2MFormArray(formGroup: FormGroup): FormArray {
    return formGroup.get('clientLwM2M') as FormArray;
  }

  instanceLwm2mFormArray(objectLwM2M: AbstractControl): FormArray {
    return objectLwM2M.get('instance') as FormArray;
  }

  resourceLwm2mFormArray(instance: AbstractControl): FormArray {
    return instance.get('resource') as FormArray;
  }

  changeInstanceResourcesCheckBox(value: MatCheckboxChange, objInd: number, instInd: number, nameFrom?: string, nameTo?: string): void {
    let instance = ((this.observeFormGroup.get('clientLwM2M') as FormArray).at(objInd).get('instance') as FormArray).at(instInd);
    let resources = instance.get('resource');
    // let resources = ((this.observeFormGroup.get('clientLwM2M') as FormArray).at(objInd).get('instance') as FormArray).at(instInd).get('resource');
    (resources as FormArray).controls.map(resource => resource.patchValue({[nameFrom]: value.checked}));
    this.indeterminate[nameFrom][objInd][instInd] = false;
    if (nameTo) {
      (resources as FormArray).controls.map(resource => resource.patchValue({[nameTo]: !value.checked}));
      this.indeterminate[nameTo][objInd][instInd] = false;
      instance.patchValue({[nameTo]: !value.checked});
    }
  }

  changeInstanceCheckBox(objInd?: number, instInd?: number, nameParameter?: string): void {
    let instance = ((this.observeFormGroup.get('clientLwM2M') as FormArray).at(objInd).get('instance') as FormArray).at(instInd);
    let indeterm = (instance.get('resource') as FormArray).controls.some(resource => {
      return resource.get(nameParameter).value === true;
    });
    let isAllObserve = (instance.get('resource') as FormArray).controls.some(resource => {
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

  changeResourceAttrTelemetry(value: boolean, objInd: number, instInd: number, resInd: number, nameFrom?: string, nameTo?: string): void {
    (((this.observeFormGroup.get('clientLwM2M') as FormArray)
      .at(objInd).get('instance') as FormArray)
      .at(instInd).get('resource') as FormArray)
      .at(resInd).patchValue({[nameTo]: !value});
    this.changeInstanceCheckBox(objInd, instInd,  nameFrom);
    this.changeInstanceCheckBox(objInd, instInd,  nameTo);
  }
}
