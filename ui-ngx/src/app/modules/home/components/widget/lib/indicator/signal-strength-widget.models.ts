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
  BackgroundSettings,
  BackgroundType,
  ColorSettings,
  ColorType,
  DateFormatSettings,
  defaultColorFunction,
  Font,
  lastUpdateAgoDateFormat
} from '@shared/models/widget-settings.models';

export enum SignalStrengthLayout {
  wifi = 'wifi',
  cellular_bar = 'cellular_bar'
}

export const signalStrengthLayouts = Object.keys(SignalStrengthLayout) as SignalStrengthLayout[];

export const signalStrengthLayoutTranslations = new Map<SignalStrengthLayout, string>(
  [
    [SignalStrengthLayout.wifi, 'widgets.signal-strength.layout-wifi'],
    [SignalStrengthLayout.cellular_bar, 'widgets.signal-strength.layout-cellular-bar']
  ]
);

export const signalStrengthLayoutImages = new Map<SignalStrengthLayout, string>(
  [
    [SignalStrengthLayout.wifi, 'assets/widget/signal-strength/wifi-layout.svg'],
    [SignalStrengthLayout.cellular_bar, 'assets/widget/signal-strength/cellular-bar-layout.svg']
  ]
);

export interface SignalStrengthWidgetSettings {
  layout: SignalStrengthLayout;
  showDate: boolean;
  dateFormat: DateFormatSettings;
  dateFont: Font;
  dateColor: string;
  activeBarsColor: ColorSettings;
  noSignalRssiValue: number;
  inactiveBarsColor: string;
  showTooltip: boolean;
  showTooltipValue: boolean;
  tooltipValueFont: Font;
  tooltipValueColor: string;
  showTooltipDate: boolean;
  tooltipDateFormat: DateFormatSettings;
  tooltipDateFont: Font;
  tooltipDateColor: string;
  tooltipBackgroundColor: string;
  tooltipBackgroundBlur: number;
  background: BackgroundSettings;
  padding: string;
}

export const signalStrengthDefaultSettings: SignalStrengthWidgetSettings = {
  layout: SignalStrengthLayout.wifi,
  showDate: false,
  dateFormat: lastUpdateAgoDateFormat(),
  dateFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '16px'
  },
  dateColor: 'rgba(0, 0, 0, 0.38)',
  activeBarsColor: {
    color: 'rgba(92, 223, 144, 1)',
    type: ColorType.range,
    rangeList: {
      range: [
        {to: -85, color: 'rgba(227, 71, 71, 1)'},
        {from: -85, to: -70, color: 'rgba(255, 122, 0, 1)'},
        {from: -70, to: -55, color: 'rgba(246, 206, 67, 1)'},
        {from: -55, color: 'rgba(92, 223, 144, 1)'}
      ]
    },
    colorFunction: defaultColorFunction
  },
  noSignalRssiValue: -100,
  inactiveBarsColor: 'rgba(224, 224, 224, 1)',
  showTooltip: true,
  showTooltipValue: true,
  tooltipValueFont: {
    family: 'Roboto',
    size: 13,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '16px'
  },
  tooltipValueColor: 'rgba(0,0,0,0.76)',
  showTooltipDate: true,
  tooltipDateFormat: lastUpdateAgoDateFormat(),
  tooltipDateFont: {
    family: 'Roboto',
    size: 13,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '16px'
  },
  tooltipDateColor: 'rgba(0,0,0,0.76)',
  tooltipBackgroundColor: 'rgba(255,255,255,0.72)',
  tooltipBackgroundBlur: 3,
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

export const signalBarActive = (rssi: number, index: number, minSignal: number): boolean => {
    switch (index) {
      case 0:
        return rssi > minSignal;
      case 1:
        return rssi >= -85;
      case 2:
        return rssi >= -70;
      case 3:
        return rssi >= -55;
    }
};
