///
/// Copyright © 2016-2025 The Thingsboard Authors
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

export type FrequencyUnits = FrequencyMetricUnits;
export type FrequencyMetricUnits = 'mHz' | 'Hz' | 'kHz' | 'MHz' | 'GHz' | 'THz' | 'rpm' | 'deg/s' | 'rad/s';

const METRIC: TbMeasureUnits<FrequencyMetricUnits> = {
  units: {
    mHz: {
      name: 'unit.millihertz',
      tags: ['frequency', 'cycles per second', 'millihertz', 'mHz'],
      to_anchor: 1 / 1000,
    },
    Hz: {
      name: 'unit.hertz',
      tags: ['frequency', 'cycles per second', 'hertz', 'Hz'],
      to_anchor: 1,
    },
    kHz: {
      name: 'unit.kilohertz',
      tags: ['frequency', 'cycles per second', 'kilohertz', 'kHz'],
      to_anchor: 1000,
    },
    MHz: {
      name: 'unit.megahertz',
      tags: ['frequency', 'cycles per second', 'megahertz', 'MHz'],
      to_anchor: 1000 * 1000,
    },
    GHz: {
      name: 'unit.gigahertz',
      tags: ['frequency', 'cycles per second', 'gigahertz', 'GHz'],
      to_anchor: 1000 * 1000 * 1000,
    },
    THz: {
      name: 'unit.terahertz',
      tags: ['frequency', 'terahertz', 'THz'],
      to_anchor: 1000 * 1000 * 1000 * 1000,
    },
    rpm: {
      name: 'unit.rotation-per-minute',
      tags: ['frequency', 'rotation per minute', 'rotations per minute', 'rpm', 'angular velocity'],
      to_anchor: 1 / 60,
    },
    'deg/s': {
      name: 'unit.deg-per-second',
      tags: ['angular velocity', 'degrees per second', 'deg/s'],
      to_anchor: 1 / 360, // 1 deg/s = 1/360 Hz
    },
    'rad/s': {
      name: 'unit.radian-per-second',
      tags: ['angular velocity', 'rotation speed', 'rad/s'],
      to_anchor: 1 / (Math.PI * 2),
    },
  },
};

const measure: TbMeasure<FrequencyUnits> = {
  METRIC,
};

export default measure;
