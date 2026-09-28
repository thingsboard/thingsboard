// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, DestroyRef, forwardRef, inject, Input } from '@angular/core';
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
import { ResourceLwM2M } from '@home/components/profile/device/lwm2m/lwm2m-profile-config.models';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { combineLatest } from 'rxjs';
import { startWith } from 'rxjs/operators';
import { GtSmBreakpointAwareDirective } from '@shared/components/gt-sm-breakpoint-aware.directive';

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
    ],
    standalone: false
})

export class Lwm2mObserveAttrTelemetryResourcesComponent extends GtSmBreakpointAwareDirective implements ControlValueAccessor, Validator {

  resourcesFormGroup: UntypedFormGroup;

  private destroyRef = inject(DestroyRef);

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

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    super();
    this.resourcesFormGroup = this.fb.group({
      resources: this.fb.array([])
    });

    this.resourcesFormGroup.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(() => this.updateModel(this.resourcesFormGroup.getRawValue().resources));
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

  get resourcesFormArray(): UntypedFormArray {
    return this.resourcesFormGroup.get('resources') as UntypedFormArray;
  }

  getNameResourceLwm2m(resourceLwM2M: ResourceLwM2M): string {
    return `#${resourceLwM2M.id} ${resourceLwM2M.name}`;
  }

  private updatedResources(resources: ResourceLwM2M[]): void {
    if (resources.length === this.resourcesFormArray.length) {
      this.resourcesFormArray.patchValue(resources, {onlySelf: true, emitEvent: false});
    } else {
      const resourcesControl: Array<AbstractControl> = [];
      if (resources) {
        resources.forEach((resource) => {
          resourcesControl.push(this.createdResourceFormGroup(resource));
        });
      }
      this.resourcesFormGroup.setControl('resources', this.fb.array(resourcesControl), {emitEvent: false});
      if (this.disabled) {
        this.resourcesFormGroup.disable({emitEvent: false});
      }
    }
  }

  private createdResourceFormGroup(resource: ResourceLwM2M): UntypedFormGroup {
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
      form.get('attribute').valueChanges.pipe(startWith(resource.attribute), takeUntilDestroyed(this.destroyRef)),
      form.get('telemetry').valueChanges.pipe(startWith(resource.telemetry), takeUntilDestroyed(this.destroyRef))
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

  isDisabledObserve(index: number): boolean{
    return this.resourcesFormArray.at(index).get('observe').disabled;
  }
}
