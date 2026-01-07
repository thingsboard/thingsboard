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
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import {
  TimeSeriesChartNoAggregationBarWidthSettings,
  timeSeriesChartNoAggregationBarWidthStrategies,
  TimeSeriesChartNoAggregationBarWidthStrategy,
  timeSeriesChartNoAggregationBarWidthStrategyTranslations
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { merge } from 'rxjs';
import { coerceBoolean } from '@shared/decorators/coercion';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-time-series-no-aggregation-bar-width-settings',
  templateUrl: './time-series-no-aggregation-bar-width-settings.component.html',
  styleUrls: ['./../../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimeSeriesNoAggregationBarWidthSettingsComponent),
      multi: true
    }
  ]
})
export class TimeSeriesNoAggregationBarWidthSettingsComponent implements OnInit, ControlValueAccessor {

  TimeSeriesChartNoAggregationBarWidthStrategy = TimeSeriesChartNoAggregationBarWidthStrategy;

  timeSeriesChartNoAggregationBarWidthStrategies = timeSeriesChartNoAggregationBarWidthStrategies;

  timeSeriesChartNoAggregationBarWidthStrategyTranslations = timeSeriesChartNoAggregationBarWidthStrategyTranslations;

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  stroked = false;

  private modelValue: TimeSeriesChartNoAggregationBarWidthSettings;

  private propagateChange = null;

  public barWidthSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.barWidthSettingsFormGroup = this.fb.group({
      strategy: [null, []],
      groupWidth: this.fb.group({
        relative: [null, []],
        relativeWidth: [null, [Validators.required, Validators.min(0.1), Validators.max(100)]],
        absoluteWidth: [null, [Validators.required, Validators.min(100)]]
      }),
      barWidth: this.fb.group({
        relative: [null, []],
        relativeWidth: [null, [Validators.required, Validators.min(0.1), Validators.max(100)]],
        absoluteWidth: [null, [Validators.required, Validators.min(100)]]
      })
    });
    this.barWidthSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    merge(this.barWidthSettingsFormGroup.get('strategy').valueChanges,
      this.barWidthSettingsFormGroup.get('groupWidth.relative').valueChanges,
      this.barWidthSettingsFormGroup.get('barWidth.relative').valueChanges
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.barWidthSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.barWidthSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: TimeSeriesChartNoAggregationBarWidthSettings): void {
    this.modelValue = value;
    this.barWidthSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
  }

  private updateValidators() {
    const strategy: TimeSeriesChartNoAggregationBarWidthStrategy =
      this.barWidthSettingsFormGroup.get('strategy').value;
    const groupWidthRelative: boolean = this.barWidthSettingsFormGroup.get('groupWidth.relative').value;
    const barWidthRelative: boolean = this.barWidthSettingsFormGroup.get('barWidth.relative').value;
    if (strategy === TimeSeriesChartNoAggregationBarWidthStrategy.group) {
      this.barWidthSettingsFormGroup.get('groupWidth').enable({emitEvent: false});
      this.barWidthSettingsFormGroup.get('barWidth').disable({emitEvent: false});
      if (groupWidthRelative) {
        this.barWidthSettingsFormGroup.get('groupWidth').get('relativeWidth').enable({emitEvent: false});
        this.barWidthSettingsFormGroup.get('groupWidth').get('absoluteWidth').disable({emitEvent: false});
      } else {
        this.barWidthSettingsFormGroup.get('groupWidth').get('relativeWidth').disable({emitEvent: false});
        this.barWidthSettingsFormGroup.get('groupWidth').get('absoluteWidth').enable({emitEvent: false});
      }
    } else if (strategy === TimeSeriesChartNoAggregationBarWidthStrategy.separate) {
      this.barWidthSettingsFormGroup.get('groupWidth').disable({emitEvent: false});
      this.barWidthSettingsFormGroup.get('barWidth').enable({emitEvent: false});
      if (barWidthRelative) {
        this.barWidthSettingsFormGroup.get('barWidth').get('relativeWidth').enable({emitEvent: false});
        this.barWidthSettingsFormGroup.get('barWidth').get('absoluteWidth').disable({emitEvent: false});
      } else {
        this.barWidthSettingsFormGroup.get('barWidth').get('relativeWidth').disable({emitEvent: false});
        this.barWidthSettingsFormGroup.get('barWidth').get('absoluteWidth').enable({emitEvent: false});
      }
    }
  }

  private updateModel() {
    this.modelValue = this.barWidthSettingsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }
}
