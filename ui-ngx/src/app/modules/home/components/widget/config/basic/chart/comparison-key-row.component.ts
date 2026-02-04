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

import { ChangeDetectorRef, Component, DestroyRef, forwardRef, Input, OnInit, ViewEncapsulation } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup
} from '@angular/forms';
import {
  DataKey,
  DataKeyComparisonSettings,
  DataKeySettingsWithComparison,
  DatasourceType
} from '@shared/models/widget.models';
import { deepClone } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-comparison-key-row',
    templateUrl: './comparison-key-row.component.html',
    styleUrls: ['./comparison-key-row.component.scss', '../../../lib/settings/common/key/data-keys.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ComparisonKeyRowComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ComparisonKeyRowComponent implements ControlValueAccessor, OnInit {

  @Input()
  disabled: boolean;

  @Input()
  datasourceType: DatasourceType;

  keyFormControl: UntypedFormControl;

  keyRowFormGroup: UntypedFormGroup;

  modelValue: DataKey;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.keyFormControl = this.fb.control(null, []);
    this.keyRowFormGroup = this.fb.group({
      showValuesForComparison: [null, []],
      comparisonValuesLabel: [null, []],
      color: [null, []]
    });
    this.keyRowFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
    this.keyRowFormGroup.get('showValuesForComparison').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateValidators());
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.keyFormControl.disable({emitEvent: false});
      this.keyRowFormGroup.disable({emitEvent: false});
    } else {
      this.keyFormControl.enable({emitEvent: false});
      this.keyRowFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: DataKey): void {
    this.modelValue = value;
    const comparisonSettings = (value?.settings as DataKeySettingsWithComparison)?.comparisonSettings;
    this.keyRowFormGroup.patchValue(
      comparisonSettings, {emitEvent: false}
    );
    this.keyFormControl.patchValue(deepClone(this.modelValue), {emitEvent: false});
    this.updateValidators();
    this.cd.markForCheck();
  }

  private updateValidators() {
    const showValuesForComparison: boolean = this.keyRowFormGroup.get('showValuesForComparison').value;
    if (showValuesForComparison) {
      this.keyFormControl.enable({emitEvent: false});
      this.keyRowFormGroup.get('comparisonValuesLabel').enable({emitEvent: false});
      this.keyRowFormGroup.get('color').enable({emitEvent: false});
    } else {
      this.keyFormControl.disable({emitEvent: false});
      this.keyRowFormGroup.get('comparisonValuesLabel').disable({emitEvent: false});
      this.keyRowFormGroup.get('color').disable({emitEvent: false});
    }
  }

  private updateModel() {
    const comparisonSettings: DataKeyComparisonSettings = this.keyRowFormGroup.value;
    if (!this.modelValue.settings) {
      this.modelValue.settings = {};
    }
    this.modelValue.settings.comparisonSettings = comparisonSettings;
    this.propagateChange(this.modelValue);
  }

}
