// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type MolarEnergyUnits = 'J/mol';

const METRIC: TbMeasureUnits<MolarEnergyUnits> = {
  units: {
    'J/mol': {
      name: 'unit.joule-per-mole',
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<MolarEnergyUnits> = {
  METRIC,
};

export default measure;
