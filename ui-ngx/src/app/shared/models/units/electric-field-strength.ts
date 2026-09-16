// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ElectricFieldStrengthUnits = 'V/m' | 'mV/m' | 'kV/m';

const METRIC: TbMeasureUnits<ElectricFieldStrengthUnits> = {
  units: {
    'V/m': {
      name: 'unit.volts-per-meter',
      to_anchor: 1,
    },
    'mV/m': {
      name: 'unit.millivolts-per-meter',
      to_anchor: 1e-3,
    },
    'kV/m': {
      name: 'unit.kilovolts-per-meter',
      to_anchor: 1e3,
    },
  },
};

const measure: TbMeasure<ElectricFieldStrengthUnits> = {
  METRIC,
};

export default measure;
