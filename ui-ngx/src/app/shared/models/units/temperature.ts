// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type TemperatureMetricUnits = '°C' | 'K';
export type TemperatureImperialUnits = '°F' | '°R';

export type TemperatureUnits =
  | TemperatureMetricUnits
  | TemperatureImperialUnits;

const METRIC: TbMeasureUnits<TemperatureMetricUnits> = {
  transform: (C) => C / (5 / 9) + 32,
  units: {
    '°C': {
      name: 'unit.celsius',
      tags: ['heat', 'cold', 'warmth', 'degrees', 'shipment condition'],
      to_anchor: 1,
    },
    K: {
      name: 'unit.kelvin',
      tags: ['heat', 'cold', 'warmth', 'degrees', 'color quality', 'white balance', 'color temperature'],
      to_anchor: 1,
      anchor_shift: 273.15,
    },
  }
};

const IMPERIAL: TbMeasureUnits<TemperatureImperialUnits> = {
  transform: (F) => (F - 32) * (5 / 9),
  units: {
    '°F': {
      name: 'unit.fahrenheit',
      tags: ['heat', 'cold', 'warmth', 'degrees'],
      to_anchor: 1,
    },
    '°R': {
      name: 'unit.rankine',
      tags: ['heat', 'cold', 'warmth'],
      to_anchor: 1,
      anchor_shift: 459.67,
    },
  }
};

const measure: TbMeasure<TemperatureUnits> = {
  METRIC,
  IMPERIAL
};

export default measure;
