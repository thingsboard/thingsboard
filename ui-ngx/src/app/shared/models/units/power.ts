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

export type PowerUnits = PowerMetricUnits | PowerImperialUnits;

export type PowerMetricUnits = 'W' | 'μW' | 'mW' | 'kW' | 'MW' | 'GW' | 'PS';
export type PowerImperialUnits = 'BTU/s' | 'ft-lb/s' | 'hp' | 'BTU/h';

const METRIC: TbMeasureUnits<PowerMetricUnits> = {
  ratio: 0.737562149,
  units: {
    W: {
      name: 'unit.watt',
      tags: ['power', 'horsepower', 'performance', 'watt', 'watts', 'electricity', 'W'],
      to_anchor: 1,
    },
    μW: {
      name: 'unit.microwatt',
      tags: ['power', 'horsepower', 'performance', 'microwatt', 'microwatts', 'electricity', 'μW'],
      to_anchor: 0.000001,
    },
    mW: {
      name: 'unit.milliwatt',
      tags: ['power', 'horsepower', 'performance', 'milliwatt', 'milliwatts', 'electricity', 'mW'],
      to_anchor: 0.001,
    },
    kW: {
      name: 'unit.kilowatt',
      tags: ['power', 'horsepower', 'performance', 'kilowatt', 'kilowatts', 'electricity', 'kW'],
      to_anchor: 1000,
    },
    MW: {
      name: 'unit.megawatt',
      tags: ['power', 'horsepower', 'performance', 'megawatt', 'megawatts', 'electricity', 'MW'],
      to_anchor: 1000000,
    },
    GW: {
      name: 'unit.gigawatt',
      tags: ['power', 'horsepower', 'performance', 'gigawatt', 'gigawatts', 'electricity', 'GW'],
      to_anchor: 1000000000,
    },
    PS: {
      name: 'unit.metric-horsepower',
      tags: ['power', 'performance', 'metric horsepower', 'PS'],
      to_anchor: 735.49875,
    },
  },
};

const IMPERIAL: TbMeasureUnits<PowerImperialUnits> = {
  ratio: 1 / 0.737562149,
  units: {
    'BTU/s': {
      name: 'unit.btu-per-second',
      tags: ['power', 'heat transfer', 'thermal energy', 'british thermal unit per second', 'Btu/s'],
      to_anchor: 778.16937,
    },
    'ft-lb/s': {
      name: 'unit.foot-pound-per-second',
      tags: ['power', 'foot-pound per second', 'foot-pounds per second', 'ft-lb/s', 'mechanical power'],
      to_anchor: 1,
    },
    hp: {
      name: 'unit.horsepower',
      tags: ['power', 'horsepower', 'performance', 'electricity', 'horsepowers', 'hp'],
      to_anchor: 550,
    },
    'BTU/h': {
      name: 'unit.btu-per-hour',
      tags: ['power', 'heat transfer', 'thermal energy', 'HVAC', 'BTU/h'],
      to_anchor: 0.216158,
    },
  },
};

const measure: TbMeasure<PowerUnits> = {
  METRIC,
  IMPERIAL
};

export default measure;
