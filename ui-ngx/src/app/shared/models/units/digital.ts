// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type DigitalUnits = 'bit' | 'B' | 'KB' | 'MB' | 'GB' | 'TB' | 'PB' | 'EB' | 'ZB' | 'YB';

const METRIC: TbMeasureUnits<DigitalUnits> = {
  units: {
    bit: {
      name: 'unit.bit',
      tags: ['data', 'binary digit', 'information'],
      to_anchor: 1.25e-1,
    },
    B: {
      name: 'unit.byte',
      tags: ['data', 'information', 'storage', 'memory'],
      to_anchor: 1
    },
    KB: {
      name: 'unit.kilobyte',
      tags: ['data'],
      to_anchor: 1024,
    },
    MB: {
      name: 'unit.megabyte',
      tags: ['data'],
      to_anchor: 1024 ** 2,
    },
    GB: {
      name: 'unit.gigabyte',
      tags: ['data'],
      to_anchor: 1024 ** 3,
    },
    TB: {
      name: 'unit.terabyte',
      tags: ['data'],
      to_anchor: 1024 ** 4,
    },
    PB: {
      name: 'unit.petabyte',
      tags: ['data'],
      to_anchor: 1024 ** 5,
    },
    EB: {
      name: 'unit.exabyte',
      tags: ['data'],
      to_anchor: 1024 ** 6,
    },
    ZB: {
      name: 'unit.zettabyte',
      tags: ['data'],
      to_anchor: 1024 ** 7,
    },
    YB: {
      name: 'unit.yottabyte',
      tags: ['data'],
      to_anchor: 1024 ** 8,
    },
  }
};

const measure: TbMeasure<DigitalUnits> = {
  METRIC,
};

export default measure;
