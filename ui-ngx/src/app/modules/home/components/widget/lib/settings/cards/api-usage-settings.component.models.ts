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

import { IAliasController } from '@core/api/widget-api.models';
import { WidgetConfigCallbacks } from '@home/components/widget/config/widget-config.component.models';
import { DataKey, Widget, widgetType } from '@shared/models/widget.models';
import { Observable } from 'rxjs';
import { BackgroundSettings, BackgroundType } from '@shared/models/widget-settings.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { materialColors } from '@shared/models/material.models';

export interface ApiUsageSettingsContext {
  aliasController: IAliasController;
  callbacks: WidgetConfigCallbacks;
  widget: Widget;
  editKey: (key: DataKey, entityAliasId: string, WidgetType?: widgetType) => Observable<DataKey>;
  generateDataKey: (key: DataKey) => DataKey;
}


export interface ApiUsageWidgetSettings {
  dsEntityAliasId: string;
  apiUsageDataKeys: ApiUsageDataKeysSettings[];
  targetDashboardState: string;
  background: BackgroundSettings;
  padding: string;
}

export interface ApiUsageDataKeysSettings {
  label: string;
  state: string;
  status: DataKey;
  maxLimit: DataKey;
  current: DataKey;
}

const generateDataKey = (label: string, status: string, maxLimit: string, current: string) => {
  return {
    label,
    state: '',
    status: {
      name: status,
      label: status,
      type: DataKeyType.timeseries,
      funcBody: undefined,
      settings: {},
      color: materialColors[0].value
    },
    maxLimit: {
      name: maxLimit,
      label: maxLimit,
      type: DataKeyType.timeseries,
      funcBody: undefined,
      settings: {},
      color: materialColors[0].value
    },
    current: {
      name: current,
      label: current,
      type: DataKeyType.timeseries,
      funcBody: undefined,
      settings: {},
      color: materialColors[0].value
    }
  }
}

export const apiUsageDefaultSettings: ApiUsageWidgetSettings = {
  dsEntityAliasId: '',
  apiUsageDataKeys: [
    generateDataKey('{i18n:api-usage.transport-messages}', 'transportApiState', 'transportMsgLimit', 'transportMsgCount'),
    generateDataKey('{i18n:api-usage.transport-data-points}', 'transportApiState', 'transportDataPointsLimit', 'transportDataPointsCount'),
    generateDataKey('{i18n:api-usage.rule-engine-executions}', 'ruleEngineApiState', 'ruleEngineExecutionLimit', 'ruleEngineExecutionCount'),
    generateDataKey('{i18n:api-usage.javascript-function-executions}', 'jsExecutionApiState', 'jsExecutionLimit', 'jsExecutionCount'),
    generateDataKey('{i18n:api-usage.tbel-function-executions}', 'tbelExecutionApiState', 'tbelExecutionLimit', 'tbelExecutionCount'),
    generateDataKey('{i18n:api-usage.data-points-storage-days}', 'dbApiState', 'storageDataPointsLimit', 'storageDataPointsCount'),
    generateDataKey('{i18n:api-usage.alarms-created}', 'alarmApiState', 'createdAlarmsLimit', 'createdAlarmsCount'),
    generateDataKey('{i18n:api-usage.emails}', 'emailApiState', 'emailLimit', 'emailCount'),
    generateDataKey('{i18n:api-usage.sms}', 'smsApiState', 'smsLimit', 'smsCount'),
  ],
  targetDashboardState: 'default',
  background: {
    type: BackgroundType.color,
    color: '#fff',
    overlay: {
      enabled: false,
      color: 'rgba(255,255,255,0.72)',
      blur: 3
    }
  },
  padding: '0'
};

export const getUniqueDataKeys = (data: ApiUsageDataKeysSettings[]): DataKey[] => {
  const seenNames = new Set<string>();
  return data
    .flatMap(item => [item.status, item.maxLimit, item.current])
    .filter(key => {
      if (seenNames.has(key.name)) {
        return false;
      }
      seenNames.add(key.name);
      return true;
    });
};
