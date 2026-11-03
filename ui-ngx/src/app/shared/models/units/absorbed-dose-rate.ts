// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type AbsorbedDoseRateUnits = 'Gy/s';

const METRIC: TbMeasureUnits<AbsorbedDoseRateUnits> = {
  units: {
    'Gy/s': {
      name: 'unit.gy-per-second',
      tags: ['radiation dose rate'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<AbsorbedDoseRateUnits> = {
  METRIC,
};

export default measure;
