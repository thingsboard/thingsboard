import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type MagneticMomentMetricUnits = 'A·m²' | 'μB';

export type MagneticMomentUnits = MagneticMomentMetricUnits;

const METRIC: TbMeasureUnits<MagneticMomentMetricUnits> = {
  units: {
    'A·m²': {
      name: 'unit.magnetic-dipole-moment',
      tags: ['magnetic dipole', 'dipole moment', 'ampere square meter', 'A·m²'],
      to_anchor: 1,
    },
    'μB': {
      name: 'unit.bohr-magneton',
      tags: ['atomic physics', 'magnetic moment', 'bohr magneton', 'μB'],
      to_anchor: 9.274e-24,
    },
  },
};

const measure: TbMeasure<MagneticMomentUnits> = {
  METRIC,
};

export default measure;
