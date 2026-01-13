///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { Component, forwardRef, Input, OnDestroy } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Instance, ResourceLwM2M, ResourceSettingTelemetry, } from './lwm2m-profile-config.models';
import { deepClone, isDefinedAndNotNull } from '@core/utils';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';

@Component({
  selector: 'tb-profile-lwm2m-observe-attr-telemetry-instances',
  templateUrl: './lwm2m-observe-attr-telemetry-instances.component.html',
  styleUrls: [ './lwm2m-observe-attr-telemetry-instances.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mObserveAttrTelemetryInstancesComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => Lwm2mObserveAttrTelemetryInstancesComponent),
      multi: true
    }
  ]
})

export class Lwm2mObserveAttrTelemetryInstancesComponent implements ControlValueAccessor, Validator, OnDestroy {

  instancesFormGroup: UntypedFormGroup;

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

  constructor(private fb: UntypedFormBuilder,
              public translate: TranslateService) {
    this.instancesFormGroup = this.fb.group({
      instances: this.fb.array([])
    });

    this.valueChange$ = this.instancesFormGroup.valueChanges.subscribe(value => this.updateModel(value.instances));
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
      this.instancesFormGroup.disable({emitEvent: false});
    } else {
      this.instancesFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Instance[]): void {
    this.updateInstances(value);
  }

  validate(control: AbstractControl): ValidationErrors | null {
    return this.instancesFormGroup.valid ? null : {
      instancesForm: false
    };
  }

  get instancesFormArray(): UntypedFormArray {
    return this.instancesFormGroup.get('instances') as UntypedFormArray;
  }

  private updateInstances(instances: Instance[]): void {
    if (instances.length === this.instancesFormArray.length) {
      this.instancesFormArray.patchValue(instances, {emitEvent: false});
    } else {
      const instancesControl: Array<AbstractControl> = [];
      if (instances) {
        instances.forEach((instance) => {
          instancesControl.push(this.createInstanceFormGroup(instance));
        });
      }
      this.instancesFormGroup.setControl('instances', this.fb.array(instancesControl), {emitEvent: false});
      if (this.disabled) {
        this.instancesFormGroup.disable({emitEvent: false});
      }
    }
  }

  private createInstanceFormGroup(instance: Instance): UntypedFormGroup {
    return this.fb.group({
      id: [instance.id],
      attributes: [instance.attributes],
      resources: [instance.resources]
    });
  }

  private updateModel(instances: Instance[]) {
    if (instances && this.instancesFormGroup.valid) {
      this.propagateChange(instances);
    } else {
      this.propagateChange(null);
    }
  }

  changeInstanceResourcesCheckBox = (value: boolean, instance: AbstractControl, type: ResourceSettingTelemetry): void => {
    const resources = deepClone(instance.get('resources').value as ResourceLwM2M[]);
    if (value && type === 'observe') {
      resources.forEach(resource => resource[type] = resource.telemetry || resource.attribute);
    } else if (!value && type !== 'observe') {
      resources.forEach(resource => {
        resource[type] = value;
        if (resource.observe && !(resource.telemetry || resource.attribute)) {
          resource.observe = false;
        }
      });
    } else {
      resources.forEach(resource => resource[type] = value);
    }
    instance.get('resources').patchValue(resources);
  }

  private updateValidators(): void {
    this.instancesFormArray.setValidators(this.required ? Validators.required : []);
    this.instancesFormArray.updateValueAndValidity();
  }

  trackByParams = (index: number, instance: Instance): number => {
    return instance.id;
  }

  getIndeterminate = (instance: AbstractControl, type: ResourceSettingTelemetry): boolean => {
    const resources = instance.get('resources').value as ResourceLwM2M[];
    if (isDefinedAndNotNull(resources)) {
      const checkedResource = resources.filter(resource => resource[type]);
      return checkedResource.length !== 0 && checkedResource.length !== resources.length;
    }
    return false;
  }

  getChecked = (instance: AbstractControl, type: ResourceSettingTelemetry): boolean => {
    const resources = instance.get('resources').value as ResourceLwM2M[];
    return isDefinedAndNotNull(resources) && resources.every(resource => resource[type]);
  }

  disableObserve(instance: AbstractControl): boolean {
    return this.disabled || !(
      this.getIndeterminate(instance, 'telemetry') ||
      this.getIndeterminate(instance, 'attribute') ||
      this.getChecked(instance, 'telemetry') ||
      this.getChecked(instance, 'attribute')
    );
  }

  get isExpend(): boolean {
    return this.instancesFormArray.length === 1;
  }

  getNameInstance(instance: Instance): string {
    return `${this.translate.instant('device-profile.lwm2m.instance')} #${instance.id}`;
  }

  disableObserveInstance = (instance: AbstractControl): boolean => {
    const checkedAttrTelemetry = this.observeInstance(instance);
    if (checkedAttrTelemetry) {
      instance.get('attributes').patchValue(null, {emitEvent: false});
    }
    return checkedAttrTelemetry;
  }


  observeInstance = (instance: AbstractControl): boolean => {
    const resources = instance.get('resources').value as ResourceLwM2M[];
    if (isDefinedAndNotNull(resources)) {
      const checkedAttribute = resources.filter(resource => resource.attribute);
      const checkedTelemetry = resources.filter(resource => resource.telemetry);
      return checkedAttribute.length === 0 && checkedTelemetry.length === 0;
    }
    return false;
  }
}
