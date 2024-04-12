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

import { GaugeType } from '@home/components/widget/lib/canvas-digital-gauge';
import { AnimationRule } from '@home/components/widget/lib/analogue-gauge.models';
import { FontSettings } from '@home/components/widget/lib/settings.models';
import { ColorSettings } from '@shared/models/widget-settings.models';

export interface AttributeSourceProperty {
  valueSource: string;
  entityAlias?: string;
  attribute?: string;
  value?: number;
}

export interface FixedLevelColors {
  from?: AttributeSourceProperty | number;
  to?: AttributeSourceProperty | number;
  color: string;
}

export interface ColorLevelSetting {
  value: number;
  color: string;
}

export type colorLevel = Array<string | ColorLevelSetting>;

export type attributesGaugeType = 'levelColors' | 'ticks';

export enum DigitalGaugeType {
  arc = 'arc',
  donut = 'donut',
  horizontalBar = 'horizontalBar',
  verticalBar = 'verticalBar'
}

export const digitalGaugeLayouts = Object.keys(DigitalGaugeType) as DigitalGaugeType[];

export const digitalGaugeLayoutTranslations = new Map<DigitalGaugeType, string>(
  [
    [DigitalGaugeType.arc, 'widgets.gauge.gauge-type-arc'],
    [DigitalGaugeType.donut, 'widgets.gauge.gauge-type-donut'],
    [DigitalGaugeType.horizontalBar, 'widgets.gauge.gauge-type-horizontal-bar'],
    [DigitalGaugeType.verticalBar, 'widgets.gauge.gauge-type-vertical-bar']
  ]
);

export const digitalGaugeLayoutImages = new Map<DigitalGaugeType, string>(
  [
    [DigitalGaugeType.arc, 'assets/widget/simple-gauge/arc-layout.svg'],
    [DigitalGaugeType.donut, 'assets/widget/simple-gauge/donut-layout.svg'],
    [DigitalGaugeType.horizontalBar, 'assets/widget/simple-gauge/horizontal-bar-layout.svg'],
    [DigitalGaugeType.verticalBar, 'assets/widget/simple-gauge/vertical-bar-layout.svg']
  ]
);

export interface DigitalGaugeSettings {
  minValue?: number;
  maxValue?: number;
  gaugeType?: GaugeType;
  donutStartAngle?: number;
  neonGlowBrightness?: number;
  dashThickness?: number;
  roundedLineCap?: boolean;
  title?: string;
  showTitle?: boolean;
  unitTitle?: string;
  showUnitTitle?: boolean;
  showTimestamp?: boolean;
  timestampFormat?: string;
  showValue?: boolean;
  showMinMax?: boolean;
  gaugeWidthScale?: number;
  defaultColor?: string;
  gaugeColor?: string;

  barColor?: ColorSettings;

  useFixedLevelColor?: boolean;
  levelColors?: colorLevel;
  fixedLevelColors?: FixedLevelColors[];
  animation?: boolean;
  animationDuration?: number;
  animationRule?: AnimationRule;
  titleFont?: FontSettings;
  labelFont?: FontSettings;
  valueFont?: FontSettings;
  minMaxFont?: FontSettings;
  decimals?: number;
  units?: string;
  hideValue?: boolean;
  hideMinMax?: boolean;
  showTicks?: boolean;
  ticksValue?: AttributeSourceProperty[];
  ticks?: number[];
  colorTicks?: string;
  tickWidth?: number;
}

export const defaultDigitalSimpleGaugeOptions: DigitalGaugeSettings = {
  gaugeType: DigitalGaugeType.donut,
  timestampFormat: 'yyyy-MM-dd HH:mm:ss',
};
