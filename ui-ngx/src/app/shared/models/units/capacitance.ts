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

export type CapacitanceUnits = 'F' | 'mF' | 'μF' | 'nF' | 'pF' | 'kF' | 'MF' | 'GF' | 'TF';

const METRIC: TbMeasureUnits<CapacitanceUnits> = {
  units: {
    F: {
      name: 'unit.farad',
      tags: ['electric capacitance'],
      to_anchor: 1,
    },
    mF: {
      name: 'unit.millifarad',
      tags: ['electric capacitance'],
      to_anchor: 1e-3,
    },
    μF: {
      name: 'unit.microfarad',
      tags: ['electric capacitance'],
      to_anchor: 1e-6,
    },
    nF: {
      name: 'unit.nanofarad',
      tags: ['electric capacitance'],
      to_anchor: 1e-9,
    },
    pF: {
      name: 'unit.picofarad',
      tags: ['electric capacitance'],
      to_anchor: 1e-12,
    },
    kF: {
      name: 'unit.kilofarad',
      tags: ['electric capacitance'],
      to_anchor: 1e3,
    },
    MF: {
      name: 'unit.megafarad',
      tags: ['electric capacitance'],
      to_anchor: 1e6,
    },
    GF: {
      name: 'unit.gigafarad',
      tags: ['electric capacitance'],
      to_anchor: 1e9,
    },
    TF: {
      name: 'unit.terfarad',
      tags: ['electric capacitance'],
      to_anchor: 1e12,
    },
  },
};

const measure: TbMeasure<CapacitanceUnits> = {
  METRIC,
};

export default measure;
