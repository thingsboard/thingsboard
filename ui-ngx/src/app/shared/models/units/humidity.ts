import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type HumidityUnits = HumidityMetricUnits;
export type HumidityMetricUnits = 'g/kg';

const METRIC: TbMeasureUnits<HumidityMetricUnits> = {
  units: {
    'g/kg': {
      name: 'unit.gram-per-kilogram',
      tags: ['humidity', 'moisture', 'specific humidity', 'g/kg'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<HumidityUnits> = {
  METRIC,
};

export default measure;
