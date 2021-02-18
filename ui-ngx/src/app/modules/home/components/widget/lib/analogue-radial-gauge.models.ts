///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { JsonSettingsSchema } from '@shared/models/widget.models';
import { AnalogueGaugeSettings, analogueGaugeSettingsSchema } from '@home/components/widget/lib/analogue-gauge.models';
import { deepClone } from '@core/utils';

export interface AnalogueRadialGaugeSettings extends AnalogueGaugeSettings {
  startAngle: number;
  ticksAngle: number;
  needleCircleSize: number;
}

export function getAnalogueRadialGaugeSettingsSchema(): JsonSettingsSchema {
  const analogueRadialGaugeSettingsSchema = deepClone(analogueGaugeSettingsSchema);
  analogueRadialGaugeSettingsSchema.schema.properties =
  {...analogueRadialGaugeSettingsSchema.schema.properties, ...{
         startAngle: {
          title: 'Start ticks angle',
          type: 'number',
          default: 45
        },
        ticksAngle: {
          title: 'Ticks angle',
          type: 'number',
          default: 270
        },
        needleCircleSize: {
          title: 'Needle circle size',
          type: 'number',
          default: 10
  }}};
  analogueRadialGaugeSettingsSchema.form.unshift(
    'startAngle',
    'ticksAngle',
    'needleCircleSize'
  );
  return analogueRadialGaugeSettingsSchema;
}
