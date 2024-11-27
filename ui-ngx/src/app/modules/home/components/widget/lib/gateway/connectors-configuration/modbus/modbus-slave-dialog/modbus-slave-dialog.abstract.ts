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

import { Directive, Inject, OnDestroy } from '@angular/core';
import {
  FormBuilder,
  FormControl,
  UntypedFormGroup,
  Validators,
} from '@angular/forms';
import {
  ModbusBaudrates,
  ModbusByteSizes,
  ModbusMethodLabelsMap,
  ModbusMethodType,
  ModbusOrderType,
  ModbusParity,
  ModbusParityLabelsMap,
  ModbusProtocolLabelsMap,
  ModbusProtocolType,
  ModbusSerialMethodType,
  ModbusSlaveInfo,
  noLeadTrailSpacesRegex,
  PortLimits,
  ReportStrategyDefaultValue,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { Subject } from 'rxjs';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { takeUntil } from 'rxjs/operators';
import { isEqual } from '@core/utils';
import { helpBaseUrl } from '@shared/models/constants';

@Directive()
export abstract class ModbusSlaveDialogAbstract<Component, Config> extends DialogComponent<Component, Config> implements OnDestroy {

  slaveConfigFormGroup: UntypedFormGroup;
  showSecurityControl: FormControl<boolean>;
  portLimits = PortLimits;

  readonly modbusProtocolTypes = Object.values(ModbusProtocolType);
  readonly modbusMethodTypes = Object.values(ModbusMethodType);
  readonly modbusSerialMethodTypes = Object.values(ModbusSerialMethodType);
  readonly modbusParities = Object.values(ModbusParity);
  readonly modbusByteSizes = ModbusByteSizes;
  readonly modbusBaudrates = ModbusBaudrates;
  readonly modbusOrderType = Object.values(ModbusOrderType);
  readonly ModbusProtocolType = ModbusProtocolType;
  readonly ModbusParityLabelsMap = ModbusParityLabelsMap;
  readonly ModbusProtocolLabelsMap = ModbusProtocolLabelsMap;
  readonly ModbusMethodLabelsMap = ModbusMethodLabelsMap;
  readonly ReportStrategyDefaultValue = ReportStrategyDefaultValue;
  readonly modbusHelpLink =
    helpBaseUrl + '/docs/iot-gateway/config/modbus/#section-master-description-and-configuration-parameters';

  private readonly serialSpecificControlKeys = ['serialPort', 'baudrate', 'stopbits', 'bytesize', 'parity', 'strict'];
  private readonly tcpUdpSpecificControlKeys = ['port', 'security', 'host'];

  private destroy$ = new Subject<void>();

  constructor(
    protected fb: FormBuilder,
    protected store: Store<AppState>,
    protected router: Router,
    @Inject(MAT_DIALOG_DATA) public data: ModbusSlaveInfo,
    public dialogRef: MatDialogRef<Component, Config>,
  ) {
    super(store, router, dialogRef);

    this.showSecurityControl = this.fb.control(false);
    this.initializeSlaveFormGroup();
    this.updateSlaveFormGroup();
    this.updateControlsEnabling(this.data.value.type);
    this.observeTypeChange();
    this.observeShowSecurity();
    this.showSecurityControl.patchValue(!!this.data.value.security && !isEqual(this.data.value.security, {}));
  }

  get protocolType(): ModbusProtocolType {
    return this.slaveConfigFormGroup.get('type').value;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    if (!this.slaveConfigFormGroup.valid) {
      return;
    }

    this.dialogRef.close(this.getSlaveResultData());
  }

  private initializeSlaveFormGroup(): void {
    this.slaveConfigFormGroup = this.fb.group({
      type: [ModbusProtocolType.TCP],
      host: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      port: [null, [Validators.required, Validators.min(PortLimits.MIN), Validators.max(PortLimits.MAX)]],
      serialPort: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      method: [ModbusMethodType.SOCKET, [Validators.required]],
      baudrate: [this.modbusBaudrates[0]],
      stopbits: [1],
      bytesize: [ModbusByteSizes[0]],
      parity: [ModbusParity.None],
      strict: [true],
      unitId: [null, [Validators.required]],
      deviceName: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      deviceType: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      timeout: [35],
      byteOrder: [ModbusOrderType.BIG],
      wordOrder: [ModbusOrderType.BIG],
      retries: [true],
      retryOnEmpty: [true],
      retryOnInvalid: [true],
      pollPeriod: [5000, [Validators.required]],
      connectAttemptTimeMs: [5000, [Validators.required]],
      connectAttemptCount: [5, [Validators.required]],
      waitAfterFailedAttemptsMs: [300000, [Validators.required]],
      values: [{}],
      security: [{}],
    });
    this.addFieldsToFormGroup();
  }

  private updateSlaveFormGroup(): void {
    this.slaveConfigFormGroup.patchValue({
      ...this.data.value,
      port: this.data.value.type === ModbusProtocolType.Serial ? null : this.data.value.port,
      serialPort: this.data.value.type === ModbusProtocolType.Serial ? this.data.value.port : '',
      values: {
        attributes: this.data.value.attributes ?? [],
        timeseries: this.data.value.timeseries ?? [],
        attributeUpdates: this.data.value.attributeUpdates ?? [],
        rpc: this.data.value.rpc ?? [],
      }
    });
  }

  private observeTypeChange(): void {
    this.slaveConfigFormGroup.get('type').valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(type => {
        this.updateControlsEnabling(type);
        this.updateMethodType(type);
      });
  }

  private updateMethodType(type: ModbusProtocolType): void {
    if (this.slaveConfigFormGroup.get('method').value !== ModbusMethodType.RTU) {
      this.slaveConfigFormGroup.get('method').patchValue(
        type === ModbusProtocolType.Serial
          ? ModbusSerialMethodType.ASCII
          : ModbusMethodType.SOCKET,
        {emitEvent: false}
      );
    }
  }

  private updateControlsEnabling(type: ModbusProtocolType): void {
    const [enableKeys, disableKeys] = type === ModbusProtocolType.Serial
      ? [this.serialSpecificControlKeys, this.tcpUdpSpecificControlKeys]
      : [this.tcpUdpSpecificControlKeys, this.serialSpecificControlKeys];

    enableKeys.forEach(key => this.slaveConfigFormGroup.get(key)?.enable({ emitEvent: false }));
    disableKeys.forEach(key => this.slaveConfigFormGroup.get(key)?.disable({ emitEvent: false }));

    this.updateSecurityEnabling(this.showSecurityControl.value);
  }

  private observeShowSecurity(): void {
    this.showSecurityControl.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(value => this.updateSecurityEnabling(value));
  }

  private updateSecurityEnabling(isEnabled: boolean): void {
    if (isEnabled && this.protocolType !== ModbusProtocolType.Serial) {
      this.slaveConfigFormGroup.get('security').enable({emitEvent: false});
    } else {
      this.slaveConfigFormGroup.get('security').disable({emitEvent: false});
    }
  }

  protected abstract addFieldsToFormGroup(): void;
  protected abstract getSlaveResultData(): Config;
}
