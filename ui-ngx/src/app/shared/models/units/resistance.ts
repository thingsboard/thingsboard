import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ResistanceMetricUnits = 'Ω' | 'μΩ' | 'mΩ' | 'kΩ' | 'MΩ' | 'GΩ';

export type ResistanceUnits = ResistanceMetricUnits;

const METRIC: TbMeasureUnits<ResistanceMetricUnits> = {
  units: {
    'Ω': {
      name: 'unit.ohm',
      tags: ['electrical resistance', 'resistance', 'impedance', 'ohm'],
      to_anchor: 1,
    },
    'μΩ': {
      name: 'unit.microohm',
      tags: ['electrical resistance', 'resistance', 'microohm', 'μΩ'],
      to_anchor: 0.000001,
    },
    'mΩ': {
      name: 'unit.milliohm',
      tags: ['electrical resistance', 'resistance', 'milliohm', 'mΩ'],
      to_anchor: 0.001,
    },
    'kΩ': {
      name: 'unit.kilohm',
      tags: ['electrical resistance', 'resistance', 'kilohm', 'kΩ'],
      to_anchor: 1000,
    },
    'MΩ': {
      name: 'unit.megohm',
      tags: ['electrical resistance', 'resistance', 'megohm', 'MΩ'],
      to_anchor: 1000000,
    },
    'GΩ': {
      name: 'unit.gigohm',
      tags: ['electrical resistance', 'resistance', 'gigohm', 'GΩ'],
      to_anchor: 1000000000,
    },
  },
};

const measure: TbMeasure<ResistanceUnits> = {
  METRIC,
};

export default measure;
