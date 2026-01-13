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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { RateLimits, rateLimitsArrayToString, stringToRateLimitsArray } from './rate-limits.models';
import { isDefinedAndNotNull } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-rate-limits-list',
  templateUrl: './rate-limits-list.component.html',
  styleUrls: ['./rate-limits-list.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RateLimitsListComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => RateLimitsListComponent),
      multi: true
    }
  ]
})
export class RateLimitsListComponent implements ControlValueAccessor, Validator, OnInit {

  @Input() disabled: boolean;

  rateLimitsListFormGroup: FormGroup;

  rateLimitsArray: Array<RateLimits>;

  private propagateChange = (_v: any) => { };

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {}

  ngOnInit(): void {
    this.rateLimitsListFormGroup = this.fb.group({
      rateLimits: this.fb.array([])
    });
    this.rateLimitsListFormGroup.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => {
        this.updateView(value?.rateLimits ?? []);
      }
    );
  }

  public removeRateLimits(index: number) {
    this.rateLimitsFormArray.removeAt(index);
  }

  public addRateLimits() {
    this.rateLimitsFormArray.push(this.fb.group({
      value: [null, [Validators.required]],
      time: [null, [Validators.required, this.uniqTimeRequired()]]
    }));
  }

  get rateLimitsFormArray(): FormArray<FormGroup> {
    return this.rateLimitsListFormGroup.get('rateLimits') as FormArray<FormGroup>;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.rateLimitsListFormGroup.disable({emitEvent: false});
    } else {
      this.rateLimitsListFormGroup.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.rateLimitsListFormGroup.valid ? null : {
      rateLimitsList: {valid: false}
    };
  }

  writeValue(rateLimits: string) {
    const rateLimitsControls: Array<FormGroup> = [];
    if (rateLimits) {
      this.rateLimitsArray = stringToRateLimitsArray(rateLimits);
      this.rateLimitsArray.forEach((rateLimit) => {
        const rateLimitsControl = this.fb.group({
          value: [rateLimit.value, [Validators.required]],
          time: [rateLimit.time, [Validators.required, this.uniqTimeRequired()]]
        });
        if (this.disabled) {
          rateLimitsControl.disable();
        }
        rateLimitsControls.push(rateLimitsControl);
      })
    } else {
      this.rateLimitsArray = null;
    }
    this.rateLimitsListFormGroup.setControl('rateLimits', this.fb.array(rateLimitsControls), {emitEvent: false});
  }

  private updateView(rateLimitsArray: Array<RateLimits>) {
    if (rateLimitsArray.length > 0) {
      const notNullRateLimits = rateLimitsArray.filter(rateLimits =>
        isDefinedAndNotNull(rateLimits.value) && isDefinedAndNotNull(rateLimits.time)
      );
      const rateLimitsString = rateLimitsArrayToString(notNullRateLimits);
      this.propagateChange(rateLimitsString);
      this.rateLimitsArray = stringToRateLimitsArray(rateLimitsString);
    } else {
      this.propagateChange(null);
      this.rateLimitsArray = null;
    }
  }

  private uniqTimeRequired(): ValidatorFn {
    return (control: FormControl) => {
      const formGroup = control.parent as FormGroup;
      if (!formGroup) return null;

      const formArray = formGroup.parent as FormArray;
      if (!formArray) return null;

      const newTime = control.value;
      const index = formArray.controls.indexOf(formGroup);

      const isDuplicate = formArray.controls
        .filter((_, i) => i !== index)
        .some(group => group.get('time')?.value === newTime && newTime !== '');

      return isDuplicate ? { duplicateTime: true } : null;
    };
  }
}
