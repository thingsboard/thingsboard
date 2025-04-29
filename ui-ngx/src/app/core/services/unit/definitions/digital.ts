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

export type DigitalUnits = DigitalMetricUnits;

export type DigitalMetricUnits = 'bit' | 'B' | 'KB' | 'MB' | 'GB' | 'TB' | 'PB' | 'EB' | 'ZB' | 'YB';

const METRIC: TbMeasureUnits<DigitalMetricUnits> = {
  units: {
    bit: {
      name: 'unit.bit',
      tags: ['data', 'binary digit', 'information', 'bit'],
      to_anchor: 1.25e-1,
    },
    B: {
      name: 'unit.byte',
      tags: ['data', 'byte', 'information', 'storage', 'memory', 'B'],
      to_anchor: 1
    },
    KB: {
      name: 'unit.kilobyte',
      tags: ['data', 'kilobyte', 'KB'],
      to_anchor: 1024,
    },
    MB: {
      name: 'unit.megabyte',
      tags: ['data', 'megabyte', 'MB'],
      to_anchor: 1024 ** 2,
    },
    GB: {
      name: 'unit.gigabyte',
      tags: ['data', 'gigabyte', 'GB'],
      to_anchor: 1024 ** 3,
    },
    TB: {
      name: 'unit.terabyte',
      tags: ['data', 'terabyte', 'TB'],
      to_anchor: 1024 ** 4,
    },
    PB: {
      name: 'unit.petabyte',
      tags: ['data', 'petabyte', 'PB'],
      to_anchor: 1024 ** 5,
    },
    EB: {
      name: 'unit.exabyte',
      tags: ['data', 'exabyte', 'EB'],
      to_anchor: 1024 ** 6,
    },
    ZB: {
      name: 'unit.zettabyte',
      tags: ['data', 'zettabyte', 'EB'],
      to_anchor: 1024 ** 7,
    },
    YB: {
      name: 'unit.yottabyte',
      tags: ['data', 'yottabyte', 'EB'],
      to_anchor: 1024 ** 8,
    },
  }
};

const measure: TbMeasure<DigitalUnits> = {
  METRIC,
};

export default measure;
