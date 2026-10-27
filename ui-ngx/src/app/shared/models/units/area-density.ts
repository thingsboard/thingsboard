// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type AreaDensityUnits = 'kg/m²';

const METRIC: TbMeasureUnits<AreaDensityUnits> = {
  units: {
    'kg/m²': {
      name: 'unit.kilogram-per-square-meter',
      tags: ['surface density', 'mass per unit area'],
      to_anchor: 1
    },
  },
};

const measure: TbMeasure<AreaDensityUnits> = {
  METRIC,
};

export default measure;
