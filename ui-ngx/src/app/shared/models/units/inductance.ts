import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type InductanceMetricUnits = 'H' | 'mH' | 'µH' | 'nH' | 'T·m/A';
export type InductanceUnits = InductanceMetricUnits;

const METRIC: TbMeasureUnits<InductanceMetricUnits> = {
  units: {
    H: {
      name: 'unit.henry',
      tags: ['inductance', 'magnetic induction', 'H'],
      to_anchor: 1,
    },
    mH: {
      name: 'unit.millihenry',
      tags: ['inductance', 'millihenry', 'mH'],
      to_anchor: 0.001,
    },
    µH: {
      name: 'unit.microhenry',
      tags: ['inductance', 'microhenry', 'µH'],
      to_anchor: 1e-6,
    },
    nH: {
      name: 'unit.nanohenry',
      tags: ['inductance', 'nanohenry', 'nH'],
      to_anchor: 1e-9,
    },
    'T·m/A': {
      name: 'unit.tesla-meter-per-ampere',
      tags: ['magnetic field', 'Tesla Meter per Ampere', 'T·m/A', 'magnetic flux'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<InductanceUnits> = {
  METRIC,
};

export default measure;
