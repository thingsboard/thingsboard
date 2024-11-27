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

import { Directive } from '@angular/core';
import { FormControl, FormGroup, ValidationErrors } from '@angular/forms';
import { takeUntil } from 'rxjs/operators';
import { isEqual } from '@core/utils';
import { GatewayConnectorBasicConfigDirective } from '@home/components/widget/lib/gateway/abstract/gateway-connector-basic-config.abstract';
import {
  ModbusBasicConfig,
  ModbusBasicConfig_v3_5_2,
} from '@home/components/widget/lib/gateway/gateway-widget.models';

@Directive()
export abstract class ModbusBasicConfigDirective<InputBasicConfig, OutputBasicConfig>
  extends GatewayConnectorBasicConfigDirective<InputBasicConfig, OutputBasicConfig> {

  enableSlaveControl: FormControl<boolean> = new FormControl(false);

  constructor() {
    super();

    this.enableSlaveControl.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(enable => {
        this.updateSlaveEnabling(enable);
        this.basicFormGroup.get('slave').updateValueAndValidity({ emitEvent: !!this.onChange });
      });
  }

  override writeValue(basicConfig: OutputBasicConfig & ModbusBasicConfig): void {
    super.writeValue(basicConfig);
    this.onEnableSlaveControl(basicConfig);
  }

  override validate(): ValidationErrors | null {
    const { master, slave } = this.basicFormGroup.value;
    const isEmpty = !master?.slaves?.length && (isEqual(slave, {}) || !slave);
    if (!this.basicFormGroup.valid || isEmpty) {
      return { basicFormGroup: { valid: false } };
    }
    return null;
  }

  protected override initBasicFormGroup(): FormGroup {
    return this.fb.group({
      master: [],
      slave: [],
    });
  }

  private updateSlaveEnabling(isEnabled: boolean): void {
    if (isEnabled) {
      this.basicFormGroup.get('slave').enable({ emitEvent: false });
    } else {
      this.basicFormGroup.get('slave').disable({ emitEvent: false });
    }
  }

  private onEnableSlaveControl(basicConfig: ModbusBasicConfig): void {
    this.enableSlaveControl.setValue(!!basicConfig.slave && !isEqual(basicConfig.slave, {}));
  }
}
