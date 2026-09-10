// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type SpecificHumidityUnits = 'g/kg';

const METRIC: TbMeasureUnits<SpecificHumidityUnits> = {
  units: {
    'g/kg': {
      name: 'unit.gram-per-kilogram',
      tags: ['humidity', 'moisture'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<SpecificHumidityUnits> = {
  METRIC,
};

export default measure;
