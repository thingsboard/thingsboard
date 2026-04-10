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
import { chartAnimationEasings, ChartAnimationSettings } from '@home/components/widget/lib/chart/chart.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-chart-animation-settings',
    templateUrl: './chart-animation-settings.component.html',
    styleUrls: ['./../../widget-settings.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ChartAnimationSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class ChartAnimationSettingsComponent implements OnInit, ControlValueAccessor {

  settingsExpanded = false;

  chartAnimationEasings = chartAnimationEasings;

  @Input()
  disabled: boolean;

  private modelValue: ChartAnimationSettings;

  private propagateChange = null;

  public animationSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.animationSettingsFormGroup = this.fb.group({
      animation: [null, []],
      animationThreshold: [null, [Validators.min(0)]],
      animationDuration: [null, [Validators.min(0)]],
      animationEasing: [null, []],
      animationDelay: [null, [Validators.min(0)]],
      animationDurationUpdate: [null, [Validators.min(0)]],
      animationEasingUpdate: [null, []],
      animationDelayUpdate: [null, [Validators.min(0)]]
    });
    this.animationSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.animationSettingsFormGroup.get('animation').valueChanges.pipe(
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
      this.animationSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.animationSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: ChartAnimationSettings): void {
    this.modelValue = value;
    this.animationSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
    this.animationSettingsFormGroup.get('animation').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((animation) => {
      this.settingsExpanded = animation;
    });
  }

  private updateValidators() {
    const animation: boolean = this.animationSettingsFormGroup.get('animation').value;
    if (animation) {
      this.animationSettingsFormGroup.enable({emitEvent: false});
    } else {
      this.animationSettingsFormGroup.disable({emitEvent: false});
      this.animationSettingsFormGroup.get('animation').enable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = this.animationSettingsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }
}
