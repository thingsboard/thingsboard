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
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  ValidatorFn,
  Validator,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  LocationKey,
  locationKeyDefaultLabelMap,
  locationKeyDefaultValueTypeMap,
  LocationKeyMapping,
  locationKeyMapping,
  locationKeyName,
  locationKeyTranslationMap,
  LocationKeyValueType,
  locationKeyValueTypeTranslationMap,
  mandatoryLocationKeys
} from '@shared/models/widget.models';

@Component({
    selector: 'tb-location-keys-table',
    templateUrl: './location-keys-table.component.html',
    styleUrls: ['./location-keys-table.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => LocationKeysTableComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => LocationKeysTableComponent),
            multi: true
        }
    ],
    standalone: false
})
export class LocationKeysTableComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  disabled: boolean;

  @Input()
  panelTitle: string;

  @Input()
  availableKeys: LocationKey[] = [];

  locationKeyTranslations = locationKeyTranslationMap;
  locationKeyDefaultLabels = locationKeyDefaultLabelMap;

  valueTypes = Object.values(LocationKeyValueType);
  valueTypeTranslations = locationKeyValueTypeTranslationMap;

  keysFormGroup: FormGroup;

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.keysFormGroup = this.fb.group({
      keys: this.fb.array([], [this.uniqueKeyNamesValidator])
    });
    this.keysFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateModel());
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.keysFormGroup.disable({emitEvent: false});
    } else {
      this.keysFormGroup.enable({emitEvent: false});
      this.disableMandatoryKeyControls();
    }
  }

  writeValue(value: LocationKeyMapping[] | undefined): void {
    this.keysFormGroup.setControl('keys', this.prepareKeysFormArray(value), {emitEvent: false});
    this.disableMandatoryKeyControls();
  }

  validate(): ValidationErrors | null {
    return this.keysFormGroup.valid ? null : {
      locationKeys: {
        valid: false
      }
    };
  }

  keysFormArray(): FormArray {
    return this.keysFormGroup.get('keys') as FormArray;
  }

  isMandatory(keyControl: AbstractControl): boolean {
    return mandatoryLocationKeys.includes(keyControl.get('key').value);
  }

  keyOptions(keyControl: AbstractControl): LocationKey[] {
    const usedKeys: LocationKey[] = this.keysFormArray().getRawValue().map(mapping => mapping.key);
    const currentKey: LocationKey = keyControl.get('key').value;
    return this.availableKeys.filter(key => key === currentKey || !usedKeys.includes(key));
  }

  get addKeyDisabled(): boolean {
    return this.disabled || this.keysFormArray().length >= this.availableKeys.length;
  }

  addKey(): void {
    const usedKeys: LocationKey[] = this.keysFormArray().getRawValue().map(mapping => mapping.key);
    const key = this.availableKeys.find(available => !usedKeys.includes(available));
    if (key) {
      this.keysFormArray().push(this.keyControl(locationKeyMapping(key)));
    }
  }

  removeKey(index: number): void {
    this.keysFormArray().removeAt(index);
  }

  keyChanged(keyControl: AbstractControl): void {
    const key: LocationKey = keyControl.get('key').value;
    keyControl.get('valueType').patchValue(locationKeyDefaultValueTypeMap.get(key));
  }

  private prepareKeysFormArray(value: LocationKeyMapping[] | undefined): FormArray {
    const mappings = (value || []).filter(mapping => this.availableKeys.includes(mapping?.key));
    mandatoryLocationKeys.filter(key => !mappings.some(mapping => mapping.key === key))
      .forEach((key, index) => mappings.splice(index, 0, locationKeyMapping(key)));
    return this.fb.array(mappings.map(mapping => this.keyControl(mapping)), [this.uniqueKeyNamesValidator]);
  }

  private keyControl(mapping: LocationKeyMapping): FormGroup {
    return this.fb.group({
      key: [mapping.key, [Validators.required]],
      label: [mapping.label ?? ''],
      valueType: [mapping.valueType ?? locationKeyDefaultValueTypeMap.get(mapping.key), [Validators.required]]
    });
  }

  private disableMandatoryKeyControls(): void {
    this.keysFormArray().controls.filter(keyControl => this.isMandatory(keyControl))
      .forEach(keyControl => keyControl.get('key').disable({emitEvent: false}));
  }

  private uniqueKeyNamesValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
    const names = (control as FormArray).getRawValue().map(mapping => locationKeyName(mapping));
    return names.length === new Set(names).size ? null : {duplicateKeyNames: true};
  };

  private updateModel(): void {
    this.propagateChange(this.keysFormGroup.valid ? this.keysFormArray().getRawValue() : null);
  }
}
