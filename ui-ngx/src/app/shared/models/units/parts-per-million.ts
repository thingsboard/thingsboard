// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type PartsPerMillionUnits = 'ppm' | 'ppb';

const METRIC: TbMeasureUnits<PartsPerMillionUnits> = {
  units: {
    ppm: {
      name: 'unit.ppm',
      tags: ['carbon dioxide', 'co²', 'carbon monoxide', 'co', 'aqi', 'air quality', 'total volatile organic compounds', 'tvoc'],
      to_anchor: 1,
    },
    ppb: {
      name: 'unit.ppb',
      tags: ['ozone', 'o³', 'nitrogen dioxide', 'no²', 'sulfur dioxide', 'so²', 'aqi', 'air quality', 'tvoc'],
      to_anchor: 0.001,
    }
  },
};

const measure: TbMeasure<PartsPerMillionUnits> = {
  METRIC,
};

export default measure;
