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

export type IlluminanceUnits = IlluminanceMetricUnits | IlluminanceImperialUnits;

export type IlluminanceMetricUnits = 'lx' | 'cd/m²' | 'lm/m²';
export type IlluminanceImperialUnits = 'fc';

const METRIC: TbMeasureUnits<IlluminanceMetricUnits> = {
  ratio: 1 / 10.76391,
  units: {
    lx: {
      name: 'unit.lux',
      tags: ['light level on a surface', 'illuminance', 'Lux', 'lx'],
      to_anchor: 1,
    },
    'cd/m²': {
      name: 'unit.candela-per-square-meter',
      tags: ['brightness', 'light level', 'Luminance'],
      to_anchor: 1,
    },
    'lm/m²': {
      name: 'unit.lumen-per-square-meter',
      tags: ['light level'],
      to_anchor: 1,
    },
  },
};

const IMPERIAL: TbMeasureUnits<IlluminanceImperialUnits> = {
  ratio: 10.76391,
  units: {
    fc: {
      name: 'unit.foot-candle',
      tags: ['illuminance', 'light level'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<IlluminanceUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
