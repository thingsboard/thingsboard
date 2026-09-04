// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ElectricFluxUnits = 'V·m' | 'kV·m' | 'MV·m' | 'µV·m' | 'mV·m' | 'nV·m';

const METRIC: TbMeasureUnits<ElectricFluxUnits> = {
  units: {
    'V·m': {
      name: 'unit.volt-meter',
      to_anchor: 1,
    },
    'kV·m': {
      name: 'unit.kilovolt-meter',
      to_anchor: 1000,
    },
    'MV·m': {
      name: 'unit.megavolt-meter',
      to_anchor: 1000000,
    },
    'µV·m': {
      name: 'unit.microvolt-meter',
      to_anchor: 0.000001,
    },
    'mV·m': {
      name: 'unit.millivolt-meter',
      to_anchor: 0.001,
    },
    'nV·m': {
      name: 'unit.nanovolt-meter',
      to_anchor: 0.000000001,
    },
  },
};

const measure: TbMeasure<ElectricFluxUnits> = {
  METRIC,
};

export default measure;
