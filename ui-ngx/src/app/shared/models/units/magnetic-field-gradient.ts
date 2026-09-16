// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type MagneticFieldGradientUnits = 'T/m' | 'G/cm';

const METRIC: TbMeasureUnits<MagneticFieldGradientUnits> = {
  units: {
    'T/m': {
      name: 'unit.tesla-per-meter',
      to_anchor: 1,
    },
    'G/cm': {
      name: 'unit.gauss-per-centimeter',
      to_anchor: 0.01,
    },
  },
};

const measure: TbMeasure<MagneticFieldGradientUnits> = {
  METRIC,
};

export default measure;
