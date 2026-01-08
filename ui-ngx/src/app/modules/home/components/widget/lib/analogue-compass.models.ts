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
