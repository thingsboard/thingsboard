import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ElectricMomentMetricUnits = 'C·m' | 'D';

export type ElectricMomentUnits = ElectricMomentMetricUnits;

const METRIC: TbMeasureUnits<ElectricMomentMetricUnits> = {
  units: {
    'C·m': {
      name: 'unit.electric-dipole-moment',
      tags: ['electric dipole', 'dipole moment', 'coulomb meter', 'C·m'],
      to_anchor: 1,
    },
    'D': {
      name: 'unit.debye',
      tags: ['polarization', 'electric dipole moment', 'debye', 'D'],
      to_anchor: 3.33564e-30
    },
  },
};

const measure: TbMeasure<ElectricMomentUnits> = {
  METRIC,
};

export default measure;
