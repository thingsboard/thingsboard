import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type CurrentDensityMetricUnits = 'µA/cm²' | 'A/m²';

export type CurrentDensityUnits = CurrentDensityMetricUnits;

const METRIC: TbMeasureUnits<CurrentDensityMetricUnits> = {
  units: {
    'µA/cm²': {
      name: 'unit.microampere-per-square-centimeter',
      tags: ['current density', 'microampere per square centimeter', 'µA/cm²'],
      to_anchor: 10000,
    },
    'A/m²': {
      name: 'unit.ampere-per-square-meter',
      tags: ['current density', 'current per unit area', 'ampere per square meter', 'A/m²'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<CurrentDensityUnits> = {
  METRIC,
};

export default measure;
