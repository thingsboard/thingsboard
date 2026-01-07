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

export type VoltageUnits = VoltageMetricUnits;
export type VoltageMetricUnits = 'pV' | 'nV' | 'μV' | 'mV' | 'V' | 'kV' | 'MV';

const METRIC: TbMeasureUnits<VoltageMetricUnits> = {
  units: {
    pV: {
      name: 'unit.picovolt',
      tags: ['volts'],
      to_anchor: 1e-12,
    },
    nV: {
      name: 'unit.nanovolt',
      tags: ['volts'],
      to_anchor: 1e-9,
    },
    μV: {
      name: 'unit.microvolt',
      tags: ['electric potential', 'electric tension'],
      to_anchor: 1e-6,
    },
    mV: {
      name: 'unit.millivolt',
      tags: ['electric potential', 'electric tension'],
      to_anchor: 0.001,
    },
    V: {
      name: 'unit.volt',
      tags: ['electric potential', 'electric tension', 'power source', 'battery', 'battery level'],
      to_anchor: 1,
    },
    kV: {
      name: 'unit.kilovolt',
      tags: ['electric potential', 'electric tension'],
      to_anchor: 1000,
    },
    MV: {
      name: 'unit.megavolt',
      tags: ['electric potential', 'electric tension'],
      to_anchor: 1e6,
    },
  },
};

const measure: TbMeasure<VoltageUnits> = {
  METRIC,
};

export default measure;
