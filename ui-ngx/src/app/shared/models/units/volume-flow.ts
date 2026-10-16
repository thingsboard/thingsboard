// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type VolumeFlowUnits = VolumeFlowMetricUnits | VolumeFlowImperialUnits;

export type VolumeFlowMetricUnits =
  | 'dm³/s'
  | 'mL/min'
  | 'L/s'
  | 'L/min'
  | 'L/hr'
  | 'm³/s'
  | 'm³/min'
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
    'm³/min': {
      name: 'unit.cubic-meters-per-minute',
      tags: ['airflow', 'ventilation', 'HVAC', 'gas flow rate'],
      to_anchor: 1000 / 60,
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
