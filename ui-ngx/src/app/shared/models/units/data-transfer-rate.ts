import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type DataTransferRateUnits = 'bps' | 'kbps' | 'Mbps' | 'Gbps' | 'Tbps' | 'B/s' | 'KB/s' | 'MB/s' | 'GB/s';

const METRIC: TbMeasureUnits<DataTransferRateUnits> = {
  units: {
    'bps': {
      name: 'unit.bit-per-second',
      tags: ['data transfer rate', 'bps'],
      to_anchor: 1,
    },
    'kbps': {
      name: 'unit.kilobit-per-second',
      tags: ['data transfer rate', 'kbps'],
      to_anchor: 1e3,
    },
    'Mbps': {
      name: 'unit.megabit-per-second',
      tags: ['data transfer rate', 'Mbps'],
      to_anchor: 1e6,
    },
    'Gbps': {
      name: 'unit.gigabit-per-second',
      tags: ['data transfer rate', 'Gbps'],
      to_anchor: 1e9,
    },
    'Tbps': {
      name: 'unit.terabit-per-second',
      tags: ['data transfer rate', 'Tbps'],
      to_anchor: 1e12,
    },
    'B/s': {
      name: 'unit.byte-per-second',
      tags: ['data transfer rate', 'B/s'],
      to_anchor: 8,
    },
    'KB/s': {
      name: 'unit.kilobyte-per-second',
      tags: ['data transfer rate', 'KB/s'],
      to_anchor: 8e3,
    },
    'MB/s': {
      name: 'unit.megabyte-per-second',
      tags: ['data transfer rate', 'MB/s'],
      to_anchor: 8e6,
    },
    'GB/s': {
      name: 'unit.gigabyte-per-second',
      tags: ['data transfer rate', 'GB/s'],
      to_anchor: 8e9,
    },
  },
};

const measure: TbMeasure<DataTransferRateUnits> = {
  METRIC,
};

export default measure;
