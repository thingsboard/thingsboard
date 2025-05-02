import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type FuelEfficiencyMetricUnits = 'km/L' | 'L/100km';
export type FuelEfficiencyImperialUnits = 'mpg' | 'gal/mi';

export type FuelEfficiencyUnits = FuelEfficiencyMetricUnits | FuelEfficiencyImperialUnits;

const METRIC: TbMeasureUnits<FuelEfficiencyMetricUnits> = {
  ratio: 2.35214583,
  units: {
    'km/L': {
      name: 'unit.kilometers-per-liter',
      tags: ['fuel efficiency', 'km/L'],
      to_anchor: 1,
    },
    'L/100km': {
      name: 'unit.liters-per-100-km',
      tags: ['fuel efficiency', 'L/100km'],
      to_anchor: 1,
      transform: (value) => 100 / value,
    },
  },
};

const IMPERIAL: TbMeasureUnits<FuelEfficiencyImperialUnits> = {
  ratio: 0.425144,
  units: {
    'mpg': {
      name: 'unit.miles-per-gallon',
      tags: ['fuel efficiency', 'mpg'],
      to_anchor: 0.425144,
    },
    'gal/mi': {
      name: 'unit.gallons-per-mile',
      tags: ['fuel efficiency', 'gal/mi'],
      to_anchor: 2.35214583,
    },
  },
};

const measure: TbMeasure<FuelEfficiencyUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
