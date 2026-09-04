// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { WidgetButtonAppearance, widgetButtonDefaultAppearance } from '@shared/components/button/widget-button.models';
import {
  DataToValueType,
  GetValueAction,
  GetValueSettings, SetValueAction,
  SetValueSettings, ValueToDataType
} from '@shared/models/action-widget-settings.models';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';

export interface CommandButtonWidgetSettings {
  appearance: WidgetButtonAppearance;
  onClickState: SetValueSettings;
  disabledState: GetValueSettings<boolean>;
}

export const commandButtonDefaultSettings: CommandButtonWidgetSettings = {
  appearance: {...widgetButtonDefaultAppearance, label: 'Send', icon: 'arrow_outward'},
  onClickState: {
    action: SetValueAction.EXECUTE_RPC,
    executeRpc: {
      method: 'setState',
      requestTimeout: 5000,
      requestPersistent: false,
      persistentPollingInterval: 1000
    },
    setAttribute: {
      key: 'state',
      scope: AttributeScope.SERVER_SCOPE
    },
    putTimeSeries: {
      key: 'state'
    },
    valueToData: {
      type: ValueToDataType.NONE,
      constantValue: true,
      valueToDataFunction: '/* Return RPC parameters or attribute/time-series value */\nreturn true;'
    }
  },
  disabledState: {
    action: GetValueAction.DO_NOTHING,
    defaultValue: false,
    getAttribute: {
      key: 'state',
      scope: null
    },
    getTimeSeries: {
      key: 'state'
    },
    getAlarmStatus: {
      severityList: null,
      typeList: null
    },
    dataToValue: {
      type: DataToValueType.NONE,
      compareToValue: true,
      dataToValueFunction: '/* Should return boolean value */\nreturn data;'
    }
  }
};
