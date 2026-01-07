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

import { Component, DestroyRef, forwardRef, Input, OnInit, ViewEncapsulation } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator
} from '@angular/forms';
import {
  TimeSeriesChartStateSettings,
  TimeSeriesChartStateSourceType,
  timeSeriesChartStateValid,
  timeSeriesChartStateValidator
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-time-series-chart-states-panel',
  templateUrl: './time-series-chart-states-panel.component.html',
  styleUrls: ['./time-series-chart-states-panel.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimeSeriesChartStatesPanelComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => TimeSeriesChartStatesPanelComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class TimeSeriesChartStatesPanelComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  disabled: boolean;

  statesFormGroup: UntypedFormGroup;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.statesFormGroup = this.fb.group({
      states: [this.fb.array([]), []]
    });
    this.statesFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        let states: TimeSeriesChartStateSettings[] = this.statesFormGroup.get('states').value;
        if (states) {
          states = states.filter(s => timeSeriesChartStateValid(s));
        }
        this.propagateChange(states);
      }
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.statesFormGroup.disable({emitEvent: false});
    } else {
      this.statesFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: TimeSeriesChartStateSettings[] | undefined): void {
    const states = value || [];
    this.statesFormGroup.setControl('states', this.prepareStatesFormArray(states), {emitEvent: false});
  }

  public validate(c: UntypedFormControl) {
    const valid = this.statesFormGroup.valid;
    return valid ? null : {
      states: {
        valid: false,
      },
    };
  }

  statesFormArray(): UntypedFormArray {
    return this.statesFormGroup.get('states') as UntypedFormArray;
  }

  trackByState(index: number, stateControl: AbstractControl): any {
    return stateControl;
  }

  removeState(index: number) {
    (this.statesFormGroup.get('states') as UntypedFormArray).removeAt(index);
  }

  addState() {
    const state: TimeSeriesChartStateSettings = {
      label: '',
      value: 0,
      sourceType: TimeSeriesChartStateSourceType.constant
    };
    const statesArray = this.statesFormGroup.get('states') as UntypedFormArray;
    const stateControl = this.fb.control(state, [timeSeriesChartStateValidator]);
    statesArray.push(stateControl);
  }

  private prepareStatesFormArray(states: TimeSeriesChartStateSettings[] | undefined): UntypedFormArray {
    const statesControls: Array<AbstractControl> = [];
    if (states) {
      states.forEach((state) => {
        statesControls.push(this.fb.control(state, [timeSeriesChartStateValidator]));
      });
    }
    return this.fb.array(statesControls);
  }

}
