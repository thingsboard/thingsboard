import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type MolarEnergyMetricUnits = 'J/mol';

export type MolarEnergyUnits = MolarEnergyMetricUnits;

const METRIC: TbMeasureUnits<MolarEnergyMetricUnits> = {
  units: {
    'J/mol': {
      name: 'unit.joule-per-mole',
      tags: ['molar energy', 'joule per mole', 'J/mol'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<MolarEnergyUnits> = {
  METRIC,
};

export default measure;
