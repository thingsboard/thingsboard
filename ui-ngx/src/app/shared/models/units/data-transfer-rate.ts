// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type DataTransferRateUnits = 'bps' | 'kbps' | 'Mbps' | 'Gbps' | 'Tbps' | 'B/s' | 'KB/s' | 'MB/s' | 'GB/s';

const METRIC: TbMeasureUnits<DataTransferRateUnits> = {
  units: {
    bps: {
      name: 'unit.bit-per-second',
      to_anchor: 1,
    },
    kbps: {
      name: 'unit.kilobit-per-second',
      to_anchor: 1e3,
    },
    Mbps: {
      name: 'unit.megabit-per-second',
      to_anchor: 1e6,
    },
    Gbps: {
      name: 'unit.gigabit-per-second',
      to_anchor: 1e9,
    },
    Tbps: {
      name: 'unit.terabit-per-second',
      to_anchor: 1e12,
    },
    'B/s': {
      name: 'unit.byte-per-second',
      to_anchor: 8,
    },
    'KB/s': {
      name: 'unit.kilobyte-per-second',
      to_anchor: 8e3,
    },
    'MB/s': {
      name: 'unit.megabyte-per-second',
      to_anchor: 8e6,
    },
    'GB/s': {
      name: 'unit.gigabyte-per-second',
      to_anchor: 8e9,
    },
  },
};

const measure: TbMeasure<DataTransferRateUnits> = {
  METRIC,
};

export default measure;
