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

export type AmountOfSubstanceUnits = 'mol' | 'nmol' | 'μmol' | 'mmol' | 'kmol';

const METRIC: TbMeasureUnits<AmountOfSubstanceUnits> = {
  units: {
    mol: {
      name: 'unit.mole',
      tags: ['chemical amount'],
      to_anchor: 1,
    },
    nmol: {
      name: 'unit.nanomole',
      tags: ['chemical amount'],
      to_anchor: 0.000000001,
    },
    μmol: {
      name: 'unit.micromole',
      tags: ['chemical amount'],
      to_anchor: 0.000001,
    },
    mmol: {
      name: 'unit.millimole',
      tags: ['chemical amount'],
      to_anchor: 0.001,
    },
    kmol: {
      name: 'unit.kilomole',
      tags: ['chemical amount'],
      to_anchor: 1000,
    },
  },
};

const measure: TbMeasure<AmountOfSubstanceUnits> = {
  METRIC,
};

export default measure;
