// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type NumberConcentrationUnits = 'particles/mL';

const METRIC: TbMeasureUnits<NumberConcentrationUnits> = {
  units: {
    'particles/mL': {
      name: 'unit.particle-density',
      tags: ['particle concentration', 'count'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<NumberConcentrationUnits> = {
  METRIC,
};

export default measure;
