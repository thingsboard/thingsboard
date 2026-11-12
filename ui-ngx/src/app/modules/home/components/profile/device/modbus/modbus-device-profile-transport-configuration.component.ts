// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, forwardRef, inject, Input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { DeviceTransportType } from '@shared/models/device.models';
import {
  ModbusByteOrder,
  ModbusDeviceProfileTransportConfiguration,
  ModbusMappingSection
} from '@shared/models/modbus.models';
import { isDefinedAndNotNull } from '@core/utils';

const INTEGER_PATTERN = '[0-9]*';

/** Profile part of the Modbus transport: timing, byte/word order and the four mapping lists (register map). */
@Component({
  selector: 'tb-modbus-device-profile-transport-configuration',
  templateUrl: './modbus-device-profile-transport-configuration.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ModbusDeviceProfileTransportConfigurationComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ModbusDeviceProfileTransportConfigurationComponent),
      multi: true
    }
  ],
  standalone: false
})
export class ModbusDeviceProfileTransportConfigurationComponent implements ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  readonly byteOrders = Object.values(ModbusByteOrder);
  readonly sections = [
    {section: ModbusMappingSection.TIMESERIES, title: 'device-profile.modbus.timeseries', hint: 'device-profile.modbus.timeseries-hint'},
    {section: ModbusMappingSection.ATTRIBUTES, title: 'device-profile.modbus.attributes', hint: 'device-profile.modbus.attributes-hint'},
    {
      section: ModbusMappingSection.ATTRIBUTE_UPDATES,
      title: 'device-profile.modbus.attribute-updates',
      hint: 'device-profile.modbus.attribute-updates-hint'
    },
    {section: ModbusMappingSection.RPC, title: 'device-profile.modbus.rpc', hint: 'device-profile.modbus.rpc-hint'}
  ];

  modbusConfigurationForm: UntypedFormGroup;

  private readonly destroyRef = inject(DestroyRef);
  private propagateChange = (_: any) => {};

  constructor(private fb: UntypedFormBuilder) {
    this.modbusConfigurationForm = this.fb.group({
      timeoutMs: [1000, [Validators.required, Validators.min(1), Validators.pattern(INTEGER_PATTERN)]],
      retries: [1, [Validators.required, Validators.min(0), Validators.pattern(INTEGER_PATTERN)]],
      pollPeriodMs: [5000, [Validators.required, Validators.min(1), Validators.pattern(INTEGER_PATTERN)]],
      attributesPollPeriodMs: [null, [Validators.min(1), Validators.pattern(INTEGER_PATTERN)]],
      byteOrder: [ModbusByteOrder.BIG, Validators.required],
      wordOrder: [ModbusByteOrder.BIG, Validators.required],
      sendDataOnlyOnChange: [false],
      timeseries: [[]],
      attributes: [[]],
      attributeUpdates: [[]],
      rpc: [[]]
    });
    this.modbusConfigurationForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateModel());
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.modbusConfigurationForm.disable({emitEvent: false});
    } else {
      this.modbusConfigurationForm.enable({emitEvent: false});
    }
  }

  writeValue(value: ModbusDeviceProfileTransportConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      this.modbusConfigurationForm.patchValue({
        ...value,
        timeseries: value.timeseries || [],
        attributes: value.attributes || [],
        attributeUpdates: value.attributeUpdates || [],
        rpc: value.rpc || []
      }, {emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.modbusConfigurationForm.valid || this.modbusConfigurationForm.disabled ? null : {
      modbusDeviceProfileTransportConfiguration: false
    };
  }

  private updateModel(): void {
    const value = this.modbusConfigurationForm.getRawValue();
    const configuration = {
      ...value,
      timeoutMs: toNumber(value.timeoutMs),
      retries: toNumber(value.retries),
      pollPeriodMs: toNumber(value.pollPeriodMs),
      attributesPollPeriodMs: toNumber(value.attributesPollPeriodMs),
      type: DeviceTransportType.MODBUS
    };
    this.propagateChange(configuration);
  }

}

const toNumber = (value: any): number => isDefinedAndNotNull(value) && value !== '' ? Number(value) : null;
