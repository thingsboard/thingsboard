// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
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
import { isEqual, isNotEmptyStr } from '@core/utils';
import {
  LocationKey,
  locationKeyDefaultNameMap,
  locationKeyDefaultValueTypeMap,
  LocationKeyMapping,
  locationKeyMapping,
  locationKeyName,
  locationKeyTranslationMap,
  LocationKeyValueType,
  locationKeyValueTypeTranslationMap,
  mandatoryLocationKeys
} from '@shared/models/location.models';

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
  locationKeyDefaultNames = locationKeyDefaultNameMap;

  valueTypes = Object.values(LocationKeyValueType);
  valueTypeTranslations = locationKeyValueTypeTranslationMap;

  keysFormGroup: FormGroup;

  private propagateChange = (_val: any) => {};
  private changeRegistered = false;
  private normalizationPending = false;

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
    this.changeRegistered = true;
    this.flushNormalization();
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
    this.normalizationPending = !isEqual(this.keysFormArray().getRawValue(), value);
    this.flushNormalization();
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
    return mandatoryLocationKeys.includes(keyControl.get('argument').value);
  }

  keyOptions(keyControl: AbstractControl): LocationKey[] {
    const usedArguments: LocationKey[] = this.keysFormArray().getRawValue().map(mapping => mapping.argument);
    const currentArgument: LocationKey = keyControl.get('argument').value;
    return this.availableKeys.filter(argument => argument === currentArgument || !usedArguments.includes(argument));
  }

  get addKeyDisabled(): boolean {
    return this.disabled || this.keysFormArray().length >= this.availableKeys.length;
  }

  addKey(): void {
    const usedArguments: LocationKey[] = this.keysFormArray().getRawValue().map(mapping => mapping.argument);
    const argument = this.availableKeys.find(available => !usedArguments.includes(available));
    if (argument) {
      this.keysFormArray().push(this.keyControl(locationKeyMapping(argument)));
    }
  }

  removeKey(index: number): void {
    this.keysFormArray().removeAt(index);
  }

  keyChanged(keyControl: AbstractControl): void {
    const argument: LocationKey = keyControl.get('argument').value;
    keyControl.get('keyName').patchValue(locationKeyDefaultNameMap.get(argument));
    keyControl.get('valueType').patchValue(locationKeyDefaultValueTypeMap.get(argument));
  }

  private prepareKeysFormArray(value: LocationKeyMapping[] | undefined): FormArray {
    const mappings = (value || []).filter(mapping => this.availableKeys.includes(mapping?.argument));
    mandatoryLocationKeys.filter(argument => !mappings.some(mapping => mapping.argument === argument))
      .forEach((argument) => mappings.splice(mandatoryLocationKeys.indexOf(argument), 0,
        locationKeyMapping(argument)));
    return this.fb.array(mappings.map(mapping => this.keyControl(mapping)), [this.uniqueKeyNamesValidator]);
  }

  private keyControl(mapping: LocationKeyMapping): FormGroup {
    return this.fb.group({
      argument: [mapping.argument, [Validators.required]],
      keyName: [isNotEmptyStr(mapping.keyName) ? mapping.keyName : locationKeyDefaultNameMap.get(mapping.argument)],
      valueType: [mapping.valueType ?? locationKeyDefaultValueTypeMap.get(mapping.argument), [Validators.required]]
    });
  }

  private disableMandatoryKeyControls(): void {
    this.keysFormArray().controls.filter(keyControl => this.isMandatory(keyControl))
      .forEach(keyControl => keyControl.get('argument').disable({emitEvent: false}));
  }

  private uniqueKeyNamesValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
    const names = (control as FormArray).getRawValue().map(mapping => locationKeyName(mapping));
    return names.length === new Set(names).size ? null : {duplicateKeyNames: true};
  };

  private flushNormalization(): void {
    if (this.normalizationPending && this.changeRegistered) {
      this.normalizationPending = false;
      this.updateModel();
    }
  }

  private updateModel(): void {
    this.propagateChange(this.keysFormGroup.valid ? this.keysFormArray().getRawValue() : null);
  }
}
