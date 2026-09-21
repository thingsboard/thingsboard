// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type LinerChargeDensityUnits = 'C/m';

const METRIC: TbMeasureUnits<LinerChargeDensityUnits> = {
  units: {
    'C/m': {
      name: 'unit.coulomb-per-meter',
      tags: ['electric displacement field per length'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<LinerChargeDensityUnits> = {
  METRIC,
};

export default measure;
