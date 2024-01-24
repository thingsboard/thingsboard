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

import { BackgroundType, cssUnit, Font } from '@shared/models/widget-settings.models';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import {
  RpcDataToStateType,
  RpcInitialStateAction,
  RpcStateToParamsType,
  RpcStateWidgetSettings,
  RpcUpdateStateAction,
  RpcUpdateStateSettings
} from '@shared/models/rpc-widget-settings.models';

export enum SingleSwitchLayout {
  right = 'right',
  left = 'left',
  centered = 'centered'
}

export const singleSwitchLayouts = Object.keys(SingleSwitchLayout) as SingleSwitchLayout[];

export const singleSwitchLayoutTranslations = new Map<SingleSwitchLayout, string>(
  [
    [SingleSwitchLayout.right, 'widgets.single-switch.layout-right'],
    [SingleSwitchLayout.left, 'widgets.single-switch.layout-left'],
    [SingleSwitchLayout.centered, 'widgets.single-switch.layout-centered']
  ]
);

export const singleSwitchLayoutImages = new Map<SingleSwitchLayout, string>(
  [
    [SingleSwitchLayout.right, 'assets/widget/single-switch/right-layout.svg'],
    [SingleSwitchLayout.left, 'assets/widget/single-switch/left-layout.svg'],
    [SingleSwitchLayout.centered, 'assets/widget/single-switch/centered-layout.svg']
  ]
);

export interface SingleSwitchWidgetSettings extends RpcStateWidgetSettings<boolean> {
  onUpdateState: RpcUpdateStateSettings;
  offUpdateState: RpcUpdateStateSettings;
  layout: SingleSwitchLayout;
  autoScale: boolean;
  showLabel: boolean;
  label: string;
  labelFont: Font;
  labelColor: string;
  showIcon: boolean;
  icon: string;
  iconSize: number;
  iconSizeUnit: cssUnit;
  iconColor: string;
  switchColorOn: string;
  switchColorOff: string;
  switchColorDisabled: string;
  tumblerColorOn: string;
  tumblerColorOff: string;
  tumblerColorDisabled: string;
  showOnLabel: boolean;
  onLabel: string;
  onLabelFont: Font;
  onLabelColor: string;
  showOffLabel: boolean;
  offLabel: string;
  offLabelFont: Font;
  offLabelColor: string;
}

export const singleSwitchDefaultSettings: SingleSwitchWidgetSettings = {
  initialState: {
    action: RpcInitialStateAction.EXECUTE_RPC,
    defaultValue: false,
    executeRpc: {
      method: 'getState',
      requestTimeout: 5000,
      requestPersistent: false,
      persistentPollingInterval: 1000
    },
    getAttribute: {
      key: 'state',
      scope: null
    },
    getTimeSeries: {
      key: 'state'
    },
    dataToState: {
      type: RpcDataToStateType.NONE,
      compareToValue: true,
      dataToStateFunction: '/* Should return boolean value */\nreturn data;'
    }
  },
  onUpdateState: {
    action: RpcUpdateStateAction.EXECUTE_RPC,
    executeRpc: {
      method: 'setState',
      requestTimeout: 5000,
      requestPersistent: false,
      persistentPollingInterval: 1000
    },
    setAttribute: {
      key: 'state',
      scope: AttributeScope.SHARED_SCOPE
    },
    putTimeSeries: {
      key: 'state'
    },
    stateToParams: {
      type: RpcStateToParamsType.CONSTANT,
      constantValue: true,
      stateToParamsFunction: '/* Convert input boolean value to RPC parameters or attribute/time-series value */\nreturn value;'
    }
  },
  offUpdateState: {
    action: RpcUpdateStateAction.EXECUTE_RPC,
    executeRpc: {
      method: 'setState',
      requestTimeout: 5000,
      requestPersistent: false,
      persistentPollingInterval: 1000
    },
    setAttribute: {
      key: 'state',
      scope: AttributeScope.SHARED_SCOPE
    },
    putTimeSeries: {
      key: 'state'
    },
    stateToParams: {
      type: RpcStateToParamsType.CONSTANT,
      constantValue: false,
      stateToParamsFunction: '/* Convert input boolean value to RPC parameters or attribute/time-series value */ \n return value;'
    }
  },
  layout: SingleSwitchLayout.right,
  autoScale: true,
  showLabel: true,
  label: 'Switch',
  labelFont: {
    family: 'Roboto',
    size: 16,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '24px'
  },
  labelColor: 'rgba(0, 0, 0, 0.76)',
  showIcon: false,
  icon: 'mdi:lightbulb-outline',
  iconSize: 24,
  iconSizeUnit: 'px',
  iconColor: 'rgba(0, 0, 0, 0.76)',
  switchColorOn: '#5469FF',
  switchColorOff: 'rgba(84, 105, 255, 0.30)',
  switchColorDisabled: '#D5D7E5',
  tumblerColorOn: '#fff',
  tumblerColorOff: '#fff',
  tumblerColorDisabled: '#fff',
  showOnLabel: false,
  onLabel: 'On',
  onLabelFont: {
    family: 'Roboto',
    size: 16,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '24px'
  },
  onLabelColor: 'rgba(0, 0, 0, 0.38)',
  showOffLabel: false,
  offLabel: 'Off',
  offLabelFont: {
    family: 'Roboto',
    size: 16,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '24px'
  },
  offLabelColor: 'rgba(0, 0, 0, 0.38)',
  background: {
    type: BackgroundType.color,
    color: '#fff',
    overlay: {
      enabled: false,
      color: 'rgba(255,255,255,0.72)',
      blur: 3
    }
  }
};
