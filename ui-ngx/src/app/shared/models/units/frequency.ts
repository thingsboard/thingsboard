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

export type FrequencyUnits = FrequencyMetricUnits;
export type FrequencyMetricUnits = 'mHz' | 'Hz' | 'kHz' | 'MHz' | 'GHz' | 'THz' | 'rpm' | 'deg/s' | 'rad/s' | 'RPM' | 'λ' | 'bpm';

const METRIC: TbMeasureUnits<FrequencyMetricUnits> = {
  units: {
    mHz: {
      name: 'unit.millihertz',
      tags: ['cycles per second'],
      to_anchor: 1 / 1000,
    },
    Hz: {
      name: 'unit.hertz',
      tags: ['cycles per second'],
      to_anchor: 1,
    },
    kHz: {
      name: 'unit.kilohertz',
      tags: ['cycles per second'],
      to_anchor: 1000,
    },
    MHz: {
      name: 'unit.megahertz',
      tags: ['cycles per second'],
      to_anchor: 1000 * 1000,
    },
    GHz: {
      name: 'unit.gigahertz',
      tags: ['cycles per second'],
      to_anchor: 1000 * 1000 * 1000,
    },
    THz: {
      name: 'unit.terahertz',
      to_anchor: 1000 * 1000 * 1000 * 1000,
    },
    rpm: {
      name: 'unit.rotation-per-minute',
      tags: ['rotations per minute', 'angular velocity'],
      to_anchor: 1 / 60,
    },
    RPM: {
      name: 'unit.rpm',
      tags: ['rotational speed', 'angular velocity'],
      to_anchor: 1 / 60,
    },
    'λ': {
      name: 'unit.lambda',
      tags: ['wavelength'],
      to_anchor: 299792458,
    },
    bpm: {
      name: 'unit.beats-per-minute',
      tags: ['heart rate', 'pulse'],
      to_anchor: 0.0167
    },
    'deg/s': {
      name: 'unit.deg-per-second',
      tags: ['angular velocity'],
      to_anchor: 1 / 360,
    },
    'rad/s': {
      name: 'unit.radian-per-second',
      tags: ['angular velocity'],
      to_anchor: 1 / (Math.PI * 2),
    },
  },
};

const measure: TbMeasure<FrequencyUnits> = {
  METRIC,
};

export default measure;
