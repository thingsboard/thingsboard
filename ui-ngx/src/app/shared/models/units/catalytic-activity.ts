// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type CatalyticActivityUnits = 'kat';

const METRIC: TbMeasureUnits<CatalyticActivityUnits> = {
  units: {
    kat: {
      name: 'unit.katal',
      tags: ['catalytic activity', 'enzyme activity'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<CatalyticActivityUnits> = {
  METRIC,
};

export default measure;
