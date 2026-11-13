// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, forwardRef, inject, Input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
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
import { DeviceTransportConfiguration, DeviceTransportType } from '@shared/models/device.models';
import { ModbusDeviceTransportConfiguration, ModbusTcpFraming, modbusUnitIdRange } from '@shared/models/modbus.models';
import { isDefinedAndNotNull } from '@core/utils';

const INTEGER_PATTERN = '[0-9]*';

/** Device part of the Modbus transport: where the device is reached (host, port, framing), its unit id and optional overrides. */
@Component({
  selector: 'tb-modbus-device-transport-configuration',
  templateUrl: './modbus-device-transport-configuration.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ModbusDeviceTransportConfigurationComponent),
      multi: true
    }, {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ModbusDeviceTransportConfigurationComponent),
      multi: true
    }
  ],
  standalone: false
})
export class ModbusDeviceTransportConfigurationComponent implements ControlValueAccessor, Validator {

  readonly framings = Object.values(ModbusTcpFraming);

  modbusDeviceForm: UntypedFormGroup;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  private readonly destroyRef = inject(DestroyRef);
  private propagateChange = (_: any) => {};

  constructor(private fb: UntypedFormBuilder) {
    this.modbusDeviceForm = this.fb.group({
      host: ['localhost', [Validators.required, Validators.pattern('(.|\\s)*\\S(.|\\s)*')]],
      port: [502, [Validators.required, Validators.min(1), Validators.max(65535), Validators.pattern(INTEGER_PATTERN)]],
      framing: [ModbusTcpFraming.MBAP, Validators.required],
      unitId: [1],
      timeoutMs: [null, [Validators.min(1), Validators.pattern(INTEGER_PATTERN)]],
      pollPeriodMs: [null, [Validators.min(1), Validators.pattern(INTEGER_PATTERN)]]
    });
    this.updateUnitIdValidators(ModbusTcpFraming.MBAP);
    this.modbusDeviceForm.get('framing').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((framing: ModbusTcpFraming) => this.updateUnitIdValidators(framing));
    this.modbusDeviceForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateModel());
  }

  get unitIdRange(): { min: number; max: number } {
    return modbusUnitIdRange.get(this.modbusDeviceForm.get('framing').value) ?? modbusUnitIdRange.get(ModbusTcpFraming.MBAP);
  }

  /** Unit id 0 is valid for Modbus TCP, but a TCP-to-serial gateway may forward it to the bus as a broadcast. */
  get showUnitIdZeroHint(): boolean {
    const unitId = this.modbusDeviceForm.get('unitId').value;
    return this.modbusDeviceForm.get('framing').value === ModbusTcpFraming.MBAP && unitId !== null && unitId !== '' && Number(unitId) === 0;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.modbusDeviceForm.disable({emitEvent: false});
    } else {
      this.modbusDeviceForm.enable({emitEvent: false});
    }
  }

  writeValue(value: ModbusDeviceTransportConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      const framing = value.endpoint?.framing ?? ModbusTcpFraming.MBAP;
      this.modbusDeviceForm.patchValue({
        host: value.endpoint?.host ?? 'localhost',
        port: value.endpoint?.port ?? 502,
        framing,
        unitId: value.unitId ?? 1,
        timeoutMs: value.timeoutMs ?? null,
        pollPeriodMs: value.pollPeriodMs ?? null
      }, {emitEvent: false});
      this.updateUnitIdValidators(framing);
    }
  }

  validate(): ValidationErrors | null {
    return this.modbusDeviceForm.valid || this.modbusDeviceForm.disabled ? null : {
      modbusDeviceTransportConfiguration: false
    };
  }

  private updateUnitIdValidators(framing: ModbusTcpFraming): void {
    const range = modbusUnitIdRange.get(framing) ?? modbusUnitIdRange.get(ModbusTcpFraming.MBAP);
    const unitIdControl = this.modbusDeviceForm.get('unitId');
    unitIdControl.setValidators([Validators.required, Validators.min(range.min), Validators.max(range.max), Validators.pattern(INTEGER_PATTERN)]);
    unitIdControl.updateValueAndValidity({emitEvent: false});
  }

  private updateModel(): void {
    let configuration: DeviceTransportConfiguration = null;
    if (this.modbusDeviceForm.valid) {
      const value = this.modbusDeviceForm.getRawValue();
      configuration = {
        type: DeviceTransportType.MODBUS,
        endpoint: {type: 'TCP', host: value.host.trim(), port: Number(value.port), framing: value.framing},
        unitId: Number(value.unitId),
        timeoutMs: toNumber(value.timeoutMs),
        pollPeriodMs: toNumber(value.pollPeriodMs)
      };
    }
    this.propagateChange(configuration);
  }

}

const toNumber = (value: any): number => isDefinedAndNotNull(value) && value !== '' ? Number(value) : null;
