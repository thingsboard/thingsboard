///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
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
  Validators
} from '@angular/forms';
import { Subscription } from 'rxjs';
import {
  RateLimits,
  rateLimitsArrayToString,
  stringToRateLimitsArray
} from './rate-limits.models';
import { isDefinedAndNotNull } from '@core/utils';

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

  rateLimitsControl: FormControl;

  private propagateChange = (v: any) => { };

  private valueChangeSubscription: Subscription = null;

  constructor(private fb: FormBuilder) {
  }

  ngOnInit(): void {
    this.rateLimitsListFormGroup = this.fb.group({
      rateLimits: this.fb.array([])
    });
    this.rateLimitsControl = this.fb.control(null);
    this.valueChangeSubscription = this.rateLimitsListFormGroup.valueChanges.subscribe((value) => {
        this.updateView(value?.rateLimits);
      }
    );
  }

  get rateLimitsFormArray(): FormArray {
    return this.rateLimitsListFormGroup.get('rateLimits') as FormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.rateLimitsListFormGroup.disable({emitEvent: false});
      this.rateLimitsControl.disable({emitEvent: false});
    } else {
      this.rateLimitsListFormGroup.enable({emitEvent: false});
      this.rateLimitsControl.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.rateLimitsListFormGroup.valid && this.rateLimitsControl.valid ? null : {
      rateLimitsList: {valid: false}
    };
  }

  writeValue(value: string) {
    const rateLimitsControls: Array<FormGroup> = [];
    if (value) {
      let rateLimitsArray = value.split(',');
      for (let i = 0; i < rateLimitsArray.length; i++) {
        const [value, time] = rateLimitsArray[i].split(':');
        const rateLimitsControl = this.fb.group({
          value: [value, [Validators.required]],
          time: [time, [Validators.required]]
        });
        if (this.disabled) {
          rateLimitsControl.disable();
        }
        rateLimitsControls.push(rateLimitsControl);
      }
    }
    this.rateLimitsListFormGroup.setControl('rateLimits', this.fb.array(rateLimitsControls), {emitEvent: false});
    this.rateLimitsControl.patchValue(stringToRateLimitsArray(value), {emitEvent: false});
  }

  public removeRateLimits(index: number) {
    (this.rateLimitsListFormGroup.get('rateLimits') as FormArray).removeAt(index);
  }

  public addRateLimits() {
    this.rateLimitsFormArray.push(this.fb.group({
      value: [null, [Validators.required]],
      time: [null, [Validators.required]]
    }));
  }

  updateView(rateLimitsArray: Array<RateLimits>) {
    if (rateLimitsArray.length > 0) {
      const notNullRateLimits = rateLimitsArray.filter(rateLimits =>
        isDefinedAndNotNull(rateLimits.value) && isDefinedAndNotNull(rateLimits.time)
      );
      const rateLimitsString = rateLimitsArrayToString(notNullRateLimits);
      this.propagateChange(rateLimitsString);
      this.rateLimitsControl.patchValue(stringToRateLimitsArray(rateLimitsString), {emitEvent: false});
    } else {
      this.propagateChange(null);
      this.rateLimitsControl.patchValue(null, {emitEvent: false});
    }
  }
}
