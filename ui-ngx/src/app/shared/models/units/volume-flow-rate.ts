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

export type VolumeFlowRateUnits = VolumeFlowRateMetricUnits | VolumeFlowRateImperialUnits;

export type VolumeFlowRateMetricUnits =
  | 'dm³/s'
  | 'mL/min'
  | 'L/s'
  | 'L/min'
  | 'L/hr'
  | 'm³/s'
  | 'm³/hr';

export type VolumeFlowRateImperialUnits =
  | 'fl-oz/s'
  | 'ft³/s'
  | 'ft³/min'
  | 'gal/hr'
  | 'GPM';

const METRIC: TbMeasureUnits<VolumeFlowRateMetricUnits> = {
  ratio: 33.8140227,
  units: {
    'dm³/s': {
      name: 'unit.cubic-decimeter-per-second',
      tags: ['volume flow', 'cubic decimeter per second', 'dm3/s'],
      to_anchor: 1,
    },
    'mL/min': {
      name: 'unit.milliliters-per-minute',
      tags: ['volume flow', 'flow rate', 'fluid dynamics', 'milliliters per minute', 'mL/min'],
      to_anchor: 1 / 60000,
    },
    'L/s': {
      name: 'unit.liter-per-second',
      tags: ['volume flow', 'airflow', 'ventilation', 'HVAC', 'gas flow rate', 'liter per second', 'L/s'],
      to_anchor: 1,
    },
    'L/min': {
      name: 'unit.liter-per-minute',
      tags: ['volume flow', 'airflow', 'ventilation', 'HVAC', 'gas flow rate', 'liter per minute', 'L/min'],
      to_anchor: 1 / 60,
    },
    'L/hr': {
      name: 'unit.liters-per-hour',
      tags: ['volume flow', 'fuel consumption', 'liter per hour', 'L/hr'],
      to_anchor: 1 / 3600,
    },
    'm³/s': {
      name: 'unit.cubic-meters-per-second',
      tags: ['volume flow', 'airflow', 'ventilation', 'HVAC', 'gas flow rate', 'cubic meters per second', 'm³/s'],
      to_anchor: 1000,
    },
    'm³/hr': {
      name: 'unit.cubic-meters-per-hour',
      tags: ['volume flow', 'airflow', 'ventilation', 'HVAC', 'gas flow rate', 'cubic meters per hour', 'm³/hr'],
      to_anchor: 5 / 18,
    },
  },
};

const IMPERIAL: TbMeasureUnits<VolumeFlowRateImperialUnits> = {
  ratio: 1 / 33.8140227,
  units: {
    'fl-oz/s': {
      name: 'unit.fluid-ounce-per-second',
      tags: ['volume flow', 'fluid ounce per second', 'fl-oz/s'],
      to_anchor: 1,
    },
    'ft³/s': {
      name: 'unit.cubic-foot-per-second',
      tags: ['volume flow', 'flow rate', 'fluid flow', 'cubic foot per second', 'cubic feet per second', 'ft³/s'],
      to_anchor: 957.506,
    },
    'ft³/min': {
      name: 'unit.cubic-foot-per-minute',
      tags: ['volume flow', 'airflow', 'ventilation', 'HVAC', 'gas flow rate', 'CFM', 'flow rate', 'fluid flow', 'cubic foot per minute', 'ft³/min'],
      to_anchor: 957.506 / 60,
    },
    'gal/hr': {
      name: 'unit.gallons-per-hour',
      tags: ['volume flow', 'fuel consumption', 'gallons per hour', 'gal/hr'],
      to_anchor: 128 / 3600,
    },
    'GPM': {
      name: 'unit.gallons-per-minute',
      tags: ['volume flow', 'airflow', 'ventilation', 'HVAC', 'gas flow rate', 'gallons per minute', 'GPM'],
      to_anchor: 128 / 60,
    },
  },
};

const measure: TbMeasure<VolumeFlowRateUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
