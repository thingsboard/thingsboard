// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { FontSettings } from '@home/components/widget/lib/settings.models';
import { AnimationRule, AnimationTarget } from '@home/components/widget/lib/analogue-gauge.models';

export interface AnalogueCompassSettings {
  majorTicks: string[];
  minorTicks: number;
  showStrokeTicks: boolean;
  needleCircleSize: number;
  showBorder: boolean;
  borderOuterWidth: number;
  colorPlate: string;
  colorMajorTicks: string;
  colorMinorTicks: string;
  colorNeedle: string;
  colorNeedleCircle: string;
  colorBorder: string;
  majorTickFont: FontSettings;
  animation: boolean;
  animationDuration: number;
  animationRule: AnimationRule;
  animationTarget: AnimationTarget;
}
