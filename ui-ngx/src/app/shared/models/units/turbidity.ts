// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type TurbidityUnits = 'NTU';

const METRIC: TbMeasureUnits<TurbidityUnits> = {
  units: {
    NTU: {
      name: 'unit.turbidity',
      tags: ['water turbidity', 'water clarity', 'Nephelometric Turbidity Units'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<TurbidityUnits> = {
  METRIC,
};

export default measure;
