// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ResistanceUnits = 'Ω' | 'μΩ' | 'mΩ' | 'kΩ' | 'MΩ' | 'GΩ';

const METRIC: TbMeasureUnits<ResistanceUnits> = {
  units: {
    Ω: {
      name: 'unit.ohm',
      tags: ['electrical resistance', 'impedance'],
      to_anchor: 1,
    },
    μΩ: {
      name: 'unit.microohm',
      tags: ['electrical resistance'],
      to_anchor: 0.000001,
    },
    mΩ: {
      name: 'unit.milliohm',
      tags: ['electrical resistance'],
      to_anchor: 0.001,
    },
    kΩ: {
      name: 'unit.kilohm',
      tags: ['electrical resistance'],
      to_anchor: 1000,
    },
    MΩ: {
      name: 'unit.megohm',
      tags: ['electrical resistance'],
      to_anchor: 1000000,
    },
    GΩ: {
      name: 'unit.gigohm',
      tags: ['electrical resistance'],
      to_anchor: 1000000000,
    },
  },
};

const measure: TbMeasure<ResistanceUnits> = {
  METRIC,
};

export default measure;
