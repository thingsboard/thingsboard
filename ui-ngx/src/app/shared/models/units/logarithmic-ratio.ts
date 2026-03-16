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

export type LogarithmicRatioUnits = 'dB' | 'bel' | 'Np';

const METRIC: TbMeasureUnits<LogarithmicRatioUnits> = {
  units: {
    dB: {
      name: 'unit.decibel',
      tags: ['noise level', 'sound level', 'volume', 'acoustics'],
      to_anchor: 1,
    },
    bel: {
      name: 'unit.bel',
      tags: ['power ratio', 'intensity ratio'],
      to_anchor: 10,
    },
    Np: {
      name: 'unit.neper',
      tags: ['gain', 'loss', 'attenuation'],
      to_anchor: 8.685889638,
    },
  },
};

const measure: TbMeasure<LogarithmicRatioUnits> = {
  METRIC,
};

export default measure;
