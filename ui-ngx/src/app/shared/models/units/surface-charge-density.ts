// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type SurfaceChargeDensityUnits = 'C/m²';

const METRIC: TbMeasureUnits<SurfaceChargeDensityUnits> = {
  units: {
    'C/m²': {
      name: 'unit.coulomb-per-square-meter',
      tags: ['electric surface charge density'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<SurfaceChargeDensityUnits> = {
  METRIC,
};

export default measure;
