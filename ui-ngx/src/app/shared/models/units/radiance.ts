// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type RadianceUnits = 'W/(m²·sr)';

const METRIC: TbMeasureUnits<RadianceUnits> = {
  units: {
    'W/(m²·sr)': {
      name: 'unit.watt-per-square-metre-steradian',
      tags: ['radiant flux density'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<RadianceUnits> = {
  METRIC,
};

export default measure;
