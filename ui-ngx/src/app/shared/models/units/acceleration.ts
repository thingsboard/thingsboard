// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type AccelerationMetricUnits = 'g₀' | 'm/s²' | 'km/h²' | 'Gal';
export type AccelerationImperialUnits = 'ft/s²';

export type AccelerationUnits = AccelerationMetricUnits | AccelerationImperialUnits;

const METRIC: TbMeasureUnits<AccelerationMetricUnits> = {
  ratio: 3.28084,
  units: {
    'g₀': {
      name: 'unit.g-force',
      tags: ['gravity', 'load'],
      to_anchor: 9.80665,
    },
    'm/s²': {
      name: 'unit.meters-per-second-squared',
      tags: ['peak to peak', 'root mean square (RMS)', 'vibration'],
      to_anchor: 1,
    },
    Gal: {
      name: 'unit.gal',
      tags: ['gravity', 'g-force'],
      to_anchor: 1,
    },
    'km/h²': {
      name: 'unit.kilometer-per-hour-squared',
      tags: ['rate of change of velocity'],
      to_anchor: 1 / 12960,
    }
  }
};

const IMPERIAL: TbMeasureUnits<AccelerationImperialUnits> = {
  ratio: 1 / 3.28084,
  units: {
    'ft/s²': {
      name: 'unit.foot-per-second-squared',
      tags: ['rate of change of velocity'],
      to_anchor: 1
    }
  }
};

const measure: TbMeasure<AccelerationUnits> = {
  METRIC,
  IMPERIAL
};

export default measure;
