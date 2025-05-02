import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type SpecificEnergyMetricUnits = 'J/kg';

export type SpecificEnergyUnits = SpecificEnergyMetricUnits;

const METRIC: TbMeasureUnits<SpecificEnergyMetricUnits> = {
  units: {
    'J/kg': {
      name: 'unit.joule-per-kilogram',
      tags: ['specific energy', 'specific energy capacity', 'joule per kilogram', 'J/kg'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<SpecificEnergyUnits> = {
  METRIC,
};

export default measure;
