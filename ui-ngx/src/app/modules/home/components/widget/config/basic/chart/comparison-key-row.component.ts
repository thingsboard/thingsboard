// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
