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


import { Component, forwardRef, Input } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Instance, ObjectLwM2M, ResourceLwM2M } from './profile-config.models';
import { deepClone, isDefinedAndNotNull, isEqual, isUndefined } from '@core/utils';
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
      clientLwM2M: this.fb.array([])
    });
    this.observeAttrTelemetryFormGroup.valueChanges.subscribe(value => {
      if (isUndefined(this.disabled) || !this.disabled) {
        this.propagateChangeState(value);
      }
    });
  }

  private propagateChange = (v: any) => { };


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

  writeValue(value: any): void {
    this.buildClientObjectsLwM2M(value.clientLwM2M);
  }

  private buildClientObjectsLwM2M = (objectsLwM2M: ObjectLwM2M []): void => {
    this.observeAttrTelemetryFormGroup.setControl('clientLwM2M',
      this.createObjectsLwM2M(objectsLwM2M)
    );
  }

  private createObjectsLwM2M = (objectsLwM2M: ObjectLwM2M[]): FormArray => {
    return this.fb.array(objectsLwM2M.map((objectLwM2M) => {
      return this.fb.group({
        id: objectLwM2M.id,
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
    return this.observeAttrTelemetryFormGroup.get('clientLwM2M') as FormArray;
  }

  instancesLwm2mFormArray = (objectLwM2M: AbstractControl): FormArray => {
    return objectLwM2M.get('instances') as FormArray;
  }

  changeInstanceResourcesCheckBox = (value: boolean, instance: AbstractControl, type: string): void => {
    const resources = instance.get('resources').value as ResourceLwM2M[];
    resources.forEach(resource => resource[type] = value);
    instance.get('resources').patchValue(resources);
    this.propagateChange(this.observeAttrTelemetryFormGroup.value);
  }

  private updateValidators = (): void => {
    this.observeAttrTelemetryFormGroup.get('clientLwM2M').setValidators(this.required ? [Validators.required] : []);
    this.observeAttrTelemetryFormGroup.get('clientLwM2M').updateValueAndValidity();
  }

  trackByParams = (index: number): number => {
    return index;
  }

  getIndeterminate = (instance: AbstractControl, type: string): boolean => {
    const resources = instance.get('resources').value as ResourceLwM2M[];
    if (isDefinedAndNotNull(resources)) {
      const checkedResource = resources.filter(resource => resource[type]);
      return checkedResource.length !== 0 && checkedResource.length !== resources.length;
    }
    return false;
  }


  getChecked = (instance: AbstractControl, type: string): boolean => {
    const resources = instance.get('resources').value as ResourceLwM2M[];
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
    const instancesIds = new Set<number>();
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
        if (isDefinedAndNotNull(res)) {
          this.updateInstancesIds(res);
        }
      }
    );
  }

  private updateInstancesIds = (data: Lwm2mObjectAddInstancesData): void => {
    const valueNew = new Set<number>();
    const valueOld = new Set<number>();
    data.instancesIds.forEach(value => {
      valueNew.add(value);
    });
    const oldInstances = (this.observeAttrTelemetryFormGroup.get('clientLwM2M').value as ObjectLwM2M [])
      .find(e => e.id === data.objectId).instances;
    oldInstances.forEach(inst => {
      valueOld.add(inst.id);
    });
    if (!isEqual(valueOld, valueNew)) {
      const idsDel = this.diffBetweenSet(valueOld, valueNew);
      const idsAdd = this.diffBetweenSet(valueNew, valueOld);
      if (idsAdd.size) {
        this.addInstancesNew(data.objectId, idsAdd);
      }
      if (idsDel.size) {
        this.deleteInstances(data.objectId, idsDel);
      }
    }
  }

  private deleteInstances = (objectId: number, idsDel: Set<number>): void => {
    const objectIndex = (this.observeAttrTelemetryFormGroup.get('clientLwM2M').value as ObjectLwM2M[])
      .findIndex(element => element.id === objectId);
    idsDel.forEach(x => {
      const instancesFormArray = ((this.observeAttrTelemetryFormGroup.get('clientLwM2M') as FormArray)
        .at(objectIndex).get('instances') as FormArray);
      const instanceIndex = instancesFormArray.value.findIndex(element => element.id === x);
      instancesFormArray.removeAt(instanceIndex);
    });
  }

  private addInstancesNew = (objectId: number, idsAdd: Set<number>): void => {
    const instancesValue = (this.observeAttrTelemetryFormGroup.get('clientLwM2M').value as ObjectLwM2M [])
      .find(objectLwM2M => objectLwM2M.id === objectId).instances;
    const instancesFormArray = ((this.observeAttrTelemetryFormGroup.get('clientLwM2M') as FormArray).controls
      .find(e => e.value.id === objectId).get('instances') as FormArray) as FormArray;
    idsAdd.forEach(x => {
      const instanceNew = deepClone(instancesValue[0]) as Instance;
      instanceNew.resources.forEach(r => {
        r.attribute = false;
        r.telemetry = false;
        r.observe = false;
      });
      instancesFormArray.push(this.fb.group({
        id: x,
        resources: {value: instanceNew.resources, disabled: this.disabled}
      }));
    });
    (instancesFormArray.controls as FormGroup[]).sort((a, b) => a.value.id - b.value.id);
  }

  private diffBetweenSet = (firstSet: Set<any>, secondSet: Set<any>): Set<any> => {
    return new Set([...Array.from(firstSet)].filter(x => !secondSet.has(x)));
  }

  private setInstancesIds = (instances: Instance []): Set<number> => {
    const instancesIds = new Set<number>();
    if (instances && instances.length) {
      instances.forEach(inst => {
        instancesIds.add(inst.id);
      });
    }
    return instancesIds;
  }
}
