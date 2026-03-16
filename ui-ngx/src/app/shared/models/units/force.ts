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

export type ForceUnits = ForceMetricUnits | ForceImperialUnits;

export type ForceMetricUnits = 'N' | 'kN' | 'dyn';
export type ForceImperialUnits = 'lbf' | 'kgf' | 'klbf' | 'pdl' | 'kip';

const METRIC: TbMeasureUnits<ForceMetricUnits> = {
  ratio: 0.224809,
  units: {
    N: {
      name: 'unit.newton',
      tags: ['pressure', 'push', 'pull', 'weight'],
      to_anchor: 1,
    },
    kN: {
      name: 'unit.kilonewton',
      to_anchor: 1000,
    },
    dyn: {
      name: 'unit.dyne',
      to_anchor: 0.00001,
    },
  },
};

const IMPERIAL: TbMeasureUnits<ForceImperialUnits> = {
  ratio: 4.44822,
  units: {
    lbf: {
      name: 'unit.pound-force',
      to_anchor: 1,
    },
    kgf: {
      name: 'unit.kilogram-force',
      to_anchor: 2.20462,
    },
    klbf: {
      name: 'unit.kilopound-force',
      to_anchor: 1000,
    },
    pdl: {
      name: 'unit.poundal',
      to_anchor: 0.031081,
    },
    kip: {
      name: 'unit.kip',
      to_anchor: 1000,
    },
  },
};

const measure: TbMeasure<ForceUnits> = {
  METRIC,
  IMPERIAL
};

export default measure;
