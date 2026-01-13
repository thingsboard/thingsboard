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

export type ElectricalConductanceUnits = 'S' | 'mS' | 'μS' | 'kS' | 'MS' | 'GS';

const METRIC: TbMeasureUnits<ElectricalConductanceUnits> = {
  units: {
    S: {
      name: 'unit.siemens',
      to_anchor: 1,
    },
    mS: {
      name: 'unit.millisiemens',
      to_anchor: 1e-3,
    },
    μS: {
      name: 'unit.microsiemens',
      to_anchor: 1e-6,
    },
    kS: {
      name: 'unit.kilosiemens',
      to_anchor: 1e3,
    },
    MS: {
      name: 'unit.megasiemens',
      to_anchor: 1e6,
    },
    GS: {
      name: 'unit.gigasiemens',
      to_anchor: 1e9,
    },
  },
};

const measure: TbMeasure<ElectricalConductanceUnits> = {
  METRIC,
};

export default measure;
