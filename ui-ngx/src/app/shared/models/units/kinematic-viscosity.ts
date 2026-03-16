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

export type KinematicViscosityMetricUnits = 'm²/s' | 'cm²/s' | 'St' | 'cSt';
export type KinematicViscosityImperialUnits = 'ft²/s' | 'in²/s';

export type KinematicViscosityUnits = KinematicViscosityMetricUnits | KinematicViscosityImperialUnits;

const METRIC: TbMeasureUnits<KinematicViscosityMetricUnits> = {
  ratio: 10.7639104167097,
  units: {
    'm²/s': {
      name: 'unit.square-meter-per-second',
      to_anchor: 1,
    },
    'cm²/s': {
      name: 'unit.square-centimeter-per-second',
      to_anchor: 1e-4,
    },
    St: {
      name: 'unit.stoke',
      to_anchor: 1e-4,
    },
    cSt: {
      name: 'unit.centistokes',
      to_anchor: 1e-6,
    },
  },
};

const IMPERIAL: TbMeasureUnits<KinematicViscosityImperialUnits> = {
  ratio: 0.09290304,
  units: {
    'ft²/s': {
      name: 'unit.square-foot-per-second',
      to_anchor: 0.09290304,
    },
    'in²/s': {
      name: 'unit.square-inch-per-second',
      to_anchor: 0.00064516,
    },
  },
};

const measure: TbMeasure<KinematicViscosityUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
