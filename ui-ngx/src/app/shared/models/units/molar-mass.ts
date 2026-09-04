// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type MolarMassUnits = 'kg/mol' | 'g/mol' | 'mg/mol';

const METRIC: TbMeasureUnits<MolarMassUnits> = {
  units: {
    'g/mol': {
      name: 'unit.gram-per-mole',
      to_anchor: 1,
    },
    'kg/mol': {
      name: 'unit.kilogram-per-mole',
      to_anchor: 1e3,
    },
    'mg/mol': {
      name: 'unit.milligram-per-mole',
      to_anchor: 1e-3,
    },
  },
};

const measure: TbMeasure<MolarMassUnits> = {
  METRIC,
};

export default measure;
