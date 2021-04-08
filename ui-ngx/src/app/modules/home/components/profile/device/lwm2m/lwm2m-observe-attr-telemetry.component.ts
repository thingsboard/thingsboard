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
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR,
  Validators
} from '@angular/forms';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {coerceBooleanProperty} from '@angular/cdk/coercion';
import {CLIENT_LWM2M, Instance, INSTANCES, ObjectLwM2M, ResourceLwM2M, RESOURCES} from './lwm2m-profile-config.models';
import {deepClone, isDefinedAndNotNull, isEqual, isUndefined} from '@core/utils';
import {MatDialog} from '@angular/material/dialog';
import {TranslateService} from '@ngx-translate/core';
import {
  Lwm2mObjectAddInstancesData,
  Lwm2mObjectAddInstancesDialogComponent
} from '@home/components/profile/device/lwm2m/lwm2m-object-add-instances-dialog.component';

@Component({
  selector: 'tb-profile-lwm2m-observe-attr-telemetry',
  templateUrl: './lwm2m-observe-attr-telemetry.component.html',
  styleUrls: [ './lwm2m-observe-attr-telemetry.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mObserveAttrTelemetryComponent),
      multi: true
    }
  ]
})

export class Lwm2mObserveAttrTelemetryComponent implements ControlValueAccessor {

  private requiredValue: boolean;

  valuePrev = null as any;
  observeAttrTelemetryFormGroup: FormGroup;

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
              private fb: FormBuilder,
              private dialog: MatDialog,
              public translate: TranslateService) {
    this.observeAttrTelemetryFormGroup = this.fb.group({
      [CLIENT_LWM2M]: this.fb.array([])
    });
    this.observeAttrTelemetryFormGroup.valueChanges.subscribe(value => {
      if (isUndefined(this.disabled) || !this.disabled) {
        // debugger
        this.propagateChangeState(value);
      }
    });
  }

  private propagateChange = (v: any) => {
  };

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  private propagateChangeState = (value: any): void => {
    if (value) {
      if (this.valuePrev === null) {
        this.valuePrev = 'init';
      } else if (this.valuePrev === 'init') {
        this.valuePrev = value;
      } else if (JSON.stringify(value) !== JSON.stringify(this.valuePrev)) {
        this.valuePrev = value;
        if (this.observeAttrTelemetryFormGroup.valid) {
          debugger
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

  writeValue(value: {}): void {
    if (isDefinedAndNotNull(value)) {
      this.buildClientObjectsLwM2M(value[CLIENT_LWM2M]);
    }
  }

  private buildClientObjectsLwM2M = (objectsLwM2M: ObjectLwM2M []): void => {
    this.observeAttrTelemetryFormGroup.setControl(CLIENT_LWM2M,
      this.createObjectsLwM2M(objectsLwM2M)
    );
  }

  private createObjectsLwM2M = (objectsLwM2M: ObjectLwM2M[]): FormArray => {
    return this.fb.array(objectsLwM2M.map((objectLwM2M) => {
      return this.fb.group({
        keyId: objectLwM2M.keyId,
        name: objectLwM2M.name,
        multiple: objectLwM2M.multiple,
        mandatory: objectLwM2M.mandatory,
        instances: this.createInstanceLwM2M(objectLwM2M.instances)
      });
    }));
  }

  private createInstanceLwM2M = (instancesLwM2M: Instance[]): FormArray => {
    return this.fb.array(instancesLwM2M.map((instanceLwM2M) => {
      return this.fb.group({
        id: instanceLwM2M.id,
        resources: {value: instanceLwM2M.resources, disabled: this.disabled}
      });
    }));
  }

  get clientLwM2MFormArray(): FormArray {
    return this.observeAttrTelemetryFormGroup.get(CLIENT_LWM2M) as FormArray;
  }

  instancesLwm2mFormArray = (objectLwM2M: AbstractControl): FormArray => {
    return objectLwM2M.get(INSTANCES) as FormArray;
  }

  changeInstanceResourcesCheckBox = (value: boolean, instance: AbstractControl, type: string): void => {
    const resources = deepClone(instance.get(RESOURCES).value as ResourceLwM2M[]);
    resources.forEach(resource => resource[type] = value);
    instance.get(RESOURCES).patchValue(resources);
    this.propagateChange(this.observeAttrTelemetryFormGroup.value);
  }

  private updateValidators = (): void => {
    this.observeAttrTelemetryFormGroup.get(CLIENT_LWM2M).setValidators(this.required ? Validators.required : []);
    this.observeAttrTelemetryFormGroup.get(CLIENT_LWM2M).updateValueAndValidity();
  }

  trackByParams = (index: number, element: any): number => {
    return index;
  }

  getIndeterminate = (instance: AbstractControl, type: string): boolean => {
    const resources = instance.get(RESOURCES).value as ResourceLwM2M[];
    if (isDefinedAndNotNull(resources)) {
      const checkedResource = resources.filter(resource => resource[type]);
      return checkedResource.length !== 0 && checkedResource.length !== resources.length;
    }
    return false;
  }

  getChecked = (instance: AbstractControl, type: string): boolean => {
    const resources = instance.get(RESOURCES).value as ResourceLwM2M[];
    return isDefinedAndNotNull(resources) && resources.every(resource => resource[type]);
  }

  getExpended = (objectLwM2M: AbstractControl): boolean => {
    return this.instancesLwm2mFormArray(objectLwM2M).length === 1;
  }

  /**
   * Instances: indicates whether this Object supports multiple Object Instances or not.
   * 1) Field in object: <MultipleInstances> == Multiple/Single
   * 2) Field in object: <Mandatory> == Mandatory/Optional
   * If this field is “Multiple” then the number of Object Instance can be from 0 to many (Object Instance ID MAX_ID=65535).
   * If this field is “Single” then the number of Object Instance can be from 0 to 1. (max count == 1)
   * If the Object field “Mandatory” is “Mandatory” and the Object field “Instances” is “Single” then -
   * the number of Object Instance MUST be 1.
   * 1. <MultipleInstances> == Multiple (true), <Mandatory>  == Optional  (false) -
   *   Object Instance ID MIN_ID=0 MAX_ID=65535 (может ни одного не быть)
   * 2. <MultipleInstances> == Multiple (true), <Mandatory>  == Mandatory (true) -
   *   Object Instance ID MIN_ID=0 MAX_ID=65535 (min один обязательный)
   * 3. <MultipleInstances> == Single   (false), <Mandatory> == Optional  (false) -
   *   Object Instance ID cnt_max = 1 cnt_min = 0 (может ни одного не быть)
   * 4. <MultipleInstances> == Single   (false), <Mandatory> == Mandatory (true) -
   *   Object Instance ID cnt_max = cnt_min = 1 (всегда есть один)
   */

  addInstances = ($event: Event, object: ObjectLwM2M): void => {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }
    this.dialog.open<Lwm2mObjectAddInstancesDialogComponent, Lwm2mObjectAddInstancesData, object>(Lwm2mObjectAddInstancesDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        instancesIds: this.instancesToSetId(object.instances),
        objectName: object.name,
        objectKeyId: object.keyId
      }
    }).afterClosed().subscribe(
      (res: Lwm2mObjectAddInstancesData | undefined) => {
        if (isDefinedAndNotNull(res)) {
          this.updateInstancesIds(res);
        }
      }
    );
  }

  private updateInstancesIds = (data: Lwm2mObjectAddInstancesData): void => {
    const objectLwM2MFormGroup = (this.observeAttrTelemetryFormGroup.get(CLIENT_LWM2M) as FormArray).controls
      .find(e => e.value.keyId === data.objectKeyId) as FormGroup;
    const instancesArray = objectLwM2MFormGroup.value.instances as Instance [];
    const instancesFormArray = objectLwM2MFormGroup.get(INSTANCES) as FormArray;
    const instance0 = deepClone(instancesFormArray.at(0).value as Instance);
    instance0.resources.forEach(r => {
      r.attribute = false;
      r.telemetry = false;
      r.observe = false;
    });
    const valueOld = this.instancesToSetId(instancesArray);
    if (!isEqual(valueOld, data.instancesIds)) {
      const idsDel = this.diffBetweenSet(valueOld, data.instancesIds);
      const idsAdd = this.diffBetweenSet(data.instancesIds, valueOld);
      if (idsAdd.size) {
        this.addInstancesNew(idsAdd, objectLwM2MFormGroup, instancesFormArray, instance0);
      }
      if (idsDel.size) {
        this.deleteInstances(idsDel, objectLwM2MFormGroup, instancesFormArray, instance0);
      }
    }
  }

  private addInstancesNew = (idsAdd: Set<number>, objectLwM2MFormGroup: FormGroup, instancesFormArray: FormArray,
                             instanceNew: Instance): void => {
    idsAdd.forEach(x => {
      this.pushInstance(instancesFormArray, x, instanceNew);
    });
    (instancesFormArray.controls as FormGroup[]).sort((a, b) => a.value.id - b.value.id);
  }

  private deleteInstances = (idsDel: Set<number>, objectLwM2MFormGroup: FormGroup, instancesFormArray: FormArray,
                             instance0: Instance): void => {
    idsDel.forEach(x => {
      const instanceIndex = instancesFormArray.value.findIndex(element => element.id === x);
      instancesFormArray.removeAt(instanceIndex);
    });
    if (instancesFormArray.length === 0) {
      this.pushInstance(instancesFormArray, 0, instance0);
    }
    (instancesFormArray.controls as FormGroup[]).sort((a, b) => a.value.id - b.value.id);
  }

  private pushInstance = (instancesFormArray: FormArray, x: number, instanceNew: Instance): void => {
    instancesFormArray.push(this.fb.group({
      id: x,
      resources: {value: instanceNew.resources, disabled: this.disabled}
    }));
  }

  private diffBetweenSet<T>(firstSet: Set<T>, secondSet: Set<T>): Set<T> {
    return new Set([...Array.from(firstSet)].filter(x => !secondSet.has(x)));
  }

  private instancesToSetId = (instances: Instance[]): Set<number> => {
    return new Set(instances.map(x => x.id));
  }
}
