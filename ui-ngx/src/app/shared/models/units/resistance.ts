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

export type ResistanceUnits = 'Ω' | 'μΩ' | 'mΩ' | 'kΩ' | 'MΩ' | 'GΩ';

const METRIC: TbMeasureUnits<ResistanceUnits> = {
  units: {
    Ω: {
      name: 'unit.ohm',
      tags: ['electrical resistance', 'impedance'],
      to_anchor: 1,
    },
    μΩ: {
      name: 'unit.microohm',
      tags: ['electrical resistance'],
      to_anchor: 0.000001,
    },
    mΩ: {
      name: 'unit.milliohm',
      tags: ['electrical resistance'],
      to_anchor: 0.001,
    },
    kΩ: {
      name: 'unit.kilohm',
      tags: ['electrical resistance'],
      to_anchor: 1000,
    },
    MΩ: {
      name: 'unit.megohm',
      tags: ['electrical resistance'],
      to_anchor: 1000000,
    },
    GΩ: {
      name: 'unit.gigohm',
      tags: ['electrical resistance'],
      to_anchor: 1000000000,
    },
  },
};

const measure: TbMeasure<ResistanceUnits> = {
  METRIC,
};

export default measure;
