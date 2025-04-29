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

export type ChargeUnits = ChargeMetricUnits;

export type ChargeMetricUnits = 'c' | 'mC' | 'μC' | 'nC' | 'pC';

const METRIC: TbMeasureUnits<ChargeMetricUnits> = {
  units: {
    c: {
      name: 'unit.coulomb',
      tags: ['charge', 'electricity', 'electrostatics', 'Coulomb', 'C'],
      to_anchor: 1,
    },
    mC: {
      name: 'unit.millicoulomb',
      tags: ['charge', 'electricity', 'electrostatics', 'millicoulombs', 'mC'],
      to_anchor: 1 / 1000,
    },
    μC: {
      name: 'unit.microcoulomb',
      tags: ['charge', 'electricity', 'electrostatics', 'microcoulomb', 'µC'],
      to_anchor: 1 / 1000000,
    },
    nC: {
      name: 'unit.nanocoulomb',
      tags: ['charge', 'electricity', 'electrostatics', 'nanocoulomb', 'nC'],
      to_anchor: 1e-9,
    },
    pC: {
      name: 'unit.picocoulomb',
      tags: ['charge', 'electricity', 'electrostatics', 'picocoulomb', 'pC'],
      to_anchor: 1e-12,
    },
  }
};

const measure: TbMeasure<ChargeUnits> = {
  METRIC,
};

export default measure;
