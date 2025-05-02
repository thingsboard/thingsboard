import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type LogarithmicUnits = 'dB' | 'B' | 'Np';

const METRIC: TbMeasureUnits<LogarithmicUnits> = {
  units: {
    'dB': {
      name: 'unit.decibel',
      tags: ['noise level', 'sound level', 'volume', 'acoustics', 'decibel', 'dB'],
      to_anchor: 1,
    },
    'B': {
      name: 'unit.bel',
      tags: ['logarithmic unit', 'power ratio', 'intensity ratio', 'bel', 'B'],
      to_anchor: 10,
    },
    'Np': {
      name: 'unit.neper',
      tags: ['logarithmic unit', 'ratio', 'gain', 'loss', 'attenuation', 'neper', 'Np'],
      to_anchor: 8.685889638,
    },
  },
};

const measure: TbMeasure<LogarithmicUnits> = {
  METRIC,
};

export default measure;
