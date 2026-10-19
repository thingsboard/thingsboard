// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type LuminousIntensityUnits = 'cd';

const METRIC: TbMeasureUnits<LuminousIntensityUnits> = {
  units: {
    cd: {
      name: 'unit.candela',
      tags: ['light intensity'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<LuminousIntensityUnits> = {
  METRIC,
};

export default measure;
