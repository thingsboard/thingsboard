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

export type AccelerationMetricUnits = 'g₀' | 'm/s²' | 'km/h²' | 'Gal';
export type AccelerationImperialUnits = 'ft/s²';

export type AccelerationUnits = AccelerationMetricUnits | AccelerationImperialUnits;

const METRIC: TbMeasureUnits<AccelerationMetricUnits> = {
  ratio: 3.28084,
  units: {
    'g₀': {
      name: 'unit.g-force',
      tags: ['gravity', 'load'],
      to_anchor: 9.80665,
    },
    'm/s²': {
      name: 'unit.meters-per-second-squared',
      tags: ['peak to peak', 'root mean square (RMS)', 'vibration'],
      to_anchor: 1,
    },
    Gal: {
      name: 'unit.gal',
      tags: ['gravity', 'g-force'],
      to_anchor: 1,
    },
    'km/h²': {
      name: 'unit.kilometer-per-hour-squared',
      tags: ['rate of change of velocity'],
      to_anchor: 1 / 12960,
    }
  }
};

const IMPERIAL: TbMeasureUnits<AccelerationImperialUnits> = {
  ratio: 1 / 3.28084,
  units: {
    'ft/s²': {
      name: 'unit.foot-per-second-squared',
      tags: ['rate of change of velocity'],
      to_anchor: 1
    }
  }
};

const measure: TbMeasure<AccelerationUnits> = {
  METRIC,
  IMPERIAL
};

export default measure;
