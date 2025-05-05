import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type SignalStrengthMetricUnits = 'dBmV' | 'dBm' | 'rssi';

export type SignalStrengthUnits = SignalStrengthMetricUnits;

const METRIC: TbMeasureUnits<SignalStrengthMetricUnits> = {
  units: {
    'dBmV': {
      name: 'unit.dbmV',
      tags: ['decibels millivolt', 'voltage level', 'signal', 'dBmV'],
      to_anchor: 1,
    },
    'dBm': {
      name: 'unit.dbm',
      tags: ['decibel milliwatts', 'output power', 'signal', 'dBm'],
      to_anchor: 1,
    },
    'rssi': {
      name: 'unit.rssi',
      tags: ['signal strength', 'signal level', 'received signal strength indicator', 'rssi', 'dBm'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<SignalStrengthUnits> = {
  METRIC,
};

export default measure;
