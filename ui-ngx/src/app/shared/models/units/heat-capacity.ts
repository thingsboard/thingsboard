// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type HeatCapacityUnits = 'J/K';

const METRIC: TbMeasureUnits<HeatCapacityUnits> = {
  units: {
    'J/K': {
      name: 'unit.joule-per-kelvin',
      tags: ['heat capacity per unit temperature'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<HeatCapacityUnits> = {
  METRIC,
};

export default measure;
