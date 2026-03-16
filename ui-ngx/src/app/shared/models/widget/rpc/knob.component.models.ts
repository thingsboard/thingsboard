///
/// Copyright © 2016-2026 The Thingsboard Authors
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
  DataToValueType,
  GetValueAction,
  GetValueSettings,
  SetValueAction,
  SetValueSettings,
  ValueToDataType
} from '@shared/models/action-widget-settings.models';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { isDefinedAndNotNull } from '@core/utils';

export interface KnobSettings {
  initialState: GetValueSettings<number>;
  valueChange: SetValueSettings;
  minValue: number;
  maxValue: number;
  initialValue: number;
  title: string;
  getValueMethod?: string; //deprecated
  setValueMethod?: string; //deprecated
  requestTimeout?: number; //deprecated
  requestPersistent?: boolean; //deprecated
  persistentPollingInterval?: number; //deprecated
}

export const knobWidgetDefaultSettings: KnobSettings = {
  initialState: {
    action: GetValueAction.EXECUTE_RPC,
    defaultValue: 50,
    executeRpc: {
      method: 'getValue',
      requestTimeout: 5000,
      requestPersistent: false,
      persistentPollingInterval: 5000
    },
    getAttribute: {
      key: 'value',
      scope: null
    },
    getTimeSeries: {
      key: 'value'
    },
    getAlarmStatus: {
      severityList: null,
      typeList: null
    },
    dataToValue: {
      type: DataToValueType.NONE,
      compareToValue: true,
      dataToValueFunction: '/* Should return double value */\nreturn data;'
    }
  },
  valueChange: {
    action: SetValueAction.EXECUTE_RPC,
    executeRpc: {
      method: 'setValue',
      requestTimeout: 5000,
      requestPersistent: false,
      persistentPollingInterval: 5000
    },
    setAttribute: {
      key: 'value',
      scope: AttributeScope.SERVER_SCOPE
    },
    putTimeSeries: {
      key: 'value'
    },
    valueToData: {
      type: ValueToDataType.VALUE,
      constantValue: 0,
      valueToDataFunction: '/* Convert input double value to RPC parameters or attribute/time-series value */\nreturn value;'
    }
  },
  title: '',
  minValue: 0,
  maxValue: 100,
  initialValue: 50
}

export const prepareKnobSettings = (settings: KnobSettings): KnobSettings => {
  if (isDefinedAndNotNull(settings.getValueMethod)) {
    settings.initialState.executeRpc.method = settings.getValueMethod;
    delete settings.getValueMethod;
  }

  if (isDefinedAndNotNull(settings.setValueMethod)) {
    settings.valueChange.executeRpc.method = settings.setValueMethod;
    delete settings.setValueMethod;
  }

  if (isDefinedAndNotNull(settings.requestPersistent)) {
    settings.initialState.executeRpc.requestPersistent = settings.requestPersistent;
    settings.valueChange.executeRpc.requestPersistent = settings.requestPersistent;
    delete settings.requestPersistent;
  }

  if (isDefinedAndNotNull(settings.persistentPollingInterval)) {
    settings.initialState.executeRpc.persistentPollingInterval = settings.persistentPollingInterval;
    settings.valueChange.executeRpc.persistentPollingInterval = settings.persistentPollingInterval;
    delete settings.persistentPollingInterval;
  }

  if (isDefinedAndNotNull(settings.requestTimeout)) {
    settings.initialState.executeRpc.requestTimeout = settings.requestTimeout;
    settings.valueChange.executeRpc.requestTimeout = settings.requestTimeout;
    delete settings.requestTimeout;
  }
  return settings;
}
