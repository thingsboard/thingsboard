// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
