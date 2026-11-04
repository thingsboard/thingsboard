// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, forwardRef, Input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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
import { ThemePalette } from '@angular/material/core';
import { Instance, ResourceLwM2M, ResourceSettingTelemetry, } from './lwm2m-profile-config.models';
import { deepClone, isDefinedAndNotNull } from '@core/utils';
import { TranslateService } from '@ngx-translate/core';
import { GtSmBreakpointAwareDirective } from '@shared/components/gt-sm-breakpoint-aware.directive';

@Component({
    selector: 'tb-profile-lwm2m-observe-attr-telemetry-instances',
    templateUrl: './lwm2m-observe-attr-telemetry-instances.component.html',
    styleUrls: ['./lwm2m-observe-attr-telemetry-instances.component.scss'],
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
    ],
    standalone: false
})

export class Lwm2mObserveAttrTelemetryInstancesComponent extends GtSmBreakpointAwareDirective implements ControlValueAccessor, Validator {

  readonly resourceToggles: {type: ResourceSettingTelemetry; color: ThemePalette; labelKey: string}[] = [
    {type: 'attribute', color: 'warn', labelKey: 'device-profile.lwm2m.select-all-attribute'},
    {type: 'telemetry', color: 'primary', labelKey: 'device-profile.lwm2m.select-all-telemetry'},
    {type: 'observe', color: 'primary', labelKey: 'device-profile.lwm2m.select-all-observe'}
  ];

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

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder,
              public translate: TranslateService) {
    super();
    this.instancesFormGroup = this.fb.group({
      instances: this.fb.array([])
    });

    this.instancesFormGroup.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => this.updateModel(value.instances));
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

  isToggleDisabled(instance: AbstractControl, type: ResourceSettingTelemetry): boolean {
    return type === 'observe' ? this.disableObserve(instance) : this.disabled;
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
