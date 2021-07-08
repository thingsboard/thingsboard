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

import { ChangeDetectorRef, Component, forwardRef, Input, OnDestroy } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Instance, ObjectLwM2M } from './lwm2m-profile-config.models';
import { deepClone, isDefinedAndNotNull, isEqual } from '@core/utils';
import { MatDialog } from '@angular/material/dialog';
import {
  Lwm2mObjectAddInstancesData,
  Lwm2mObjectAddInstancesDialogComponent
} from '@home/components/profile/device/lwm2m/lwm2m-object-add-instances-dialog.component';
import _ from 'lodash';
import { Subscription } from 'rxjs';

@Component({
  selector: 'tb-profile-lwm2m-observe-attr-telemetry',
  templateUrl: './lwm2m-observe-attr-telemetry.component.html',
  styleUrls: [ './lwm2m-observe-attr-telemetry.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mObserveAttrTelemetryComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => Lwm2mObserveAttrTelemetryComponent),
      multi: true
    }
  ]
})

export class Lwm2mObserveAttrTelemetryComponent implements ControlValueAccessor, OnDestroy, Validator {

  modelsFormGroup: FormGroup;

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

  private valueChange$: Subscription = null;
  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder,
              private dialog: MatDialog,
              private cd: ChangeDetectorRef) {
    this.modelsFormGroup = this.fb.group({
      models: this.fb.array([])
    });
  }

  ngOnDestroy() {
    if (this.valueChange$) {
      this.valueChange$.unsubscribe();
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.modelsFormGroup.disable({emitEvent: false});
    } else {
      this.modelsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: ObjectLwM2M[]): void {
    if (isDefinedAndNotNull(value)) {
      this.updateModels(value);
    }
  }

  validate(): ValidationErrors | null {
    return this.modelsFormGroup.valid ? null : {
      modelsFormGroup: false
    };
  }

  get modelsFormArray(): FormArray {
    return this.modelsFormGroup.get('models') as FormArray;
  }

  private updateModels(models: ObjectLwM2M[]) {
    if (models.length === this.modelsFormArray.length) {
      this.modelsFormArray.patchValue(models, {emitEvent: false});
    } else {
      if (this.valueChange$) {
        this.valueChange$.unsubscribe();
      }
      const modelControls: Array<AbstractControl> = [];
      models.forEach(model => {
        modelControls.push(this.createModelFormGroup(model));
      });
      this.modelsFormGroup.setControl('models', this.fb.array(modelControls));
      if (this.disabled) {
        this.modelsFormGroup.disable({emitEvent: false});
      }
      this.valueChange$ = this.modelsFormGroup.valueChanges.subscribe(value => {
        this.updateModel(value.models);
      });
    }
  }

  private createModelFormGroup(objectLwM2M: ObjectLwM2M): FormGroup {
    return this.fb.group({
      id: [objectLwM2M.id],
      keyId: [objectLwM2M.keyId],
      name: [objectLwM2M.name],
      multiple: [objectLwM2M.multiple],
      mandatory: [objectLwM2M.mandatory],
      attributes: [objectLwM2M.attributes],
      instances: [objectLwM2M.instances]
    });
  }

  private updateModel = (value: ObjectLwM2M[]): void => {
    if (value && this.modelsFormGroup.valid) {
      this.propagateChange(value);
    } else {
      this.propagateChange(null);
    }
  }

  private updateValidators = (): void => {
    this.modelsFormArray.setValidators(this.required ? Validators.required : []);
    this.modelsFormArray.updateValueAndValidity();
  }

  trackByParams = (index: number, objectLwM2M: ObjectLwM2M): number => {
    return objectLwM2M.id;
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

  addInstances = ($event: Event, control: AbstractControl): void => {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }
    const object: ObjectLwM2M = control.value;
    const instancesId: Set<number> = this.instancesToSetId(object.instances);
    this.dialog.open<Lwm2mObjectAddInstancesDialogComponent, Lwm2mObjectAddInstancesData>(Lwm2mObjectAddInstancesDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        instancesId: new Set(instancesId),
        objectName: object.name,
        objectId: object.id
      }
    }).afterClosed().subscribe(
      (res: Set<number> | null) => {
        if (isDefinedAndNotNull(res)) {
          this.updateInstancesIds(res, control, instancesId);
        }
      }
    );
  }

  private updateInstancesIds(instancesId: Set<number>, control: AbstractControl, prevInstancesId: Set<number>) {
    let instancesValue: Instance[] = control.get('instances').value;
    const instanceTemplate = deepClone(instancesValue[0]);
    instanceTemplate.attributes = {};
    instanceTemplate.resources.forEach(resource => {
      resource.attribute = false;
      resource.telemetry = false;
      resource.observe = false;
      resource.keyName = _.camelCase(resource.name);
      resource.attributes = {};
    });
    if (!isEqual(prevInstancesId, instancesId)) {
      const idsDel = this.diffBetweenSet(prevInstancesId, instancesId);
      const idsAdd = this.diffBetweenSet(instancesId, prevInstancesId);
      if (idsAdd.size) {
        idsAdd.forEach(id => {
          const template = deepClone(instanceTemplate);
          template.resources.forEach(resource => resource.keyName += id);
          template.id = id;
          instancesValue.push(template);
        });
      }
      if (idsDel.size) {
        instancesValue = instancesValue.filter(instance => !idsDel.has(instance.id));
        if (instancesValue.length === 0) {
          instanceTemplate.id = 0;
          instancesValue.push(instanceTemplate);
        }
      }
      if (idsAdd.size || idsDel.size) {
        instancesValue.sort((a, b) => a.id - b.id);
        control.get('instances').patchValue(instancesValue);
        this.cd.markForCheck();
      }
    }
  }

  private diffBetweenSet<T>(firstSet: Set<T>, secondSet: Set<T>): Set<T> {
    return new Set([...Array.from(firstSet)].filter(x => !secondSet.has(x)));
  }

  private instancesToSetId(instances: Instance[]): Set<number> {
    return new Set(instances.map(x => x.id));
  }

  getNameObject = (objectLwM2M: ObjectLwM2M): string => {
    return `${objectLwM2M.name} #${objectLwM2M.keyId}`;
  }
}
