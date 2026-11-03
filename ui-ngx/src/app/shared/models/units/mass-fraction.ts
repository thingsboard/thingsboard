// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type MassFractionUnits = '°Bx';

const METRIC: TbMeasureUnits<MassFractionUnits> = {
  units: {
    '°Bx': {
      name: 'unit.degrees-brix',
      tags: ['sugar content', 'fruit ripeness'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<MassFractionUnits> = {
  METRIC,
};

export default measure;
