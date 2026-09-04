// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type SolidAngleUnits = 'sr';

const METRIC: TbMeasureUnits<SolidAngleUnits> = {
  units: {
    sr: {
      name: 'unit.steradian',
      tags: ['spatial extent'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<SolidAngleUnits> = {
  METRIC,
};

export default measure;
