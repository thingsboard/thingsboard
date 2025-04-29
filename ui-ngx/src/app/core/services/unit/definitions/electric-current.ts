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

export type ElectricCurrentUnits = ElectricCurrentMetricalUnits;

export type ElectricCurrentMetricalUnits = 'A' | 'nA' | 'μA' | 'mA' | 'kA' | 'MA' | 'GA';

const METRIC: TbMeasureUnits<ElectricCurrentMetricalUnits> = {
  units: {
    A: {
      name: 'unit.ampere',
      tags: ['electric current', 'current flow', 'flow of electricity', 'electrical flow', 'ampere', 'amperes', 'amperage', 'A'],
      to_anchor: 1,
    },
    nA: {
      name: 'unit.nanoampere',
      tags: ['electric current', 'nanoampere', 'nanoamperes', 'nA'],
      to_anchor: 1e-9,
    },
    μA: {
      name: 'unit.microampere',
      tags: ['electric current', 'microampere', 'microamperes', 'μA'],
      to_anchor: 1e-6,
    },
    mA: {
      name: 'unit.milliampere',
      tags: ['electric current', 'milliampere', 'milliamperes', 'mA'],
      to_anchor: 0.001,
    },
    kA: {
      name: 'unit.kiloampere',
      tags: ['electric current', 'kiloampere', 'kiloamperes', 'kA'],
      to_anchor: 1000,
    },
    MA: {
      name: 'unit.megaampere',
      tags: ['electric current', 'megaampere', 'megaamperes', 'MA'],
      to_anchor: 1e6,
    },
    GA: {
      name: 'unit.gigaampere',
      tags: ['electric current', 'gigaampere', 'gigaamperes', 'GA'],
      to_anchor: 1e9,
    },
  }
};

const measure: TbMeasure<ElectricCurrentUnits> = {
  METRIC,
};

export default measure;
