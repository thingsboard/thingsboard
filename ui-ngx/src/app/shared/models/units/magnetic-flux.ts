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

export type MagneticFluxUnits = 'Wb' | 'µWb' | 'mWb' | 'Mx' | 'G·cm²' | 'kG·cm²';

const METRIC: TbMeasureUnits<MagneticFluxUnits> = {
  units: {
    Wb: {
      name: 'unit.weber',
      to_anchor: 1,
    },
    µWb: {
      name: 'unit.microweber',
      to_anchor: 1e-6,
    },
    mWb: {
      name: 'unit.milliweber',
      to_anchor: 1e-3,
    },
    Mx: {
      name: 'unit.maxwell',
      tags: ['magnetic field'],
      to_anchor: 1e-8,
    },
    'G·cm²': {
      name: 'unit.gauss-square-centimeter',
      to_anchor: 1e-8,
    },
    'kG·cm²': {
      name: 'unit.kilogauss-square-centimeter',
      to_anchor: 1e-5,
    },
  },
};

const measure: TbMeasure<MagneticFluxUnits> = {
  METRIC,
};

export default measure;
