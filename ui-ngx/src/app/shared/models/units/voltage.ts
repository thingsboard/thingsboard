// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type VoltageUnits = VoltageMetricUnits;
export type VoltageMetricUnits = 'pV' | 'nV' | 'μV' | 'mV' | 'V' | 'kV' | 'MV';

const METRIC: TbMeasureUnits<VoltageMetricUnits> = {
  units: {
    pV: {
      name: 'unit.picovolt',
      tags: ['volts'],
      to_anchor: 1e-12,
    },
    nV: {
      name: 'unit.nanovolt',
      tags: ['volts'],
      to_anchor: 1e-9,
    },
    μV: {
      name: 'unit.microvolt',
      tags: ['electric potential', 'electric tension'],
      to_anchor: 1e-6,
    },
    mV: {
      name: 'unit.millivolt',
      tags: ['electric potential', 'electric tension'],
      to_anchor: 0.001,
    },
    V: {
      name: 'unit.volt',
      tags: ['electric potential', 'electric tension', 'power source', 'battery', 'battery level'],
      to_anchor: 1,
    },
    kV: {
      name: 'unit.kilovolt',
      tags: ['electric potential', 'electric tension'],
      to_anchor: 1000,
    },
    MV: {
      name: 'unit.megavolt',
      tags: ['electric potential', 'electric tension'],
      to_anchor: 1e6,
    },
  },
};

const measure: TbMeasure<VoltageUnits> = {
  METRIC,
};

export default measure;
