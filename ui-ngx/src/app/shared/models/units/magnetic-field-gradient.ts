import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type MagneticFieldGradientUnits = MagneticFieldGradientMetricUnits;
export type MagneticFieldGradientMetricUnits = 'T/m' | 'G/cm';

const METRIC: TbMeasureUnits<MagneticFieldGradientMetricUnits> = {
  units: {
    'T/m': {
      name: 'unit.tesla-per-meter',
      tags: ['magnetic field', 'tesla per meter', 'T/m'],
      to_anchor: 1,
    },
    'G/cm': {
      name: 'unit.gauss-per-centimeter',
      tags: ['magnetic field', 'gauss per centimeter', 'G/cm'],
      to_anchor: 0.01,
    },
  },
};

const measure: TbMeasure<MagneticFieldGradientUnits> = {
  METRIC,
};

export default measure;
