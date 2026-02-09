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

import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  EventEmitter,
  forwardRef,
  Input,
  OnInit,
  Output,
  ViewEncapsulation
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import {
  TimeSeriesChartStateSettings,
  TimeSeriesChartStateSourceType,
  timeSeriesStateSourceTypes,
  timeSeriesStateSourceTypeTranslations
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-time-series-chart-state-row',
    templateUrl: './time-series-chart-state-row.component.html',
    styleUrls: ['./time-series-chart-state-row.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => TimeSeriesChartStateRowComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class TimeSeriesChartStateRowComponent implements ControlValueAccessor, OnInit {

  TimeSeriesChartStateSourceType = TimeSeriesChartStateSourceType;

  timeSeriesStateSourceTypes = timeSeriesStateSourceTypes;

  timeSeriesStateSourceTypeTranslations = timeSeriesStateSourceTypeTranslations;

  @Input()
  disabled: boolean;

  @Output()
  stateRemoved = new EventEmitter();

  stateFormGroup: UntypedFormGroup;

  modelValue: TimeSeriesChartStateSettings;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.stateFormGroup = this.fb.group({
      label: [null, []],
      value: [null, [Validators.required]],
      sourceType: [null, [Validators.required]],
      sourceValue: [null, [Validators.required]],
      sourceRangeFrom: [null, []],
      sourceRangeTo: [null, []]
    });
    this.stateFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
    this.stateFormGroup.get('sourceType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.stateFormGroup.disable({emitEvent: false});
    } else {
      this.stateFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: TimeSeriesChartStateSettings): void {
    this.modelValue = value;
    this.stateFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
    this.cd.markForCheck();
  }

  private updateValidators() {
    const sourceType: TimeSeriesChartStateSourceType = this.stateFormGroup.get('sourceType').value;
    if (sourceType === TimeSeriesChartStateSourceType.constant) {
      this.stateFormGroup.get('sourceValue').enable({emitEvent: false});
      this.stateFormGroup.get('sourceRangeFrom').disable({emitEvent: false});
      this.stateFormGroup.get('sourceRangeTo').disable({emitEvent: false});
    } else if (sourceType === TimeSeriesChartStateSourceType.range) {
      this.stateFormGroup.get('sourceValue').disable({emitEvent: false});
      this.stateFormGroup.get('sourceRangeFrom').enable({emitEvent: false});
      this.stateFormGroup.get('sourceRangeTo').enable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = this.stateFormGroup.value;
    this.propagateChange(this.modelValue);
  }
}
