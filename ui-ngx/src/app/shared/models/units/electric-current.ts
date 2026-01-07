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

export type ElectricCurrentUnits = 'A' | 'pA' | 'nA' | 'μA' | 'mA' | 'kA' | 'MA' | 'GA';

const METRIC: TbMeasureUnits<ElectricCurrentUnits> = {
  units: {
    A: {
      name: 'unit.ampere',
      tags: ['current flow', 'flow of electricity', 'electrical flow', 'amperes', 'amperage'],
      to_anchor: 1,
    },
    pA: {
      name: 'unit.picoampere',
      tags: ['picoamperes'],
      to_anchor: 1e-12,
    },
    nA: {
      name: 'unit.nanoampere',
      tags: ['nanoamperes'],
      to_anchor: 1e-9,
    },
    μA: {
      name: 'unit.microampere',
      tags: ['microamperes'],
      to_anchor: 1e-6,
    },
    mA: {
      name: 'unit.milliampere',
      tags: ['milliamperes'],
      to_anchor: 0.001,
    },
    kA: {
      name: 'unit.kiloampere',
      tags: ['kiloamperes'],
      to_anchor: 1000,
    },
    MA: {
      name: 'unit.megaampere',
      tags: ['megaamperes'],
      to_anchor: 1e6,
    },
    GA: {
      name: 'unit.gigaampere',
      tags: ['gigaamperes'],
      to_anchor: 1e9,
    },
  }
};

const measure: TbMeasure<ElectricCurrentUnits> = {
  METRIC,
};

export default measure;
