// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type MagneticPermeabilityUnits = 'H/m' | 'G/Oe';

const METRIC: TbMeasureUnits<MagneticPermeabilityUnits> = {
  units: {
    'H/m': {
      name: 'unit.henry-per-meter',
      to_anchor: 1,
    },
    'G/Oe': {
      name: 'unit.gauss-per-oersted',
      tags: ['magnetic field'],
      to_anchor: 1/ 795774.715,
    },
  },
};


const measure: TbMeasure<MagneticPermeabilityUnits> = {
  METRIC,
};

export default measure;
