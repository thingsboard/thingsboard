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
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  ChartFillSettings,
  ChartFillType,
  chartFillTypes,
  chartFillTypeTranslations
} from '@home/components/widget/lib/chart/chart.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-chart-fill-settings',
  templateUrl: './chart-fill-settings.component.html',
  styleUrls: ['./../../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ChartFillSettingsComponent),
      multi: true
    }
  ]
})
export class ChartFillSettingsComponent implements OnInit, ControlValueAccessor {

  chartFillTypes = chartFillTypes;

  chartFillTypeTranslationMap: Map<ChartFillType, string> = new Map<ChartFillType, string>([]);

  ChartFillType = ChartFillType;

  @Input()
  disabled: boolean;

  @Input()
  titleText = 'widgets.chart.fill';

  @Input()
  fillNoneTitle = 'widgets.chart.fill-type-none';

  private modelValue: ChartFillSettings;

  private propagateChange = null;

  public fillSettingsFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.fillSettingsFormGroup = this.fb.group({
      type: [null, []],
      opacity: [null, [Validators.min(0), Validators.max(1)]],
      gradient: this.fb.group({
        start: [null, [Validators.min(0), Validators.max(100)]],
        end: [null, [Validators.min(0), Validators.max(100)]]
      })
    });
    this.fillSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.fillSettingsFormGroup.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
    for (const type of chartFillTypes) {
      let translation: string;
      if (type === ChartFillType.none) {
        translation = this.fillNoneTitle;
      } else {
        translation = chartFillTypeTranslations.get(type);
      }
      this.chartFillTypeTranslationMap.set(type, translation);
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.fillSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.fillSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: ChartFillSettings): void {
    this.modelValue = value;
    this.fillSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
  }

  private updateValidators() {
    const type: ChartFillType = this.fillSettingsFormGroup.get('type').value;
    if (type === ChartFillType.none) {
      this.fillSettingsFormGroup.get('opacity').disable({emitEvent: false});
      this.fillSettingsFormGroup.get('gradient').disable({emitEvent: false});
    } else if (type === ChartFillType.opacity) {
      this.fillSettingsFormGroup.get('opacity').enable({emitEvent: false});
      this.fillSettingsFormGroup.get('gradient').disable({emitEvent: false});
    } else if (type === ChartFillType.gradient) {
      this.fillSettingsFormGroup.get('opacity').disable({emitEvent: false});
      this.fillSettingsFormGroup.get('gradient').enable({emitEvent: false});
    }
  }

  private updateModel() {
    const value: ChartFillSettings = this.fillSettingsFormGroup.getRawValue();
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }
}
