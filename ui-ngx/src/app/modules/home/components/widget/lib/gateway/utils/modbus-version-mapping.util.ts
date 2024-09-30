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

import {
  LegacySlaveConfig,
  ModbusDataType,
  ModbusLegacyRegisterValues,
  ModbusLegacySlave,
  ModbusMasterConfig,
  ModbusRegisterValues,
  ModbusSlave,
  ModbusValue,
  ModbusValues,
  ReportStrategyType,
  SlaveConfig
} from '@home/components/widget/lib/gateway/gateway-widget.models';

export class ModbusVersionMappingUtil {

  static mapMasterToUpgradedVersion(master: ModbusMasterConfig<LegacySlaveConfig>): ModbusMasterConfig {
    return {
      slaves: master.slaves.map((slave: LegacySlaveConfig) => {
        const { sendDataOnlyOnChange, ...restSlave } = slave;
        return {
          ...restSlave,
          deviceType: slave.deviceType ?? 'default',
          reportStrategy: sendDataOnlyOnChange
            ? { type: ReportStrategyType.OnChange }
            : { type: ReportStrategyType.OnReportPeriod, reportPeriod: slave.pollPeriod }
        };
      })
    };
  }

  static mapMasterToDowngradedVersion(master: ModbusMasterConfig): ModbusMasterConfig<LegacySlaveConfig> {
    return {
      slaves: master.slaves.map((slave: SlaveConfig) => {
        const { reportStrategy, ...restSlave } = slave;
        return {
          ...restSlave,
          sendDataOnlyOnChange: reportStrategy?.type !== ReportStrategyType.OnReportPeriod
        };
      })
    };
  }

  static mapSlaveToDowngradedVersion(slave: ModbusSlave): ModbusLegacySlave {
    if (!slave?.values) {
      return slave as Omit<ModbusLegacySlave, 'values'>;
    }
    const values = Object.keys(slave.values).reduce((acc, valueKey) => {
      acc = {
        ...acc,
        [valueKey]: [
          slave.values[valueKey]
        ]
      };
      return acc;
    }, {} as ModbusLegacyRegisterValues);
    return {
      ...slave,
      values
    };
  }

  static mapSlaveToUpgradedVersion(slave: ModbusLegacySlave): ModbusSlave {
    if (!slave?.values) {
      return slave as Omit<ModbusSlave, 'values'>;
    }
    const values = Object.keys(slave.values).reduce((acc, valueKey) => {
      acc = {
        ...acc,
        [valueKey]: this.mapValuesToUpgradedVersion(slave.values[valueKey][0])
      };
      return acc;
    }, {} as ModbusRegisterValues);
    return {
      ...slave,
      values
    };
  }

  private static mapValuesToUpgradedVersion(registerValues: ModbusValues): ModbusValues {
    return Object.keys(registerValues).reduce((acc, valueKey) => {
      acc = {
       ...acc,
        [valueKey]: registerValues[valueKey].map((value: ModbusValue) =>
          ({ ...value, type: (value.type as string) === 'int' ? ModbusDataType.INT16 : value.type }))
      };
      return acc;
    }, {} as ModbusValues);
  }
}
