// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type RadiantIntensityUnits = 'W/sr';

const METRIC: TbMeasureUnits<RadiantIntensityUnits> = {
  units: {
    'W/sr': {
      name: 'unit.watt-per-steradian',
      tags: ['power per unit solid angle'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<RadiantIntensityUnits> = {
  METRIC,
};

export default measure;
