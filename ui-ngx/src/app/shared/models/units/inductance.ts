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

export type InductanceMetricUnits = 'H' | 'mH' | 'µH' | 'nH' | 'T·m/A';
export type InductanceUnits = InductanceMetricUnits;

const METRIC: TbMeasureUnits<InductanceMetricUnits> = {
  units: {
    H: {
      name: 'unit.henry',
      tags: ['inductance', 'magnetic induction', 'H'],
      to_anchor: 1,
    },
    mH: {
      name: 'unit.millihenry',
      tags: ['inductance', 'millihenry', 'mH'],
      to_anchor: 0.001,
    },
    µH: {
      name: 'unit.microhenry',
      tags: ['inductance', 'microhenry', 'µH'],
      to_anchor: 1e-6,
    },
    nH: {
      name: 'unit.nanohenry',
      tags: ['inductance', 'nanohenry', 'nH'],
      to_anchor: 1e-9,
    },
    'T·m/A': {
      name: 'unit.tesla-meter-per-ampere',
      tags: ['magnetic field', 'Tesla Meter per Ampere', 'T·m/A', 'magnetic flux'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<InductanceUnits> = {
  METRIC,
};

export default measure;
