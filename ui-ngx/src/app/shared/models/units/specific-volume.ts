// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type SpecificVolumeUnits = 'm³/kg';

const METRIC: TbMeasureUnits<SpecificVolumeUnits> = {
  units: {
    'm³/kg': {
      name: 'unit.cubic-meter-per-kilogram',
      tags: ['volume per unit mass'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<SpecificVolumeUnits> = {
  METRIC,
};

export default measure;
