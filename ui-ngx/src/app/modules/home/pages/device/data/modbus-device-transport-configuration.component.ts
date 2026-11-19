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
import {
  MODBUS_RS485_MAX_DELAY_MS,
  ModbusDeviceTransportConfiguration,
  ModbusEndpointType,
  ModbusSerialEndpoint,
  ModbusSerialParity,
  ModbusSerialStopBits,
  ModbusTcpEndpoint,
  ModbusTcpFraming,
  modbusUnitIdRangeOf
} from '@shared/models/modbus.models';
import { isDefinedAndNotNull } from '@core/utils';
import { merge } from 'rxjs';

const INTEGER_PATTERN = '[0-9]*';
const NOT_BLANK_PATTERN = '(.|\\s)*\\S(.|\\s)*';

/** Device part of the Modbus transport: where the device is reached (TCP host or serial port), its unit id and optional overrides. */
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

  readonly ModbusEndpointType = ModbusEndpointType;
  readonly rs485MaxDelayMs = MODBUS_RS485_MAX_DELAY_MS;
  readonly endpointTypes = Object.values(ModbusEndpointType);
  readonly framings = Object.values(ModbusTcpFraming);
  readonly parities = Object.values(ModbusSerialParity);
  readonly stopBits = Object.values(ModbusSerialStopBits);

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
      endpointType: [ModbusEndpointType.TCP, Validators.required],
      host: ['localhost', [Validators.required, Validators.pattern(NOT_BLANK_PATTERN)]],
      port: [502, [Validators.required, Validators.min(1), Validators.max(65535), Validators.pattern(INTEGER_PATTERN)]],
      framing: [ModbusTcpFraming.MBAP, Validators.required],
      serial: this.fb.group({
        port: ['', [Validators.required, Validators.pattern(NOT_BLANK_PATTERN)]],
        baudRate: [9600, [Validators.required, Validators.min(1), Validators.pattern(INTEGER_PATTERN)]],
        parity: [ModbusSerialParity.EVEN, Validators.required],
        stopBits: [ModbusSerialStopBits.ONE, Validators.required]
      }),
      rs485Enabled: [false],
      rs485: this.fb.group({
        rtsActiveHigh: [true],
        termination: [false],
        rxDuringTx: [false],
        delayBeforeMs: [0, [Validators.required, Validators.min(0), Validators.max(MODBUS_RS485_MAX_DELAY_MS), Validators.pattern(INTEGER_PATTERN)]],
        delayAfterMs: [0, [Validators.required, Validators.min(0), Validators.max(MODBUS_RS485_MAX_DELAY_MS), Validators.pattern(INTEGER_PATTERN)]]
      }),
      unitId: [1],
      timeoutMs: [null, [Validators.min(1), Validators.pattern(INTEGER_PATTERN)]],
      pollPeriodMs: [null, [Validators.min(1), Validators.pattern(INTEGER_PATTERN)]]
    });
    this.updateEndpointControls();
    merge(
      this.modbusDeviceForm.get('endpointType').valueChanges,
      this.modbusDeviceForm.get('rs485Enabled').valueChanges,
      this.modbusDeviceForm.get('framing').valueChanges
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateEndpointControls());
    this.modbusDeviceForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateModel());
  }

  get endpointType(): ModbusEndpointType {
    return this.modbusDeviceForm.get('endpointType').value;
  }

  get rs485Enabled(): boolean {
    return this.modbusDeviceForm.get('rs485Enabled').value;
  }

  get unitIdRange(): { min: number; max: number } {
    return modbusUnitIdRangeOf(this.endpointType, this.modbusDeviceForm.get('framing').value);
  }

  /** Unit id 0 is valid for Modbus TCP, but a TCP-to-serial gateway may forward it to the bus as a broadcast. */
  get showUnitIdZeroHint(): boolean {
    const unitId = this.modbusDeviceForm.get('unitId').value;
    return this.unitIdRange.min === 0 && unitId !== null && unitId !== '' && Number(unitId) === 0;
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
      this.updateEndpointControls();
    }
  }

  writeValue(value: ModbusDeviceTransportConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      const common = {
        unitId: value.unitId ?? 1,
        timeoutMs: value.timeoutMs ?? null,
        pollPeriodMs: value.pollPeriodMs ?? null
      };
      if (value.endpoint?.type === ModbusEndpointType.SERIAL) {
        const endpoint = value.endpoint as ModbusSerialEndpoint;
        this.modbusDeviceForm.patchValue({
          ...common,
          endpointType: ModbusEndpointType.SERIAL,
          serial: {
            port: endpoint.port ?? '',
            baudRate: endpoint.baudRate ?? 9600,
            parity: endpoint.parity ?? ModbusSerialParity.EVEN,
            stopBits: endpoint.stopBits ?? ModbusSerialStopBits.ONE
          },
          rs485Enabled: isDefinedAndNotNull(endpoint.rs485),
          rs485: endpoint.rs485 ?? {}
        }, {emitEvent: false});
      } else {
        const endpoint = value.endpoint as ModbusTcpEndpoint;
        this.modbusDeviceForm.patchValue({
          ...common,
          endpointType: ModbusEndpointType.TCP,
          host: endpoint?.host ?? 'localhost',
          port: endpoint?.port ?? 502,
          framing: endpoint?.framing ?? ModbusTcpFraming.MBAP
        }, {emitEvent: false});
      }
      if (!this.disabled) {
        this.updateEndpointControls();
      }
    }
  }

  validate(): ValidationErrors | null {
    return this.modbusDeviceForm.valid || this.modbusDeviceForm.disabled ? null : {
      modbusDeviceTransportConfiguration: false
    };
  }

  /** Only the fields of the selected connection take part in validation and in the value; the others keep what was typed. */
  private updateEndpointControls(): void {
    const serial = this.endpointType === ModbusEndpointType.SERIAL;
    const toggle = (name: string, enabled: boolean) => {
      const control = this.modbusDeviceForm.get(name);
      if (enabled) {
        control.enable({emitEvent: false});
      } else {
        control.disable({emitEvent: false});
      }
    };
    ['host', 'port', 'framing'].forEach(name => toggle(name, !serial));
    ['serial', 'rs485Enabled'].forEach(name => toggle(name, serial));
    toggle('rs485', serial && this.rs485Enabled);

    const range = this.unitIdRange;
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
        endpoint: this.endpointOf(value),
        unitId: Number(value.unitId),
        timeoutMs: toNumber(value.timeoutMs),
        pollPeriodMs: toNumber(value.pollPeriodMs)
      };
    }
    this.propagateChange(configuration);
  }

  private endpointOf(value: any): ModbusTcpEndpoint | ModbusSerialEndpoint {
    if (value.endpointType !== ModbusEndpointType.SERIAL) {
      return {type: 'TCP', host: value.host.trim(), port: Number(value.port), framing: value.framing};
    }
    const endpoint: ModbusSerialEndpoint = {
      type: 'SERIAL',
      port: value.serial.port.trim(),
      baudRate: Number(value.serial.baudRate),
      parity: value.serial.parity,
      stopBits: value.serial.stopBits
    };
    if (value.rs485Enabled) {
      endpoint.rs485 = {
        rtsActiveHigh: value.rs485.rtsActiveHigh,
        termination: value.rs485.termination,
        rxDuringTx: value.rs485.rxDuringTx,
        delayBeforeMs: Number(value.rs485.delayBeforeMs),
        delayAfterMs: Number(value.rs485.delayAfterMs)
      };
    }
    return endpoint;
  }
}

const toNumber = (value: any): number => isDefinedAndNotNull(value) && value !== '' ? Number(value) : null;
