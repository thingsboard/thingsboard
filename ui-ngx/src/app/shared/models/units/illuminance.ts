// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type IlluminanceUnits = IlluminanceMetricUnits | IlluminanceImperialUnits;

export type IlluminanceMetricUnits = 'lx' | 'cd/m²' | 'lm/m²';
export type IlluminanceImperialUnits = 'fc';

const METRIC: TbMeasureUnits<IlluminanceMetricUnits> = {
  ratio: 1 / 10.76391,
  units: {
    lx: {
      name: 'unit.lux',
      tags: ['light level on a surface', 'illuminance', 'Lux', 'lx'],
      to_anchor: 1,
    },
    'cd/m²': {
      name: 'unit.candela-per-square-meter',
      tags: ['brightness', 'light level', 'Luminance'],
      to_anchor: 1,
    },
    'lm/m²': {
      name: 'unit.lumen-per-square-meter',
      tags: ['light level'],
      to_anchor: 1,
    },
  },
};

const IMPERIAL: TbMeasureUnits<IlluminanceImperialUnits> = {
  ratio: 10.76391,
  units: {
    fc: {
      name: 'unit.foot-candle',
      tags: ['illuminance', 'light level'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<IlluminanceUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
