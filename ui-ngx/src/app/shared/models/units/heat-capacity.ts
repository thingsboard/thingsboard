import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type HeatCapacityMetricUnits = 'J/K';

export type HeatCapacityUnits = HeatCapacityMetricUnits;

const METRIC: TbMeasureUnits<HeatCapacityMetricUnits> = {
  units: {
    'J/K': {
      name: 'unit.joule-per-kelvin',
      tags: ['specific heat capacity', 'heat capacity per unit temperature', 'joule per kelvin', 'J/K'],
      to_anchor: 1, // Base unit: J/K
    },
  },
};

const measure: TbMeasure<HeatCapacityUnits> = {
  METRIC,
};

export default measure;
