// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import {
  WidgetButtonAppearance,
  widgetButtonDefaultAppearance
} from '@shared/components/button/widget-button.models';
import { DataToValueType, GetValueAction, GetValueSettings } from '@shared/models/action-widget-settings.models';

export interface ActionButtonWidgetSettings {
  appearance: WidgetButtonAppearance;
  activatedState: GetValueSettings<boolean>;
  disabledState: GetValueSettings<boolean>;
}

export const actionButtonDefaultSettings: ActionButtonWidgetSettings = {
  appearance: widgetButtonDefaultAppearance,
  activatedState: {
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
