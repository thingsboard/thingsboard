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

export type VolumeFlowUnits = VolumeFlowMetricUnits | VolumeFlowImperialUnits;

export type VolumeFlowMetricUnits =
  | 'dm³/s'
  | 'mL/min'
  | 'L/s'
  | 'L/min'
  | 'L/hr'
  | 'm³/s'
  | 'm³/hr';

export type VolumeFlowImperialUnits =
  | 'fl-oz/s'
  | 'ft³/s'
  | 'ft³/min'
  | 'gal/hr'
  | 'GPM';

const METRIC: TbMeasureUnits<VolumeFlowMetricUnits> = {
  ratio: 33.8140227,
  units: {
    'L/s': {
      name: 'unit.liter-per-second',
      tags: ['airflow', 'ventilation', 'HVAC', 'gas flow rate'],
      to_anchor: 1,
    },
    'dm³/s': {
      name: 'unit.cubic-decimeter-per-second',
      tags: ['cubic decimeter per second'],
      to_anchor: 1,
    },
    'mL/min': {
      name: 'unit.milliliters-per-minute',
      tags: ['flow rate', 'fluid dynamics'],
      to_anchor: 1 / 60000,
    },
    'L/min': {
      name: 'unit.liter-per-minute',
      tags: ['airflow', 'ventilation', 'HVAC', 'gas flow rate'],
      to_anchor: 1 / 60,
    },
    'L/hr': {
      name: 'unit.liters-per-hour',
      tags: ['fuel consumption'],
      to_anchor: 1 / 3600,
    },
    'm³/s': {
      name: 'unit.cubic-meters-per-second',
      tags: ['airflow', 'ventilation', 'HVAC', 'gas flow rate'],
      to_anchor: 1000,
    },
    'm³/hr': {
      name: 'unit.cubic-meters-per-hour',
      tags: ['airflow', 'ventilation', 'HVAC', 'gas flow rate'],
      to_anchor: 5 / 18,
    },
  },
};

const IMPERIAL: TbMeasureUnits<VolumeFlowImperialUnits> = {
  ratio: 1 / 33.8140227,
  units: {
    'fl-oz/s': {
      name: 'unit.fluid-ounce-per-second',
      tags: ['fluid ounce per second', 'fl-oz/s'],
      to_anchor: 1,
    },
    'ft³/s': {
      name: 'unit.cubic-foot-per-second',
      tags: ['flow rate', 'fluid flow'],
      to_anchor: 957.506,
    },
    'ft³/min': {
      name: 'unit.cubic-foot-per-minute',
      tags: ['airflow', 'ventilation', 'HVAC', 'gas flow rate', 'CFM', 'flow rate', 'fluid flow'],
      to_anchor: 957.506 / 60,
    },
    'gal/hr': {
      name: 'unit.gallons-per-hour',
      tags: ['fuel consumption'],
      to_anchor: 128 / 3600,
    },
    'GPM': {
      name: 'unit.gallons-per-minute',
      tags: ['airflow', 'ventilation', 'HVAC', 'gas flow rate'],
      to_anchor: 128 / 60,
    },
  },
};

const measure: TbMeasure<VolumeFlowUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
