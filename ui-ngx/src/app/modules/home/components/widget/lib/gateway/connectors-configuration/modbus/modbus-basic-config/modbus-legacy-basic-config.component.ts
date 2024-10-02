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

import { ChangeDetectionStrategy, Component, forwardRef } from '@angular/core';
import { NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import {
  LegacySlaveConfig,
  ModbusBasicConfig,
  ModbusLegacyBasicConfig,
  ModbusLegacySlave,
  ModbusMasterConfig,
  ModbusSlave
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { ModbusSlaveConfigComponent } from '../modbus-slave-config/modbus-slave-config.component';
import { ModbusMasterTableComponent } from '../modbus-master-table/modbus-master-table.component';
import { EllipsisChipListDirective } from '@shared/directives/ellipsis-chip-list.directive';
import { ModbusVersionMappingUtil } from '@home/components/widget/lib/gateway/utils/modbus-version-mapping.util';
import {
  ModbusBasicConfigDirective
} from '@home/components/widget/lib/gateway/connectors-configuration/modbus/modbus-basic-config/modbus-basic-config.abstract';

@Component({
  selector: 'tb-modbus-legacy-basic-config',
  templateUrl: './modbus-basic-config.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ModbusLegacyBasicConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ModbusLegacyBasicConfigComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    ModbusSlaveConfigComponent,
    ModbusMasterTableComponent,
    EllipsisChipListDirective,
  ],
  styleUrls: ['./modbus-basic-config.component.scss'],
})
export class ModbusLegacyBasicConfigComponent extends ModbusBasicConfigDirective<ModbusBasicConfig, ModbusLegacyBasicConfig> {

  isLegacy = true;

  protected override mapConfigToFormValue(config: ModbusLegacyBasicConfig): ModbusBasicConfig {
    return {
      master: config.master?.slaves ? config.master : { slaves: [] } as ModbusMasterConfig<LegacySlaveConfig>,
      slave: config.slave ? ModbusVersionMappingUtil.mapSlaveToUpgradedVersion(config.slave as ModbusLegacySlave) : {},
    } as ModbusBasicConfig;
  }

  protected override getMappedValue(value: ModbusBasicConfig): ModbusLegacyBasicConfig {
    return {
      master: value.master as ModbusMasterConfig<LegacySlaveConfig>,
      slave: value.slave ? ModbusVersionMappingUtil.mapSlaveToDowngradedVersion(value.slave as ModbusSlave) : {} as ModbusLegacySlave,
    };
  }
}
