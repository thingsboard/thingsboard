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

export type RadiationDoseUnits = 'Gy' | 'Sv' | 'Rad' | 'Rem' | 'R' | 'C/kg' | 'cps';

const METRIC: TbMeasureUnits<RadiationDoseUnits> = {
  units: {
    Sv: {
      name: 'unit.sievert',
      tags: ['sievert', 'radiation dose equivalent', 'Sv'],
      to_anchor: 1,
    },
    Gy: {
      name: 'unit.gray',
      tags: ['absorbed dose', 'gray', 'Gy'],
      to_anchor: 1,
    },
    Rad: {
      name: 'unit.rad',
      tags: ['rad'],
      to_anchor: 0.01,
    },
    Rem: {
      name: 'unit.rem',
      tags: ['radiation dose equivalent'],
      to_anchor: 0.01,
    },
    R: {
      name: 'unit.roentgen',
      tags: ['radiation exposure'],
      to_anchor: 0.0093,
    },
    'C/kg': {
      name: 'unit.coulombs-per-kilogram',
      tags: ['radiation exposure', 'electric charge-to-mass ratio'],
      to_anchor: 34,
    },
    cps: {
      name: 'unit.cps',
      tags: ['radiation detection'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<RadiationDoseUnits> = {
  METRIC,
};

export default measure;
