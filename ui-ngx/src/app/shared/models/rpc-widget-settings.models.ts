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

import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { BackgroundSettings } from '@shared/models/widget-settings.models';

export enum RpcInitialStateAction {
  DO_NOTHING = 'DO_NOTHING',
  EXECUTE_RPC = 'EXECUTE_RPC',
  GET_ATTRIBUTE = 'GET_ATTRIBUTE',
  GET_TIME_SERIES = 'GET_TIME_SERIES'
}

export const rpcInitialStateActions = Object.keys(RpcInitialStateAction) as RpcInitialStateAction[];

export const rpcInitialStateTranslations = new Map<RpcInitialStateAction, string>(
  [
    [RpcInitialStateAction.DO_NOTHING, 'widgets.rpc-state.do-nothing'],
    [RpcInitialStateAction.EXECUTE_RPC, 'widgets.rpc-state.execute-rpc'],
    [RpcInitialStateAction.GET_ATTRIBUTE, 'widgets.rpc-state.get-attribute'],
    [RpcInitialStateAction.GET_TIME_SERIES, 'widgets.rpc-state.get-time-series']
  ]
);

export interface RpcSettings {
  method: string;
  requestTimeout: number;
  requestPersistent: boolean;
  persistentPollingInterval: number;
}

export interface RpcTelemetrySettings {
  key: string;
}

export interface RpcGetAttributeSettings extends RpcTelemetrySettings {
  scope: AttributeScope | null;
}

export interface RpcSetAttributeSettings extends RpcTelemetrySettings {
  scope: AttributeScope.SERVER_SCOPE | AttributeScope.SHARED_SCOPE;
}

export enum RpcDataToStateType {
  NONE = 'NONE',
  FUNCTION = 'FUNCTION'
}

export interface RpcDataToStateSettings {
  type: RpcDataToStateType;
  dataToStateFunction: string;
  compareToValue?: any;
}

export interface RpcInitialStateSettings<V> {
  action: RpcInitialStateAction;
  defaultValue: V;
  executeRpc: RpcSettings;
  getAttribute: RpcGetAttributeSettings;
  getTimeSeries: RpcTelemetrySettings;
  dataToState: RpcDataToStateSettings;
}

export enum RpcUpdateStateAction {
  EXECUTE_RPC = 'EXECUTE_RPC',
  SET_ATTRIBUTE = 'SET_ATTRIBUTE',
  ADD_TIME_SERIES = 'ADD_TIME_SERIES'
}

export const rpcUpdateStateActions = Object.keys(RpcUpdateStateAction) as RpcUpdateStateAction[];

export const rpcUpdateStateTranslations = new Map<RpcUpdateStateAction, string>(
  [
    [RpcUpdateStateAction.EXECUTE_RPC, 'widgets.rpc-state.execute-rpc'],
    [RpcUpdateStateAction.SET_ATTRIBUTE, 'widgets.rpc-state.set-attribute'],
    [RpcUpdateStateAction.ADD_TIME_SERIES, 'widgets.rpc-state.add-time-series']
  ]
);

export enum RpcStateToParamsType {
  CONSTANT = 'CONSTANT',
  FUNCTION = 'FUNCTION',
  NONE = 'NONE'
}

export interface RpcStateToParamsSettings {
  type: RpcStateToParamsType;
  constantValue: any;
  stateToParamsFunction: string;
}

export interface RpcUpdateStateSettings {
  action: RpcUpdateStateAction;
  executeRpc: RpcSettings;
  setAttribute: RpcSetAttributeSettings;
  putTimeSeries: RpcTelemetrySettings;
  stateToParams: RpcStateToParamsSettings;
}

export interface RpcStateBehaviourSettings<V> {
  initialState: RpcInitialStateSettings<V>;
  updateStateByValue: (value: V) => RpcUpdateStateSettings;
}

export interface RpcStateWidgetSettings<V> {
  initialState: RpcInitialStateSettings<V>;
  background: BackgroundSettings;
}
