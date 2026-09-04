// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type MagneticMomentUnits = 'A·m²' | 'μB';

const METRIC: TbMeasureUnits<MagneticMomentUnits> = {
  units: {
    'A·m²': {
      name: 'unit.magnetic-dipole-moment',
      tags: ['magnetic dipole moment'],
      to_anchor: 1,
    },
    μB: {
      name: 'unit.bohr-magneton',
      tags: ['atomic physics'],
      to_anchor: 9.274e-24,
    },
  },
};

const measure: TbMeasure<MagneticMomentUnits> = {
  METRIC,
};

export default measure;
