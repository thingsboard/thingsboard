// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type MolarConcentrationUnits = 'mol/m³';

const METRIC: TbMeasureUnits<MolarConcentrationUnits> = {
  units: {
    'mol/m³': {
      name: 'unit.mole-per-cubic-meter',
      tags: ['amount of substance per unit volume'],
      to_anchor: 1,
    }
  },
};

const measure: TbMeasure<MolarConcentrationUnits> = {
  METRIC,
};

export default measure;
