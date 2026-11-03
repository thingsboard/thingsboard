// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type SurfaceTensionUnits = 'N/m';

const METRIC: TbMeasureUnits<SurfaceTensionUnits> = {
  units: {
    'N/m': {
      name: 'unit.newton-per-meter',
      tags: ['linear density', 'force per unit length'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<SurfaceTensionUnits> = {
  METRIC,
};

export default measure;
