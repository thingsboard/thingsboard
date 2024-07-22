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

import { ChangeDetectionStrategy, Component, forwardRef, OnDestroy } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormControl,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators,
} from '@angular/forms';
import {
  ModbusMethodLabelsMap,
  ModbusMethodType,
  ModbusOrderType,
  ModbusProtocolLabelsMap,
  ModbusProtocolType,
  ModbusRegisterValues,
  ModbusSlave,
  noLeadTrailSpacesRegex,
  PortLimits,
  SlaveConfig,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { Subject } from 'rxjs';
import { startWith, takeUntil } from 'rxjs/operators';
import { GatewayPortTooltipPipe } from '@home/pipes/public-api';
import { ModbusSecurityConfigComponent } from '../modbus-security-config/modbus-security-config.component';
import { ModbusValuesComponent, } from '../modbus-values/modbus-values.component';

@Component({
  selector: 'tb-modbus-slave-config',
  templateUrl: './modbus-slave-config.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ModbusSlaveConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ModbusSlaveConfigComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    ModbusValuesComponent,
    ModbusSecurityConfigComponent,
    GatewayPortTooltipPipe,
  ],
  styles: [`
    :host {
      .nested-expansion-header {
        .mat-content {
          height: 100%;
        }
      }
    }
  `],
})
export class ModbusSlaveConfigComponent implements ControlValueAccessor, Validator, OnDestroy {

  slaveConfigFormGroup: UntypedFormGroup;
  showSecurityControl: UntypedFormControl;
  ModbusProtocolLabelsMap = ModbusProtocolLabelsMap;
  ModbusMethodLabelsMap = ModbusMethodLabelsMap;
  portLimits = PortLimits;

  readonly modbusProtocolTypes = Object.values(ModbusProtocolType);
  readonly modbusMethodTypes = Object.values(ModbusMethodType);
  readonly modbusOrderType = Object.values(ModbusOrderType);
  readonly ModbusProtocolType = ModbusProtocolType;
  readonly serialSpecificControlKeys = ['serialPort', 'baudrate'];
  readonly tcpUdpSpecificControlKeys = ['port', 'security', 'host'];

  private onChange: (value: SlaveConfig) => void;
  private onTouched: () => void;

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder) {
    this.showSecurityControl = this.fb.control(false);
    this.slaveConfigFormGroup = this.fb.group({
      type: [ModbusProtocolType.TCP, []],
      host: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      port: [null, [Validators.required, Validators.min(PortLimits.MIN), Validators.max(PortLimits.MAX)]],
      serialPort: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      method: [ModbusMethodType.SOCKET, []],
      unitId: [null, [Validators.required]],
      baudrate: [null, []],
      deviceName: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      deviceType: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      pollPeriod: [null, []],
      sendDataToThingsBoard: [false, []],
      byteOrder:[ModbusOrderType.BIG, []],
      security: [],
      identity: this.fb.group({
        vendorName: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
        productCode: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
        vendorUrl: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
        productName: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
        modelName: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
      }),
      values: [],
    });

    this.slaveConfigFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value: SlaveConfig) => {
      if (value.type === ModbusProtocolType.Serial) {
        value.port = value.serialPort;
        delete value.serialPort;
      }
      this.onChange(value);
      this.onTouched();
    });

    this.observeTypeChange();
    this.observeFormEnable();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: (value: SlaveConfig) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  validate(): ValidationErrors | null {
    return this.slaveConfigFormGroup.valid ? null : {
      serverConfigFormGroup: { valid: false }
    };
  }

  writeValue(slaveConfig: ModbusSlave): void {
    this.showSecurityControl.patchValue(!!slaveConfig.security);
    this.updateSlaveConfig(slaveConfig);
    this.updateFormEnableState(slaveConfig.sendDataToThingsBoard);
  }

  private observeTypeChange(): void {
    this.slaveConfigFormGroup.get('type').valueChanges.pipe(takeUntil(this.destroy$)).subscribe(type => {
      this.updateFormEnableState(this.slaveConfigFormGroup.get('sendDataToThingsBoard').value);
    });
  }

  private observeFormEnable(): void {
    this.slaveConfigFormGroup.get('sendDataToThingsBoard').valueChanges
      .pipe(startWith(this.slaveConfigFormGroup.get('sendDataToThingsBoard').value), takeUntil(this.destroy$))
      .subscribe(value => {
      this.updateFormEnableState(value);
    });
  }

  private updateFormEnableState(enabled: boolean): void {
    if (enabled) {
      this.slaveConfigFormGroup.enable({emitEvent: false});
      this.showSecurityControl.enable({emitEvent: false});
    } else {
      this.slaveConfigFormGroup.disable({emitEvent: false});
      this.showSecurityControl.disable({emitEvent: false});
      this.slaveConfigFormGroup.get('sendDataToThingsBoard').enable({emitEvent: false});
    }
    this.updateEnablingByProtocol(this.slaveConfigFormGroup.get('type').value);
  }

  private updateEnablingByProtocol(type: ModbusProtocolType): void {
    if (type === ModbusProtocolType.Serial) {
      if (this.slaveConfigFormGroup.get('sendDataToThingsBoard').value) {
        this.serialSpecificControlKeys.forEach(key => this.slaveConfigFormGroup.get(key)?.enable({emitEvent: false}));
      }
      this.tcpUdpSpecificControlKeys.forEach(key => this.slaveConfigFormGroup.get(key)?.disable({emitEvent: false}));
    } else {
      this.serialSpecificControlKeys.forEach(key => this.slaveConfigFormGroup.get(key)?.disable({emitEvent: false}));
      if (this.slaveConfigFormGroup.get('sendDataToThingsBoard').value) {
        this.tcpUdpSpecificControlKeys.forEach(key => this.slaveConfigFormGroup.get(key)?.enable({emitEvent: false}));
      }
    }
  };

  private updateSlaveConfig(slaveConfig: ModbusSlave): void {
    const {
      type,
      method,
      unitId,
      deviceName,
      deviceType,
      pollPeriod,
      sendDataToThingsBoard,
      byteOrder,
      security,
      identity,
      values,
      baudrate,
      host,
      port,
    } = slaveConfig;
    let slaveState: ModbusSlave = {
      host: host ?? '',
      type: type ?? ModbusProtocolType.TCP,
      method: method ?? ModbusMethodType.SOCKET,
      unitId: unitId ?? null,
      deviceName: deviceName ?? '',
      deviceType: deviceType ?? '',
      pollPeriod: pollPeriod ?? null,
      sendDataToThingsBoard: !!sendDataToThingsBoard,
      byteOrder: byteOrder ?? ModbusOrderType.BIG,
      security: security ?? {},
      identity: identity ?? {
        vendorName: '',
        productCode: '',
        vendorUrl: '',
        productName: '',
        modelName: '',
      },
      values: values ?? {} as ModbusRegisterValues,
      port: port ?? null,
    };
    if (slaveConfig.type === ModbusProtocolType.Serial) {
      slaveState = { ...slaveState, baudrate, serialPort: port, host: '', port: null } as ModbusSlave;
    } else {
      slaveState = { ...slaveState, serialPort: '', baudrate: null } as ModbusSlave;
    }
    this.slaveConfigFormGroup.setValue(slaveState, {emitEvent: false});
  }
}
