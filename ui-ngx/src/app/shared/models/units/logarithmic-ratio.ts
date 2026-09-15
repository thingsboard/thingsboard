// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type LogarithmicRatioUnits = 'dB' | 'bel' | 'Np';

const METRIC: TbMeasureUnits<LogarithmicRatioUnits> = {
  units: {
    dB: {
      name: 'unit.decibel',
      tags: ['noise level', 'sound level', 'volume', 'acoustics'],
      to_anchor: 1,
    },
    bel: {
      name: 'unit.bel',
      tags: ['power ratio', 'intensity ratio'],
      to_anchor: 10,
    },
    Np: {
      name: 'unit.neper',
      tags: ['gain', 'loss', 'attenuation'],
      to_anchor: 8.685889638,
    },
  },
};

const measure: TbMeasure<LogarithmicRatioUnits> = {
  METRIC,
};

export default measure;
