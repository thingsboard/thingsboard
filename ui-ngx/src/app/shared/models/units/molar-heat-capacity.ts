import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type MolarHeatCapacityMetricUnits = 'J/(mol·K)';

export type MolarHeatCapacityUnits = MolarHeatCapacityMetricUnits;

const METRIC: TbMeasureUnits<MolarHeatCapacityMetricUnits> = {
  units: {
    'J/(mol·K)': {
      name: 'unit.joule-per-mole-kelvin',
      tags: ['molar heat capacity', 'joule per mole-kelvin', 'J/(mol·K)'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<MolarHeatCapacityUnits> = {
  METRIC,
};

export default measure;
