import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type SpecificHeatCapacityUnits = 'J/(kg·K)';

const METRIC: TbMeasureUnits<SpecificHeatCapacityUnits> = {
  units: {
    'J/(kg·K)': {
      name: 'unit.joule-per-kilogram-kelvin',
      tags: ['heat capacity per unit mass and temperature'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<SpecificHeatCapacityUnits> = {
  METRIC,
};

export default measure;
