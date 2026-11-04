// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { RangeUnit, rangeUnitTranslations } from '@home/components/rule-node/rule-node-config.models';
import { isDefinedAndNotNull, isNumeric } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { coerceBoolean, coerceNumber } from '@shared/decorators/coercion';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';

interface DistanceUnitInputModel {
  distance: number;
  distanceUnit: RangeUnit;
}

const distanceUnitsInMeters = new Map<RangeUnit, number>([
  [RangeUnit.METER, 1],
  [RangeUnit.KILOMETER, 1000]
]);

const distanceUnitsBySize = [...distanceUnitsInMeters.entries()].sort(([, a], [, b]) => b - a);

@Component({
    selector: 'tb-distance-unit-input',
    templateUrl: './distance-unit-input.component.html',
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DistanceUnitInputComponent),
            multi: true
        }, {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => DistanceUnitInputComponent),
            multi: true
        }],
    standalone: false
})
export class DistanceUnitInputComponent implements ControlValueAccessor, Validator, OnInit {

  @Input()
  labelText: string;

  @Input()
  hintText: string;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  requiredText: string;

  @Input()
  @coerceNumber()
  minDistance = 0;

  @Input()
  minErrorText: string;

  @Input()
  patternText: string;

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  @coerceBoolean()
  sameWidthInputs = false;

  @Input()
  containerClass: string | string[] | Record<string, boolean | undefined | null> = 'flex gap-4';

  distanceUnits = [...distanceUnitsInMeters.keys()];

  distanceUnitTranslations = rangeUnitTranslations;

  distanceInputForm = this.fb.group({
    distance: [0],
    distanceUnit: [RangeUnit.METER]
  });

  minValueValidator = 0;

  private modelValue: number;

  private propagateChange: (value: any) => void = () => {};

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.refreshDistanceValidators();

    this.distanceInputForm.get('distanceUnit').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.refreshDistanceValidators();
      this.distanceInputForm.get('distance').markAsTouched({onlySelf: true});
    });

    this.distanceInputForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => this.updatedModel(value));
  }

  get hasError(): string {
    const distanceControl = this.distanceInputForm.get('distance');
    if (distanceControl.hasError('required') && this.requiredText) {
      return this.requiredText;
    } else if (distanceControl.hasError('pattern') && this.patternText) {
      return this.patternText;
    } else if (distanceControl.hasError('min') && this.minErrorText) {
      return this.minErrorText;
    }
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    if (isDisabled) {
      this.distanceInputForm.disable({emitEvent: false});
    } else {
      this.distanceInputForm.enable({emitEvent: false});
    }
  }

  writeValue(meters: number) {
    if (meters !== this.modelValue) {
      if (isDefinedAndNotNull(meters) && isNumeric(meters) && Number(meters) !== 0) {
        this.distanceInputForm.patchValue(this.parseDistance(meters), {emitEvent: false});
        this.modelValue = meters;
      } else {
        this.distanceInputForm.patchValue(this.metersModel(0), {emitEvent: false});
        this.modelValue = 0;
      }
      this.refreshDistanceValidators();
    }
  }

  validate(): ValidationErrors | null {
    return this.distanceInputForm.disabled || this.distanceInputForm.valid ? null : {
      distanceInput: {
        valid: false
      }
    };
  }

  private updatedModel(value: Partial<DistanceUnitInputModel>) {
    const distance = isDefinedAndNotNull(value.distance)
      ? value.distance * distanceUnitsInMeters.get(value.distanceUnit) : null;
    if (this.modelValue !== distance) {
      this.modelValue = distance;
      this.propagateChange(distance);
    }
  }

  private parseDistance(value: number): DistanceUnitInputModel {
    for (const [distanceUnit, meters] of distanceUnitsBySize) {
      const calc = value / meters;
      if (Number.isInteger(calc)) {
        return {distance: calc, distanceUnit};
      }
    }
    return this.metersModel(value);
  }

  private metersModel(distance: number): DistanceUnitInputModel {
    return {distance, distanceUnit: RangeUnit.METER};
  }

  private refreshDistanceValidators() {
    const distanceControl = this.distanceInputForm.get('distance');
    const currentUnit = this.distanceInputForm.get('distanceUnit').value;
    const metersInUnit = distanceUnitsInMeters.get(currentUnit) ?? 1;
    const validators: ValidatorFn[] = [Validators.pattern(/^\d*$/)];
    if (this.required) {
      validators.push(Validators.required);
    }
    this.minValueValidator = Math.ceil(this.minDistance / metersInUnit);
    validators.push(Validators.min(this.minValueValidator));
    distanceControl.setValidators(validators);
    distanceControl.updateValueAndValidity({onlySelf: true, emitEvent: false});
  }
}
