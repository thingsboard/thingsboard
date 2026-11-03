// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type MagneticFluxUnits = 'Wb' | 'µWb' | 'mWb' | 'Mx' | 'G·cm²' | 'kG·cm²';

const METRIC: TbMeasureUnits<MagneticFluxUnits> = {
  units: {
    Wb: {
      name: 'unit.weber',
      to_anchor: 1,
    },
    µWb: {
      name: 'unit.microweber',
      to_anchor: 1e-6,
    },
    mWb: {
      name: 'unit.milliweber',
      to_anchor: 1e-3,
    },
    Mx: {
      name: 'unit.maxwell',
      tags: ['magnetic field'],
      to_anchor: 1e-8,
    },
    'G·cm²': {
      name: 'unit.gauss-square-centimeter',
      to_anchor: 1e-8,
    },
    'kG·cm²': {
      name: 'unit.kilogauss-square-centimeter',
      to_anchor: 1e-5,
    },
  },
};

const measure: TbMeasure<MagneticFluxUnits> = {
  METRIC,
};

export default measure;
