import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type PolarizationMetricUnits = 'C·m²/V';

export type PolarizationUnits = PolarizationMetricUnits;

const METRIC: TbMeasureUnits<PolarizationMetricUnits> = {
  units: {
    'C·m²/V': {
      name: 'unit.coulomb-per-square-meter-per-volt',
      tags: ['polarization', 'electric field', 'coulomb per square meter per volt', 'C·m²/V'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<PolarizationUnits> = {
  METRIC,
};

export default measure;
