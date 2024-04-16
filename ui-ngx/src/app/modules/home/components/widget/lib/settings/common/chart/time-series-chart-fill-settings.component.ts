///
/// Copyright © 2016-2024 The Thingsboard Authors
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
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import {
  SeriesFillSettings,
  SeriesFillType,
  seriesFillTypes,
  seriesFillTypeTranslations
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-time-series-chart-fill-settings',
  templateUrl: './time-series-chart-fill-settings.component.html',
  styleUrls: ['./../../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TimeSeriesChartFillSettingsComponent),
      multi: true
    }
  ]
})
export class TimeSeriesChartFillSettingsComponent implements OnInit, ControlValueAccessor {

  seriesFillTypes = seriesFillTypes;

  seriesFillTypeTranslationMap: Map<SeriesFillType, string> = new Map<SeriesFillType, string>([]);

  SeriesFillType = SeriesFillType;

  @Input()
  disabled: boolean;

  @Input()
  title = 'widgets.time-series-chart.series.fill';

  @Input()
  fillNoneTitle = 'widgets.time-series-chart.series.fill-type-none';

  private modelValue: SeriesFillSettings;

  private propagateChange = null;

  public fillSettingsFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
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
    this.fillSettingsFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.fillSettingsFormGroup.get('type').valueChanges.subscribe(() => {
      this.updateValidators();
    });
    for (const type of seriesFillTypes) {
      let translation: string;
      if (type === SeriesFillType.none) {
        translation = this.fillNoneTitle;
      } else {
        translation = seriesFillTypeTranslations.get(type);
      }
      this.seriesFillTypeTranslationMap.set(type, translation);
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

  writeValue(value: SeriesFillSettings): void {
    this.modelValue = value;
    this.fillSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
  }

  private updateValidators() {
    const type: SeriesFillType = this.fillSettingsFormGroup.get('type').value;
    if (type === SeriesFillType.none) {
      this.fillSettingsFormGroup.get('opacity').disable({emitEvent: false});
      this.fillSettingsFormGroup.get('gradient').disable({emitEvent: false});
    } else if (type === SeriesFillType.opacity) {
      this.fillSettingsFormGroup.get('opacity').enable({emitEvent: false});
      this.fillSettingsFormGroup.get('gradient').disable({emitEvent: false});
    } else if (type === SeriesFillType.gradient) {
      this.fillSettingsFormGroup.get('opacity').disable({emitEvent: false});
      this.fillSettingsFormGroup.get('gradient').enable({emitEvent: false});
    }
  }

  private updateModel() {
    const value: SeriesFillSettings = this.fillSettingsFormGroup.getRawValue();
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }
}
