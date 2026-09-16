// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type DensityMetricUnits = 'kg/m³' | 'g/cm³' | 'mg/dL' | 'g/m³' | 'mg/mL' | 'mg/L' | 'mg/m³' | 'µg/m³';
export type DensityImperialUnits = 'lb/ft³' | 'oz/in³' | 'ton/yd³';

export type DensityUnits = DensityMetricUnits | DensityImperialUnits;

const METRIC: TbMeasureUnits<DensityMetricUnits> = {
  ratio: 0.062428,
  units: {
    'kg/m³': {
      name: 'unit.kilogram-per-cubic-meter',
      tags: ['mass per unit volume'],
      to_anchor: 1,
    },
    'g/cm³': {
      name: 'unit.gram-per-cubic-centimeter',
      tags: ['mass per unit volume'],
      to_anchor: 1000,
    },
    'mg/dL': {
      name: 'unit.milligrams-per-deciliter',
      tags: ['glucose', 'blood sugar', 'glucose level', 'concentration'],
      to_anchor: 0.01,
    },
    'g/m³': {
      name: 'unit.gram-per-cubic-meter',
      tags: ['humidity', 'moisture', 'absolute humidity', 'concentration'],
      to_anchor: 0.001,
    },
    'mg/L': {
      name: 'unit.mg-per-liter',
      tags: ['dissolved oxygen', 'water quality', 'mg/L', 'concentration'],
      to_anchor: 0.001,
    },
    'mg/mL': {
      name: 'unit.milligram-per-milliliter',
      tags: ['mass per unit volume', 'concentration'],
      to_anchor: 1,
    },
    'mg/m³': {
      name: 'unit.milligram-per-cubic-meter',
      tags: ['mass per unit volume', 'concentration'],
      to_anchor: 1e-6,
    },
    'µg/m³': {
      name: 'unit.micrograms-per-cubic-meter',
      tags: ['coarse particulate matter', 'pm10', 'fine particulate matter', 'pm2.5', 'aqi', 'air quality', 'total volatile organic compounds', 'tvoc', 'concentration'],
      to_anchor: 1e-9,
    },
  },
};

const IMPERIAL: TbMeasureUnits<DensityImperialUnits> = {
  ratio: 1 / 0.062428,
  units: {
    'lb/ft³': {
      name: 'unit.pound-per-cubic-foot',
      tags: ['mass per unit volume'],
      to_anchor: 1,
    },
    'oz/in³': {
      name: 'unit.ounces-per-cubic-inch',
      tags: ['mass per unit volume'],
      to_anchor: 1728,
    },
    'ton/yd³': {
      name: 'unit.tons-per-cubic-yard',
      tags: ['mass per unit volume'],
      to_anchor: 74.074,
    },
  },
};

const measure: TbMeasure<DensityUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
