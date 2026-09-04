// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type SpecificEnergyUnits = 'J/kg';

const METRIC: TbMeasureUnits<SpecificEnergyUnits> = {
  units: {
    'J/kg': {
      name: 'unit.joule-per-kilogram',
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<SpecificEnergyUnits> = {
  METRIC,
};

export default measure;
