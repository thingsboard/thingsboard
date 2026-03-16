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
import { WidgetButtonCustomStyles } from '@shared/components/button/widget-button.models';
import { BackgroundSettings, BackgroundType, cssUnit, Font } from '@shared/models/widget-settings.models';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { TbUnit } from '@shared/models/unit.models';

const defaultMainColor = '#305680';

export enum ValueStepperType {
  simplified = 'simplified',
  default = 'default',
  default_volume = 'default_volume'
}

export const valueStepperTypes = Object.keys(ValueStepperType) as ValueStepperType[];

export const valueStepperTypeTranslations = new Map<ValueStepperType, string>(
  [
    [ValueStepperType.simplified, 'widgets.value-stepper.simplified'],
    [ValueStepperType.default, 'widgets.value-stepper.filled'],
    [ValueStepperType.default_volume, 'widgets.value-stepper.volume']
  ]
);

export const valueStepperTypeImages = new Map<ValueStepperType, string>(
  [
    [ValueStepperType.simplified, 'assets/widget/value-stepper/simplified.svg'],
    [ValueStepperType.default, 'assets/widget/value-stepper/filled.svg'],
    [ValueStepperType.default_volume, 'assets/widget/value-stepper/volume.svg']
  ]
);

export interface ValueStepperWidgetSettings {
  initialState: GetValueSettings<number>;
  leftButtonClick: SetValueSettings;
  rightButtonClick: SetValueSettings;
  disabledState: GetValueSettings<boolean>;

  appearance: ValueStepperAppearance;
  buttonAppearance: {
    leftButton: ValueStepperButtonAppearance;
    rightButton: ValueStepperButtonAppearance;
  }

  background: BackgroundSettings;
  padding: string;
}

export interface ValueStepperAppearance {
  type: ValueStepperType;
  autoScale: boolean;
  minValueRange: number;
  maxValueRange: number;
  valueStep: number;
  showValueBox: boolean;
  valueUnits: TbUnit;
  valueDecimals: number;
  valueFont: Font;
  valueColor: string;
  valueBoxBackground: string;
  showBorder: boolean;
  borderWidth: number;
  borderColor: string;
}

export interface ValueStepperButtonAppearance {
  showButton: boolean;
  icon: string;
  iconSize: number;
  iconSizeUnit: cssUnit;
  mainColorOn: string;
  backgroundColorOn: string;
  mainColorOff: string;
  backgroundColorOff: string;
  mainColorDisabled: string;
  backgroundColorDisabled: string;
  customStyle: WidgetButtonCustomStyles;
}

export const valueStepperDefaultAppearance: ValueStepperAppearance = {
  type: ValueStepperType.simplified,
  autoScale: true,
  minValueRange: -100,
  maxValueRange: 100,
  valueStep: 0.5,
  showValueBox: true,
  valueUnits: '',
  valueDecimals: 1,
  valueFont: {
    family: 'Roboto',
    weight: '500',
    style: 'normal',
    size: 16,
    sizeUnit: 'px',
    lineHeight: '24px'
  },
  valueColor: '#000',
  valueBoxBackground: 'rgba(0, 0, 0, 0.12)',
  showBorder: true,
  borderWidth: 1,
  borderColor: defaultMainColor
}

export const valueStepperButtonDefaultAppearance: ValueStepperButtonAppearance = {
  showButton: true,
  icon: '',
  iconSize: 24,
  iconSizeUnit: 'px',

  mainColorOn: '#3F52DD',
  backgroundColorOn: '#FFFFFF',
  mainColorOff: '#A2A2A2',
  backgroundColorOff: '#FFFFFF',
  mainColorDisabled: 'rgba(0,0,0,0.12)',
  backgroundColorDisabled: '#FFFFFF',
  customStyle: {
    enabled: null,
    hovered: null,
    pressed: null,
    activated: null,
    disabled: null
  }
}

export const valueStepperDefaultSettings: ValueStepperWidgetSettings = {
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
  leftButtonClick: {
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
  rightButtonClick: {
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
  appearance: valueStepperDefaultAppearance,
  buttonAppearance: {
    leftButton: {...valueStepperButtonDefaultAppearance, icon: 'arrow_back_ios_new'},
    rightButton: {...valueStepperButtonDefaultAppearance, icon: 'arrow_forward_ios'}
  },
  background: {
    type: BackgroundType.color,
    color: '#fff',
    overlay: {
      enabled: false,
      color: 'rgba(255,255,255,0.72)',
      blur: 3
    }
  },
  padding: '12px'
};
