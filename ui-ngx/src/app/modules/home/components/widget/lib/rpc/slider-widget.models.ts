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
import { BackgroundSettings, BackgroundType, cssUnit, Font } from '@shared/models/widget-settings.models';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { TbUnit } from '@shared/models/unit.models';

export enum SliderLayout {
  default = 'default',
  extended = 'extended',
  simplified = 'simplified'
}

export const sliderLayouts = Object.keys(SliderLayout) as SliderLayout[];

export const sliderLayoutTranslations = new Map<SliderLayout, string>(
  [
    [SliderLayout.default, 'widgets.slider.layout-default'],
    [SliderLayout.extended, 'widgets.slider.layout-extended'],
    [SliderLayout.simplified, 'widgets.slider.layout-simplified']
  ]
);

export const sliderLayoutImages = new Map<SliderLayout, string>(
  [
    [SliderLayout.default, 'assets/widget/slider/default-layout.svg'],
    [SliderLayout.extended, 'assets/widget/slider/extended-layout.svg'],
    [SliderLayout.simplified, 'assets/widget/slider/simplified-layout.svg']
  ]
);

export interface SliderWidgetSettings {
  initialState: GetValueSettings<number>;
  disabledState: GetValueSettings<boolean>;
  valueChange: SetValueSettings;
  layout: SliderLayout;
  autoScale: boolean;
  showValue: boolean;
  valueUnits: TbUnit;
  valueDecimals: number;
  valueFont: Font;
  valueColor: string;
  showTicks: boolean;
  tickMin: number;
  tickMax: number;
  ticksFont: Font;
  ticksColor: string;
  showTickMarks: boolean;
  tickMarksCount: number;
  tickMarksColor: string;
  mainColor: string;
  backgroundColor: string;
  mainColorDisabled: string;
  backgroundColorDisabled: string;
  leftIcon: string;
  leftIconSize: number;
  leftIconSizeUnit: cssUnit;
  leftIconColor: string;
  rightIcon: string;
  rightIconSize: number;
  rightIconSizeUnit: cssUnit;
  rightIconColor: string;
  background: BackgroundSettings;
  padding: string;
}

export const sliderWidgetDefaultSettings: SliderWidgetSettings = {
  initialState: {
    action: GetValueAction.EXECUTE_RPC,
    defaultValue: 0,
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
    getAlarmStatus: {
      severityList: null,
      typeList: null
    },
    dataToValue: {
      type: DataToValueType.NONE,
      compareToValue: true,
      dataToValueFunction: '/* Should return integer value */\nreturn data;'
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
  },
  valueChange: {
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
      type: ValueToDataType.VALUE,
      constantValue: 0,
      valueToDataFunction: '/* Convert input integer value to RPC parameters or attribute/time-series value */\nreturn value;'
    }
  },
  layout: SliderLayout.default,
  autoScale: true,
  showValue: true,
  valueUnits: '%',
  valueDecimals: 0,
  valueFont: {
    family: 'Roboto',
    size: 36,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '36px'
  },
  valueColor: 'rgba(0, 0, 0, 0.87)',
  showTicks: true,
  tickMin: 0,
  tickMax: 100,
  ticksFont: {
    family: 'Roboto',
    size: 11,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '16px'
  },
  ticksColor: 'rgba(0,0,0,0.54)',
  showTickMarks: true,
  tickMarksCount: 11,
  tickMarksColor: '#5469FF',
  mainColor: '#5469FF',
  backgroundColor: '#CCD2FF',
  mainColorDisabled: '#9BA2B0',
  backgroundColorDisabled: '#D5D7E5',
  leftIcon: 'lightbulb',
  leftIconSize: 24,
  leftIconSizeUnit: 'px',
  leftIconColor: '#5469FF',
  rightIcon: 'mdi:lightbulb-on',
  rightIconSize: 24,
  rightIconSizeUnit: 'px',
  rightIconColor: '#5469FF',
  background: {
    type: BackgroundType.color,
    color: '#fff',
    overlay: {
      enabled: false,
      color: 'rgba(255,255,255,0.72)',
      blur: 3
    }
  },
  padding: '24px'
};
