// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ElectricDipoleMomentUnits = 'C·m' | 'D';

const METRIC: TbMeasureUnits<ElectricDipoleMomentUnits> = {
  units: {
    'C·m': {
      name: 'unit.electric-dipole-moment',
      to_anchor: 1,
    },
    D: {
      name: 'unit.debye',
      tags: ['polarization'],
      to_anchor: 3.33564e-30
    },
  },
};

const measure: TbMeasure<ElectricDipoleMomentUnits> = {
  METRIC,
};

export default measure;
