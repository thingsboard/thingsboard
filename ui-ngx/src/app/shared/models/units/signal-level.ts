// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type SignalLevelUnits = 'dBmV' | 'dBm' | 'rssi';

const METRIC: TbMeasureUnits<SignalLevelUnits> = {
  units: {
    dBmV: {
      name: 'unit.dbmV',
      tags: ['decibels millivolt', 'voltage level'],
      to_anchor: 1,
    },
    dBm: {
      name: 'unit.dbm',
      tags: ['decibel milliwatts', 'output power'],
      to_anchor: 1,
    },
    rssi: {
      name: 'unit.rssi',
      tags: ['signal strength', 'received signal strength indicator'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<SignalLevelUnits> = {
  METRIC,
};

export default measure;
