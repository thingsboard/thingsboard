// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ElectricPolarizabilityUnits = 'C·m²/V';

const METRIC: TbMeasureUnits<ElectricPolarizabilityUnits> = {
  units: {
    'C·m²/V': {
      name: 'unit.coulomb-per-square-meter-per-volt',
      tags: ['electric field'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<ElectricPolarizabilityUnits> = {
  METRIC,
};

export default measure;
