// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type EnergyDensityUnits = 'J/m³';

const METRIC: TbMeasureUnits<EnergyDensityUnits> = {
  units: {
    'J/m³': {
      name: 'unit.joule-per-cubic-meter',
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<EnergyDensityUnits> = {
  METRIC,
};

export default measure;
