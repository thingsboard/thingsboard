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


import { Component, forwardRef, Input, OnInit, Output } from "@angular/core";
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
  ObjectLwM2M,
  OBSERVE,
  ResourceLwM2M,
  TELEMETRY
} from "./profile-config.models";
import { isNotNullOrUndefined } from 'codelyzer/util/isNotNullOrUndefined';
import { deepClone, isUndefined } from '@core/utils';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import {
  Lwm2mObjectAddInstancesComponent,
  Lwm2mObjectAddInstancesData
} from '@home/components/profile/device/lwm2m/lwm2m-object-add-instances.component';

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
  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  disabled: boolean;

  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
      this.updateValidators();
    }
  }

  constructor(private store: Store<AppState>,
              private fb: FormBuilder,
              private dialog: MatDialog,
              private translate: TranslateService) {
    this.observeAttrTelemetryFormGroup = this.fb.group({
      clientLwM2M: this.fb.array([])
    });
    this.observeAttrTelemetryFormGroup.valueChanges.subscribe(value => {
      if (isUndefined(this.disabled) || !this.disabled) {
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
    return this.fb.array(objectsLwM2MJson.map((objectLwM2M) => {
      return this.fb.group({
        id: objectLwM2M.id,
        name: objectLwM2M.name,
        multiple: objectLwM2M.multiple,
        mandatory: objectLwM2M.mandatory,
        instances: this.createInstanceLwM2M(objectLwM2M.instances)
      })
    }))
  }

  createInstanceLwM2M(instanceLwM2MJson: Instance []): FormArray {
    return this.fb.array(instanceLwM2MJson.map((instanceLwM2M, index) => {
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

  /**
   * Instances: indicates whether this Object supports multiple Object Instances or not.
   * 1) Field in object: <MultipleInstances> == Multiple/Single
   * 2) Field in object: <Mandatory> == Mandatory/Optional
   * If this field is “Multiple” then the number of Object Instance can be from 0 to many (Object Instance ID MAX_ID=65535).
   * If this field is “Single” then the number of Object Instance can be from 0 to 1. (max count == 1)
   * If the Object field “Mandatory” is “Mandatory” and the Object field “Instances” is “Single” then, the number of Object Instance MUST be 1.
   * 1. <MultipleInstances> == Multiple (true), <Mandatory>  == Optional  (false) => Object Instance ID MIN_ID=0 MAX_ID=65535 (может ни одного не быть)
   * 2. <MultipleInstances> == Multiple (true), <Mandatory>  == Mandatory (true)  => Object Instance ID MIN_ID=0 MAX_ID=65535 (min один обязательный)
   * 3. <MultipleInstances> == Single   (false), <Mandatory> == Optional  (false) => Object Instance ID cnt_max = 1 cnt_min = 0 (может ни одного не быть)
   * 4. <MultipleInstances> == Single   (false), <Mandatory> == Mandatory (true)  => Object Instance ID cnt_max = cnt_min = 1 (всегда есть один)
   * @param $event
   * @param objectId
   * @param objectName
   */

  addInstances($event: Event, object: ObjectLwM2M): void {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }
    let instancesIds = new Set<number>();
    object.instances.forEach(inst => {
      instancesIds.add(inst.id);
    });
    this.dialog.open<Lwm2mObjectAddInstancesComponent, Lwm2mObjectAddInstancesData, object>(Lwm2mObjectAddInstancesComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        instancesIds: this.setInstancesIds(object.instances),
        objectName: object.name,
        objectId: object.id
      }
    }).afterClosed().subscribe(
      (res: Lwm2mObjectAddInstancesData | undefined) => {
        if (isNotNullOrUndefined(res)) {
          this.updateInstancesIds(res);
        }
      }
    );
  }

  updateInstancesIds(data: Lwm2mObjectAddInstancesData) {
    let valueNew = new Set<number>();
    let valueOld = new Set<number>();
    data.instancesIds.forEach(value => {
      valueNew.add(value);
    })
    let oldInstances = (this.observeAttrTelemetryFormGroup.get('clientLwM2M').value as ObjectLwM2M []).find(e => e.id == data.objectId).instances;
    oldInstances.forEach(inst => {
      valueOld.add(inst.id);
    });
    if (JSON.stringify(Array.from(valueOld)) != JSON.stringify(Array.from(valueNew))) {
      let idsDel = this.diffBetweenSet(valueNew, this.deepCloneSet(valueOld));
      let idsAdd = this.diffBetweenSet(valueOld, this.deepCloneSet(valueNew));
      if (idsAdd.size) {
        this.addInstancesNew(data.objectId, idsAdd)
      }
      if (idsDel.size) {
        this.delInstances(data.objectId, idsDel);
      }
    }
  }

  delInstances(objectId: number, idsDel: Set<number>): void {
    let isIdIndex = (element) => element.id == objectId;
    let objectIndex = (this.observeAttrTelemetryFormGroup.get('clientLwM2M').value as ObjectLwM2M []).findIndex(isIdIndex);
    idsDel.forEach(x => {
      isIdIndex = (element) => element.value.id == x;
      let instancesFormArray = ((this.observeAttrTelemetryFormGroup.get('clientLwM2M') as FormArray).controls[objectIndex].get("instances") as FormArray);
      let instanceIndex = instancesFormArray.controls.findIndex(isIdIndex);
      instancesFormArray.removeAt(instanceIndex);
    })
  }

  addInstancesNew(objectId: number, idsAdd: Set<number>): void {
    let instancesValue = (this.observeAttrTelemetryFormGroup.get('clientLwM2M').value as ObjectLwM2M []).find(e => e.id == objectId).instances;
    let instancesFormArray = ((this.observeAttrTelemetryFormGroup.get('clientLwM2M') as FormArray).controls.find(e => e.value.id == objectId).get("instances") as FormArray) as FormArray;
    idsAdd.forEach(x => {
      let instanceNew = deepClone(instancesValue[0]) as Instance;
      instanceNew.id = x;
      instanceNew.resources.forEach(r => {
        r.attribute = false;
        r.telemetry = false;
        r.observe = false;
      });
      instancesFormArray.push(this.fb.group({
        id: x,
        [this.observe]: {value: false, disabled: this.disabled},
        [this.attribute]: {value: false, disabled: this.disabled},
        [this.telemetry]: {value: false, disabled: this.disabled},
        resources: {value: instanceNew.resources, disabled: this.disabled}
      }));
    });
    (instancesFormArray.controls as FormGroup[]).sort((a,b) => a.value.id - b.value.id);
  }

  deepCloneSet(oldSet: Set<any>): Set<any> {
    let newSet = new Set<number>();
    oldSet.forEach(p => {
      newSet.add(p);
    })
    return newSet;
  }

  diffBetweenSet(firstSet: Set<any>, secondSet: Set<any>): Set<any> {
    firstSet.forEach(p => {
      secondSet.delete(p);
    })
    return secondSet
  }

  setInstancesIds(instances: Instance []): Set<number> {
    let instancesIds = new Set<number>();
    if (instances && instances.length) {
      instances.forEach(inst => {
        instancesIds.add(inst.id);
      });
    }
    return instancesIds;
  }
}
