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

export type PartsPerMillionUnits = 'ppm' | 'ppb';

const METRIC: TbMeasureUnits<PartsPerMillionUnits> = {
  units: {
    ppm: {
      name: 'unit.ppm',
      tags: ['carbon dioxide', 'co²', 'carbon monoxide', 'co', 'aqi', 'air quality', 'total volatile organic compounds', 'tvoc'],
      to_anchor: 1,
    },
    ppb: {
      name: 'unit.ppb',
      tags: ['ozone', 'o³', 'nitrogen dioxide', 'no²', 'sulfur dioxide', 'so²', 'aqi', 'air quality', 'tvoc'],
      to_anchor: 0.001,
    }
  },
};

const measure: TbMeasure<PartsPerMillionUnits> = {
  METRIC,
};

export default measure;
