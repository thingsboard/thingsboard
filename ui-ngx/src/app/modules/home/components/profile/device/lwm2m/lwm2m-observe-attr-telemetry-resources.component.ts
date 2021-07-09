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

import { Component, forwardRef, Input, OnDestroy } from '@angular/core';
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
import { ResourceLwM2M } from '@home/components/profile/device/lwm2m/lwm2m-profile-config.models';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { combineLatest, Subject, Subscription } from 'rxjs';
import { startWith, takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-profile-lwm2m-observe-attr-telemetry-resource',
  templateUrl: './lwm2m-observe-attr-telemetry-resources.component.html',
  styleUrls: ['./lwm2m-observe-attr-telemetry-resources.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mObserveAttrTelemetryResourcesComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => Lwm2mObserveAttrTelemetryResourcesComponent),
      multi: true
    }
  ]
})

export class Lwm2mObserveAttrTelemetryResourcesComponent implements ControlValueAccessor, OnDestroy, Validator {

  resourcesFormGroup: FormGroup;

  @Input()
  disabled = false;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
    }
  }

  private destroy$ = new Subject();
  private valueChange$: Subscription = null;
  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder) {
    this.resourcesFormGroup = this.fb.group({
      resources: this.fb.array([])
    });
  }

  ngOnDestroy() {
    if (this.valueChange$) {
      this.valueChange$.unsubscribe();
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnTouched(fn: any): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  writeValue(value: ResourceLwM2M[]): void {
    this.updatedResources(value);
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.resourcesFormGroup.disable({emitEvent: false});
    } else {
      this.resourcesFormArray.controls.forEach(resource => {
        resource.get('id').enable({emitEvent: false});
        resource.get('name').enable({emitEvent: false});
        resource.get('keyName').enable({emitEvent: false});
        resource.get('attribute').enable({emitEvent: false});
        resource.get('telemetry').enable({onlySelf: true});
        resource.get('attributes').enable({emitEvent: false});
      });
    }
  }

  validate(): ValidationErrors | null {
    return this.resourcesFormGroup.valid ? null : {
      resources: false
    };
  }

  get resourcesFormArray(): FormArray {
    return this.resourcesFormGroup.get('resources') as FormArray;
  }

  getNameResourceLwm2m(resourceLwM2M: ResourceLwM2M): string {
    return `#${resourceLwM2M.id} ${resourceLwM2M.name}`;
  }

  private updatedResources(resources: ResourceLwM2M[]): void {
    if (resources.length === this.resourcesFormArray.length) {
      this.resourcesFormArray.patchValue(resources, {onlySelf: true});
    } else {
      if (this.valueChange$) {
        this.valueChange$.unsubscribe();
      }
      const resourcesControl: Array<AbstractControl> = [];
      if (resources) {
        resources.forEach((resource) => {
          resourcesControl.push(this.createdResourceFormGroup(resource));
        });
      }
      this.resourcesFormGroup.setControl('resources', this.fb.array(resourcesControl));
      if (this.disabled) {
        this.resourcesFormGroup.disable({emitEvent: false});
      }
      this.valueChange$ = this.resourcesFormGroup.valueChanges.subscribe(() => {
        this.updateModel(this.resourcesFormGroup.getRawValue().resources);
      });
    }
  }

  private createdResourceFormGroup(resource: ResourceLwM2M): FormGroup {
    const form = this.fb.group( {
      id: [resource.id],
      name: [resource.name],
      attribute: [resource.attribute],
      telemetry: [resource.telemetry],
      observe: [resource.observe],
      keyName: [resource.keyName, [Validators.required, Validators.pattern('(.|\\s)*\\S(.|\\s)*')]],
      attributes: [resource.attributes]
    });
    combineLatest([
      form.get('attribute').valueChanges.pipe(startWith(resource.attribute), takeUntil(this.destroy$)),
      form.get('telemetry').valueChanges.pipe(startWith(resource.telemetry), takeUntil(this.destroy$))
    ]).subscribe(([attribute, telemetry]) => {
      if (!this.disabled) {
        if (attribute || telemetry) {
          form.get('observe').enable({emitEvent: false});
        } else {
          form.get('observe').disable({emitEvent: false});
          form.get('observe').patchValue(false, {emitEvent: false});
          form.get('attributes').patchValue({}, {emitEvent: false});
        }
      }
    });
    return form;
  }

  private updateModel(value: ResourceLwM2M[]) {
    if (value && this.resourcesFormGroup.valid) {
      this.propagateChange(value);
    } else {
      this.propagateChange(null);
    }
  }

  trackByParams(index: number, resource: ResourceLwM2M): number {
    return resource.id;
  }

  isDisabledObserve(index: number): boolean{
    return this.resourcesFormArray.at(index).get('observe').disabled;
  }
}
