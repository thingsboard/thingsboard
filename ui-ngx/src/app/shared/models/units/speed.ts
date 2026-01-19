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

export type SpeedUnits = SpeedMetricUnits | SpeedImperialUnits;

export type SpeedMetricUnits = 'm/s' | 'km/h' | 'mm/min' | 'm/min' | 'mm/s';
export type SpeedImperialUnits = 'mph' | 'kt' | 'ft/s' | 'ft/min' | 'in/s' | 'in/h';

const METRIC: TbMeasureUnits<SpeedMetricUnits> = {
  ratio: 1 / 1.609344,
  units: {
    'm/s': {
      name: 'unit.meter-per-second',
      tags: ['velocity', 'pace', 'peak', 'peak to peak', 'root mean square (RMS)', 'vibration', 'wind speed', 'weather'],
      to_anchor: 3.6,
    },
    'km/h': {
      name: 'unit.kilometer-per-hour',
      tags: ['velocity', 'pace'],
      to_anchor: 1,
    },
    'mm/min': {
      name: 'unit.millimeters-per-minute',
      tags: ['feed rate', 'cutting feed rate'],
      to_anchor: 0.00006,
    },
    'm/min': {
      name: 'unit.meter-per-minute',
      tags: ['velocity', 'pace'],
      to_anchor: 0.06,
    },
    'mm/s': {
      name: 'unit.millimeters-per-second',
      tags: ['velocity', 'vibration rate'],
      to_anchor: 0.0036,
    },
  },
};

const IMPERIAL: TbMeasureUnits<SpeedImperialUnits> = {
  ratio: 1.609344,
  units: {
    mph: {
      name: 'unit.mile-per-hour',
      tags: ['velocity', 'pace'],
      to_anchor: 1,
    },
    kt: {
      name: 'unit.knot',
      tags: ['velocity', 'pace'],
      to_anchor: 1.150779,
    },
    'ft/s': {
      name: 'unit.foot-per-second',
      tags: ['velocity', 'pace'],
      to_anchor: 0.681818,
    },
    'ft/min': {
      name: 'unit.foot-per-minute',
      tags: ['velocity', 'pace'],
      to_anchor: 0.0113636,
    },
    'in/s': {
      name: 'unit.inch-per-second',
      tags: ['velocity', 'pace'],
      to_anchor: 0.0568182,
    },
    'in/h': {
      name: 'unit.inch-per-hour',
      tags: ['velocity', 'pace'],
      to_anchor: 0.00001578,
    },
  },
};

const measure: TbMeasure<SpeedUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
