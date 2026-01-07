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

export type PowerDensityMetricUnits = 'mW/cm²' | 'W/cm²' | 'kW/cm²' | 'mW/m²' | 'W/m²' | 'kW/m²';
export type PowerDensityImperialUnits = 'W/in²' | 'kW/in²';

export type PowerDensityUnits = PowerDensityMetricUnits | PowerDensityImperialUnits;

const METRIC: TbMeasureUnits<PowerDensityMetricUnits> = {
  ratio: 0.00064516,
  units: {
    'mW/cm²': {
      name: 'unit.milliwatt-per-square-centimeter',
      tags: ['radiation intensity', 'sunlight intensity', 'signal power', 'intensity', 'UV Intensity'],
      to_anchor: 10000,
    },
    'W/cm²': {
      name: 'unit.watt-per-square-centimeter',
      tags: ['intensity of power'],
      to_anchor: 10000,
    },
    'kW/cm²': {
      name: 'unit.kilowatt-per-square-centimeter',
      tags: ['intensity of power'],
      to_anchor: 10000000,
    },
    'mW/m²': {
      name: 'unit.milliwatt-per-square-meter',
      tags: ['intensity of power'],
      to_anchor: 0.001,
    },
    'W/m²': {
      name: 'unit.watt-per-square-meter',
      tags: ['intensity of power'],
      to_anchor: 1,
    },
    'kW/m²': {
      name: 'unit.kilowatt-per-square-meter',
      tags: ['intensity of power'],
      to_anchor: 1000,
    },
  },
};

const IMPERIAL: TbMeasureUnits<PowerDensityImperialUnits> = {
  ratio: 1 / 0.00064516,
  units: {
    'W/in²': {
      name: 'unit.watt-per-square-inch',
      tags: ['intensity of power'],
      to_anchor: 1,
    },
    'kW/in²': {
      name: 'unit.kilowatt-per-square-inch',
      tags: ['intensity of power'],
      to_anchor: 1000,
    },
  },
};

const measure: TbMeasure<PowerDensityUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
