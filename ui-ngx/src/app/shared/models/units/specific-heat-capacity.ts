import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type SpecificHeatCapacityMetricUnits = 'J/(kg·K)';

export type SpecificHeatCapacityUnits = SpecificHeatCapacityMetricUnits;

const METRIC: TbMeasureUnits<SpecificHeatCapacityMetricUnits> = {
  units: {
    'J/(kg·K)': {
      name: 'unit.joule-per-kilogram-kelvin',
      tags: ['specific heat capacity', 'heat capacity per unit mass and temperature', 'joule per kilogram-kelvin', 'J/(kg·K)'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<SpecificHeatCapacityUnits> = {
  METRIC,
};

export default measure;
