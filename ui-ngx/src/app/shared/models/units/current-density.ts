// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type CurrentDensityUnits = 'µA/cm²' | 'A/m²';

const METRIC: TbMeasureUnits<CurrentDensityUnits> = {
  units: {
    'µA/cm²': {
      name: 'unit.microampere-per-square-centimeter',
      tags: ['current per unit area'],
      to_anchor: 10000,
    },
    'A/m²': {
      name: 'unit.ampere-per-square-meter',
      tags: ['current per unit area'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<CurrentDensityUnits> = {
  METRIC,
};

export default measure;
