// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { widgetType } from '@shared/models/widget.models';
import { AlarmSeverity } from '@shared/models/alarm.models';
import { TbFunction } from '@shared/models/js-function.models';

export enum GetValueAction {
  DO_NOTHING = 'DO_NOTHING',
  EXECUTE_RPC = 'EXECUTE_RPC',
  GET_ATTRIBUTE = 'GET_ATTRIBUTE',
  GET_TIME_SERIES = 'GET_TIME_SERIES',
  GET_ALARM_STATUS = 'GET_ALARM_STATUS',
  GET_DASHBOARD_STATE = 'GET_DASHBOARD_STATE',
  GET_DASHBOARD_STATE_OBJECT = 'GET_DASHBOARD_STATE_OBJECT',
}

export const getValueActions = Object.keys(GetValueAction) as GetValueAction[];

export const getValueActionsByWidgetType = (type: widgetType): GetValueAction[] => {
  if (type !== widgetType.rpc) {
    return getValueActions.filter(action => action !== GetValueAction.EXECUTE_RPC);
  } else {
    return getValueActions;
  }
};

export const getValueActionTranslations = new Map<GetValueAction, string>(
  [
    [GetValueAction.DO_NOTHING, 'widgets.value-action.do-nothing'],
    [GetValueAction.EXECUTE_RPC, 'widgets.value-action.execute-rpc'],
    [GetValueAction.GET_ATTRIBUTE, 'widgets.value-action.get-attribute'],
    [GetValueAction.GET_TIME_SERIES, 'widgets.value-action.get-time-series'],
    [GetValueAction.GET_ALARM_STATUS, 'widgets.value-action.get-alarm-status'],
    [GetValueAction.GET_DASHBOARD_STATE, 'widgets.value-action.get-dashboard-state'],
    [GetValueAction.GET_DASHBOARD_STATE_OBJECT, 'widgets.value-action.get-dashboard-state-object'],
  ]
);

export interface RpcSettings {
  method: string;
  requestTimeout: number;
  requestPersistent: boolean;
  persistentPollingInterval: number;
}

export interface AlarmStatusSettings {
  severityList: Array<AlarmSeverity>;
  typeList: Array<string>;
}

export interface TelemetryValueSettings {
  key: string;
}

export interface GetAttributeValueSettings extends TelemetryValueSettings {
  scope: AttributeScope | null;
}

export interface SetAttributeValueSettings extends TelemetryValueSettings {
  scope: AttributeScope.SERVER_SCOPE | AttributeScope.SHARED_SCOPE;
}

export enum DataToValueType {
  NONE = 'NONE',
  FUNCTION = 'FUNCTION'
}

export interface DataToValueSettings {
  type: DataToValueType;
  dataToValueFunction: TbFunction;
  compareToValue?: any;
}

export interface ValueActionSettings {
  actionLabel?: string;
}

export interface GetValueSettings<V> extends ValueActionSettings {
  action: GetValueAction;
  defaultValue: V;
  executeRpc?: RpcSettings;
  getAttribute: GetAttributeValueSettings;
  getTimeSeries: TelemetryValueSettings;
  getAlarmStatus: AlarmStatusSettings;
  dataToValue: DataToValueSettings;
}

export enum SetValueAction {
  EXECUTE_RPC = 'EXECUTE_RPC',
  SET_ATTRIBUTE = 'SET_ATTRIBUTE',
  ADD_TIME_SERIES = 'ADD_TIME_SERIES'
}

export const setValueActions = Object.keys(SetValueAction) as SetValueAction[];

export const setValueActionsByWidgetType = (type: widgetType): SetValueAction[] => {
  if (type !== widgetType.rpc) {
    return setValueActions.filter(action => action !== SetValueAction.EXECUTE_RPC);
  } else {
    return setValueActions;
  }
};

export const setValueActionTranslations = new Map<SetValueAction, string>(
  [
    [SetValueAction.EXECUTE_RPC, 'widgets.value-action.execute-rpc'],
    [SetValueAction.SET_ATTRIBUTE, 'widgets.value-action.set-attribute'],
    [SetValueAction.ADD_TIME_SERIES, 'widgets.value-action.add-time-series']
  ]
);

export enum ValueToDataType {
  VALUE = 'VALUE',
  CONSTANT = 'CONSTANT',
  FUNCTION = 'FUNCTION',
  NONE = 'NONE'
}

export interface ValueToDataSettings {
  type: ValueToDataType;
  constantValue: any;
  valueToDataFunction: TbFunction;
}

export interface SetValueSettings extends ValueActionSettings {
  action: SetValueAction;
  executeRpc: RpcSettings;
  setAttribute: SetAttributeValueSettings;
  putTimeSeries: TelemetryValueSettings;
  valueToData: ValueToDataSettings;
}
