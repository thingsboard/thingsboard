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

export type PowerUnits = PowerMetricUnits | PowerImperialUnits;

export type PowerMetricUnits = 'W' | 'μW' | 'mW' | 'kW' | 'MW' | 'GW' | 'PS';
export type PowerImperialUnits =
  | 'ft-lb/s'
  | 'hp'
  | 'BTU/s'
  | 'BTU/h'
  | 'BTU/d'
  | 'MBtu/s'
  | 'MBtu/h'
  | 'MBtu/d'
  | 'MMBtu/s'
  | 'MMBtu/h'
  | 'MMBtu/d';

const METRIC: TbMeasureUnits<PowerMetricUnits> = {
  ratio: 0.737562149,
  units: {
    W: {
      name: 'unit.watt',
      tags: ['horsepower', 'performance', 'electricity'],
      to_anchor: 1,
    },
    μW: {
      name: 'unit.microwatt',
      tags: ['horsepower', 'performance', 'electricity'],
      to_anchor: 0.000001,
    },
    mW: {
      name: 'unit.milliwatt',
      tags: ['horsepower', 'performance', 'electricity'],
      to_anchor: 0.001,
    },
    kW: {
      name: 'unit.kilowatt',
      tags: ['horsepower', 'performance', 'electricity'],
      to_anchor: 1000,
    },
    MW: {
      name: 'unit.megawatt',
      tags: [ 'horsepower', 'performance', 'electricity'],
      to_anchor: 1000000,
    },
    GW: {
      name: 'unit.gigawatt',
      tags: ['horsepower', 'performance', 'electricity'],
      to_anchor: 1000000000,
    },
    PS: {
      name: 'unit.metric-horsepower',
      tags: ['performance'],
      to_anchor: 735.49875,
    },
  },
};

const IMPERIAL: TbMeasureUnits<PowerImperialUnits> = {
  ratio: 1 / 0.737562149,
  units: {
    'ft-lb/s': {
      name: 'unit.foot-pound-per-second',
      tags: ['mechanical power'],
      to_anchor: 1,
    },
    hp: {
      name: 'unit.horsepower',
      tags: ['performance', 'electricity'],
      to_anchor: 550,
    },
    'BTU/s': {
      name: 'unit.btu-per-second',
      tags: ['heat transfer', 'thermal energy'],
      to_anchor: 778.16937,
    },
    'BTU/h': {
      name: 'unit.btu-per-hour',
      tags: ['heat transfer', 'thermal energy', 'HVAC'],
      to_anchor: 0.216158,
    },
    'BTU/d': {
      name: 'unit.btu-per-day',
      tags: ['heat transfer', 'thermal energy'],
      to_anchor: 0.00900613,
    },
    'MBtu/s': {
      name: 'unit.btu-per-second',
      tags: ['heat transfer', 'thermal energy'],
      to_anchor: 778169.37,
    },
    'MBtu/h': {
      name: 'unit.btu-per-hour',
      tags: ['heat transfer', 'thermal energy'],
      to_anchor: 216.158,
    },
    'MBtu/d': {
      name: 'unit.btu-per-day',
      tags: ['heat transfer', 'thermal energy'],
      to_anchor: 9.00613,
    },
    'MMBtu/s': {
      name: 'unit.btu-per-second',
      tags: ['heat transfer', 'thermal energy'],
      to_anchor: 778169370,
    },
    'MMBtu/h': {
      name: 'unit.btu-per-hour',
      tags: ['heat transfer', 'thermal energy'],
      to_anchor: 216158,
    },
    'MMBtu/d': {
      name: 'unit.btu-per-day',
      tags: ['heat transfer', 'thermal energy'],
      to_anchor: 9006.13,
    },
  },
};

const measure: TbMeasure<PowerUnits> = {
  METRIC,
  IMPERIAL
};

export default measure;
