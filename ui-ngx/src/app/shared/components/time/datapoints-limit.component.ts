// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { TimeService } from '@core/services/time.service';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
    selector: 'tb-datapoints-limit',
    templateUrl: './datapoints-limit.component.html',
    styleUrls: ['./datapoints-limit.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DatapointsLimitComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => DatapointsLimitComponent),
            multi: true
        }
    ],
    standalone: false
})
export class DatapointsLimitComponent implements ControlValueAccessor, Validator, OnInit, OnDestroy {

  datapointsLimitFormGroup: FormGroup;

  modelValue: number | null;

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

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder,
              private timeService: TimeService) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.datapointsLimitFormGroup = this.fb.group({
      limit: [null, [Validators.min(this.minDatapointsLimit()), Validators.max(this.maxDatapointsLimit())]]
    });
    this.datapointsLimitFormGroup.get('limit').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateView(value);
    });
  }

  updateValidators() {
    if (this.datapointsLimitFormGroup) {
      if (this.required) {
        this.datapointsLimitFormGroup.get('limit').addValidators(Validators.required);
      } else {
        this.datapointsLimitFormGroup.get('limit').removeValidators(Validators.required);
      }
      this.datapointsLimitFormGroup.get('limit').updateValueAndValidity();
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.datapointsLimitFormGroup.disable({emitEvent: false});
    } else {
      this.datapointsLimitFormGroup.enable({emitEvent: false});
    }
  }

  private checkLimit(limit?: number): number {
    if (!limit || limit < this.minDatapointsLimit()) {
      return this.minDatapointsLimit();
    } else if (limit > this.maxDatapointsLimit()) {
      return this.maxDatapointsLimit();
    }
    return limit;
  }

  writeValue(value: number | null): void {
    this.modelValue = this.checkLimit(value);
    this.datapointsLimitFormGroup.patchValue(
      { limit: this.modelValue }, {emitEvent: false}
    );
  }

  updateView(value: number | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  validate(): ValidationErrors {
    return this.datapointsLimitFormGroup.get('limit').valid ? null : {
      datapointsLimitFormGroup: false,
    };
  }

  minDatapointsLimit() {
    return this.timeService.getMinDatapointsLimit();
  }

  maxDatapointsLimit() {
    return this.timeService.getMaxDatapointsLimit();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

}
