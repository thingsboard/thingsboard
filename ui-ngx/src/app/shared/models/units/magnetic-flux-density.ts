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

export type MagneticFluxDensityUnits = 'T' | 'mT' | 'μT' | 'nT' | 'kT' | 'MT' | 'G' | 'kG' | 'γ' | 'A/m' | 'Oe';

const METRIC: TbMeasureUnits<MagneticFluxDensityUnits> = {
  units: {
    T: {
      name: 'unit.tesla',
      tags: ['magnetic field strength'],
      to_anchor: 1,
    },
    mT: {
      name: 'unit.millitesla',
      tags: ['magnetic field strength'],
      to_anchor: 0.001,
    },
    μT: {
      name: 'unit.microtesla',
      tags: ['magnetic field strength'],
      to_anchor: 0.000001,
    },
    nT: {
      name: 'unit.nanotesla',
      tags: ['magnetic field strength'],
      to_anchor: 0.000000001,
    },
    kT: {
      name: 'unit.kilotesla',
      tags: ['magnetic field strength'],
      to_anchor: 1000,
    },
    MT: {
      name: 'unit.megatesla',
      tags: ['magnetic field strength'],
      to_anchor: 1000000,
    },
    G: {
      name: 'unit.gauss',
      tags: ['magnetic field strength'],
      to_anchor: 0.0001,
    },
    kG: {
      name: 'unit.kilogauss',
      tags: ['magnetic field strength'],
      to_anchor: 0.1,
    },
    γ: {
      name: 'unit.gamma',
      to_anchor: 0.000000001,
    },
    'A/m': {
      name: 'unit.ampere-per-meter',
      tags: ['magnetic field strength', 'magnetic field intensity'],
      to_anchor: 0.00000125663706143591,
    },
    Oe: {
      name: 'unit.oersted',
      tags: ['magnetic field strength'],
      to_anchor: 0.0001,
    },
  },
};

const measure: TbMeasure<MagneticFluxDensityUnits> = {
  METRIC,
};

export default measure;
