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

export type DynamicViscosityMetricUnits = 'Pa·s' | 'cP' | 'P' | 'N·s/m²' | 'dyn·s/cm²' | 'kg/(m·s)';
export type DynamicViscosityImperialUnits = 'lb/(ft·h)';

export type DynamicViscosityUnits = DynamicViscosityMetricUnits | DynamicViscosityImperialUnits;

const METRIC: TbMeasureUnits<DynamicViscosityMetricUnits> = {
  ratio: 2419.0883293091,
  units: {
    'Pa·s': {
      name: 'unit.pascal-second',
      tags: ['fluid mechanics'],
      to_anchor: 1,
    },
    cP: {
      name: 'unit.centipoise',
      tags: ['fluid mechanics'],
      to_anchor: 0.001,
    },
    P: {
      name: 'unit.poise',
      tags: ['fluid mechanics'],
      to_anchor: 0.1,
    },
    'N·s/m²': {
      name: 'unit.newton-second-per-square-meter',
      tags: ['fluid mechanics'],
      to_anchor: 1,
    },
    'dyn·s/cm²': {
      name: 'unit.dyne-second-per-square-centimeter',
      tags: ['fluid mechanics'],
      to_anchor: 0.1,
    },
    'kg/(m·s)': {
      name: 'unit.kilogram-per-meter-second',
      tags: ['fluid mechanics'],
      to_anchor: 1,
    },
  },
};

const IMPERIAL: TbMeasureUnits<DynamicViscosityImperialUnits> = {
  ratio: 0.00041337887,
  units: {
    'lb/(ft·h)': {
      name: 'unit.pound-per-foot-hour',
      tags: ['fluid mechanics'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<DynamicViscosityUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
