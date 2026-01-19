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

import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type TemperatureMetricUnits = '°C' | 'K';
export type TemperatureImperialUnits = '°F' | '°R';

export type TemperatureUnits =
  | TemperatureMetricUnits
  | TemperatureImperialUnits;

const METRIC: TbMeasureUnits<TemperatureMetricUnits> = {
  transform: (C) => C / (5 / 9) + 32,
  units: {
    '°C': {
      name: 'unit.celsius',
      tags: ['heat', 'cold', 'warmth', 'degrees', 'shipment condition'],
      to_anchor: 1,
    },
    K: {
      name: 'unit.kelvin',
      tags: ['heat', 'cold', 'warmth', 'degrees', 'color quality', 'white balance', 'color temperature'],
      to_anchor: 1,
      anchor_shift: 273.15,
    },
  }
};

const IMPERIAL: TbMeasureUnits<TemperatureImperialUnits> = {
  transform: (F) => (F - 32) * (5 / 9),
  units: {
    '°F': {
      name: 'unit.fahrenheit',
      tags: ['heat', 'cold', 'warmth', 'degrees'],
      to_anchor: 1,
    },
    '°R': {
      name: 'unit.rankine',
      tags: ['heat', 'cold', 'warmth'],
      to_anchor: 1,
      anchor_shift: 459.67,
    },
  }
};

const measure: TbMeasure<TemperatureUnits> = {
  METRIC,
  IMPERIAL
};

export default measure;
