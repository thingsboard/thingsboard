// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type AmountOfSubstanceUnits = 'mol' | 'nmol' | 'μmol' | 'mmol' | 'kmol';

const METRIC: TbMeasureUnits<AmountOfSubstanceUnits> = {
  units: {
    mol: {
      name: 'unit.mole',
      tags: ['chemical amount'],
      to_anchor: 1,
    },
    nmol: {
      name: 'unit.nanomole',
      tags: ['chemical amount'],
      to_anchor: 0.000000001,
    },
    μmol: {
      name: 'unit.micromole',
      tags: ['chemical amount'],
      to_anchor: 0.000001,
    },
    mmol: {
      name: 'unit.millimole',
      tags: ['chemical amount'],
      to_anchor: 0.001,
    },
    kmol: {
      name: 'unit.kilomole',
      tags: ['chemical amount'],
      to_anchor: 1000,
    },
  },
};

const measure: TbMeasure<AmountOfSubstanceUnits> = {
  METRIC,
};

export default measure;
