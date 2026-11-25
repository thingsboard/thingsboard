// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, forwardRef, inject, Input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { merge } from 'rxjs';
import {
  isModbusNumericType,
  isModbusSizedType,
  MODBUS_MAX_ADDRESS,
  ModbusDataType,
  modbusDataTypeRegisters,
  ModbusFunctionCode,
  modbusFunctionCodeInfoMap,
  ModbusMapping,
  modbusMappingNeedsBit,
  ModbusMappingSection,
  modbusSectionAllowsFunctionCode,
  modbusTypeAllowsFunctionCode
} from '@shared/models/modbus.models';
import { isDefinedAndNotNull, isNotEmptyStr } from '@core/utils';
import { GtSmBreakpointAwareDirective } from '@shared/components/gt-sm-breakpoint-aware.directive';

const INTEGER_PATTERN = '[0-9]*';

/**
 * Editable table of the mappings of one list (timeseries, attributes, attribute updates or RPC).
 * Mirrors the backend ModbusMapping.validate rules per row, so mistakes show next to the field instead of on save:
 * function codes are limited by the list and the data type, the register count follows the type unless the type is
 * STRING or BYTES, the bit is asked for only for a BITS value inside a register, the multiplier only for numbers.
 */
@Component({
  selector: 'tb-modbus-device-profile-mappings',
  templateUrl: './modbus-device-profile-mappings.component.html',
  styleUrls: ['./modbus-device-profile-mappings.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ModbusDeviceProfileMappingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ModbusDeviceProfileMappingsComponent),
      multi: true
    }
  ],
  standalone: false
})
export class ModbusDeviceProfileMappingsComponent extends GtSmBreakpointAwareDirective implements ControlValueAccessor, Validator {

  @Input()
  section: ModbusMappingSection;

  @Input()
  disabled: boolean;

  readonly dataTypes = Object.values(ModbusDataType);
  readonly functionCodeNames = new Map([...modbusFunctionCodeInfoMap].map(([code, info]) => [code, info.name]));

  mappingsForm: UntypedFormGroup;

  private readonly destroyRef = inject(DestroyRef);
  private propagateChange = (_: any) => {};

  constructor(private fb: UntypedFormBuilder) {
    super();
    this.mappingsForm = this.fb.group({
      mappings: this.fb.array([], uniqueTagsValidator)
    });
    this.mappingsForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateModel());
  }

  get mappingsFormArray(): UntypedFormArray {
    return this.mappingsForm.get('mappings') as UntypedFormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.mappingsForm.disable({emitEvent: false});
    } else {
      this.mappingsForm.enable({emitEvent: false});
      this.mappingsFormArray.controls.forEach(row => this.updateRowControls(row as UntypedFormGroup));
    }
  }

  writeValue(mappings: ModbusMapping[] | null): void {
    const rows = (mappings || []).map(mapping => this.createRow(mapping));
    this.mappingsForm.setControl('mappings', this.fb.array(rows, uniqueTagsValidator), {emitEvent: false});
    if (this.disabled) {
      this.mappingsForm.disable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.mappingsForm.valid || this.mappingsForm.disabled ? null : {modbusMappings: false};
  }

  addMapping(): void {
    this.mappingsFormArray.push(this.createRow());
  }

  removeMapping(index: number): void {
    this.mappingsFormArray.removeAt(index);
  }

  /** Function codes offered for a row: allowed in this list and for the row's data type. */
  functionCodesFor(row: AbstractControl): ModbusFunctionCode[] {
    const type: ModbusDataType = row.get('type').value;
    return this.allowedFunctionCodes(type);
  }

  maxCountFor(row: AbstractControl): number {
    const functionCode: ModbusFunctionCode = row.get('functionCode').value;
    return functionCode ? modbusFunctionCodeInfoMap.get(functionCode).maxCount : null;
  }

  private allowedFunctionCodes(type: ModbusDataType): ModbusFunctionCode[] {
    return [...modbusFunctionCodeInfoMap.keys()].filter(functionCode =>
      modbusSectionAllowsFunctionCode(this.section, functionCode) && (!type || modbusTypeAllowsFunctionCode(type, functionCode)));
  }

  private createRow(mapping?: ModbusMapping): UntypedFormGroup {
    const type = mapping?.type ?? ModbusDataType.UINT16;
    const row = this.fb.group({
      tag: [mapping?.tag ?? '', [Validators.required, Validators.pattern('(.|\\s)*\\S(.|\\s)*')]],
      type: [type, Validators.required],
      functionCode: [mapping?.functionCode ?? this.allowedFunctionCodes(type)[0] ?? null, Validators.required],
      address: [mapping?.address ?? 0,
        [Validators.required, Validators.min(0), Validators.max(MODBUS_MAX_ADDRESS), Validators.pattern(INTEGER_PATTERN)]],
      objectsCount: [mapping?.objectsCount ?? null],
      bit: [mapping?.bit ?? null],
      multiplier: [mapping?.multiplier ?? null, nonZeroNumberValidator]
    }, {validators: addressRangeValidator});
    this.updateRowControls(row);
    merge(row.get('type').valueChanges, row.get('functionCode').valueChanges).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateRowControls(row));
    return row;
  }

  /** Enables, disables and validates the dependent fields of a row after its type or function code changed. */
  private updateRowControls(row: UntypedFormGroup): void {
    if (this.disabled) {
      return;
    }
    const type: ModbusDataType = row.get('type').value;
    const functionCodeControl = row.get('functionCode');
    const allowed = this.allowedFunctionCodes(type);
    if (functionCodeControl.value && !allowed.includes(functionCodeControl.value)) {
      functionCodeControl.setValue(allowed[0] ?? null, {emitEvent: false});
    }
    const functionCode: ModbusFunctionCode = functionCodeControl.value;

    const countControl = row.get('objectsCount');
    if (isModbusSizedType(type)) {
      const maxCount = functionCode ? modbusFunctionCodeInfoMap.get(functionCode).maxCount : 1;
      countControl.setValidators([Validators.required, Validators.min(1), Validators.max(maxCount), Validators.pattern(INTEGER_PATTERN)]);
      countControl.enable({emitEvent: false});
    } else {
      countControl.clearValidators();
      countControl.setValue(isModbusNumericType(type) ? modbusDataTypeRegisters.get(type) : 1, {emitEvent: false});
      countControl.disable({emitEvent: false});
    }

    const bitControl = row.get('bit');
    if (modbusMappingNeedsBit(type, functionCode)) {
      bitControl.setValidators([Validators.required, Validators.min(0), Validators.max(15), Validators.pattern(INTEGER_PATTERN)]);
      bitControl.enable({emitEvent: false});
    } else {
      bitControl.clearValidators();
      bitControl.setValue(null, {emitEvent: false});
      bitControl.disable({emitEvent: false});
    }

    const multiplierControl = row.get('multiplier');
    if (isModbusNumericType(type)) {
      multiplierControl.enable({emitEvent: false});
    } else {
      multiplierControl.setValue(null, {emitEvent: false});
      multiplierControl.disable({emitEvent: false});
    }

    countControl.updateValueAndValidity({emitEvent: false});
    bitControl.updateValueAndValidity({emitEvent: false});
    row.updateValueAndValidity({emitEvent: false});
  }

  /** Only the fields the backend expects for the row's type: the count only when the user sets it, the bit only when needed. */
  private updateModel(): void {
    const mappings: ModbusMapping[] = this.mappingsFormArray.controls.map(row => {
      const value = (row as UntypedFormGroup).getRawValue();
      const mapping: ModbusMapping = {
        tag: isNotEmptyStr(value.tag) ? value.tag.trim() : value.tag,
        type: value.type,
        functionCode: value.functionCode,
        address: toNumber(value.address)
      };
      if (isModbusSizedType(value.type)) {
        mapping.objectsCount = toNumber(value.objectsCount);
      }
      if (modbusMappingNeedsBit(value.type, value.functionCode)) {
        mapping.bit = toNumber(value.bit);
      }
      if (isModbusNumericType(value.type) && isDefinedAndNotNull(value.multiplier) && value.multiplier !== '') {
        mapping.multiplier = toNumber(value.multiplier);
      }
      return mapping;
    });
    this.propagateChange(mappings);
  }

}

const toNumber = (value: any): number => isDefinedAndNotNull(value) && value !== '' ? Number(value) : null;

const nonZeroNumberValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value;
  if (!isDefinedAndNotNull(value) || value === '') {
    return null;
  }
  const number = Number(value);
  return Number.isFinite(number) && number !== 0 ? null : {nonZeroNumber: true};
};

/** The mapped range must end at or before the last address (0xFFFF). */
const addressRangeValidator: ValidatorFn = (row: AbstractControl): ValidationErrors | null => {
  const address = toNumber(row.get('address')?.value);
  const count = toNumber(row.get('objectsCount')?.value) ?? 1;
  if (address === null || !Number.isInteger(address) || address < 0) {
    return null;
  }
  return address + count - 1 > MODBUS_MAX_ADDRESS ? {addressRange: true} : null;
};

/** The same key twice in one list would make one of the values unreachable. */
const uniqueTagsValidator: ValidatorFn = (array: AbstractControl): ValidationErrors | null => {
  const rows = (array as UntypedFormArray).controls;
  const seen = new Map<string, AbstractControl>();
  let duplicate = false;
  rows.forEach(row => {
    const tagControl = row.get('tag');
    const tag = (tagControl.value || '').trim();
    const errors = {...(tagControl.errors || {})};
    delete errors.duplicate;
    if (tag && seen.has(tag)) {
      errors.duplicate = true;
      duplicate = true;
    }
    if (tag && !seen.has(tag)) {
      seen.set(tag, tagControl);
    }
    tagControl.setErrors(Object.keys(errors).length ? errors : null);
  });
  return duplicate ? {duplicateTags: true} : null;
};
