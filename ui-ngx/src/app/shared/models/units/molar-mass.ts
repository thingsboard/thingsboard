import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type MolarMassUnits = 'kg/mol' | 'g/mol' | 'mg/mol';

const METRIC: TbMeasureUnits<MolarMassUnits> = {
  units: {
    'g/mol': {
      name: 'unit.gram-per-mole',
      tags: ['molar mass', 'gram per mole', 'g/mol'],
      to_anchor: 1,
    },
    'kg/mol': {
      name: 'unit.kilogram-per-mole',
      tags: ['molar mass', 'kilogram per mole', 'kg/mol'],
      to_anchor: 1e3,
    },
    'mg/mol': {
      name: 'unit.milligram-per-mole',
      tags: ['molar mass', 'milligram per mole', 'mg/mol'],
      to_anchor: 1e-3,
    },
  },
};

const measure: TbMeasure<MolarMassUnits> = {
  METRIC,
};

export default measure;
