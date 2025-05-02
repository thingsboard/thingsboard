import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type AmountOfSubstanceMetricUnits = 'mol' | 'nmol' | 'μmol' | 'mmol' | 'kmol';

export type AmountOfSubstanceUnits = AmountOfSubstanceMetricUnits;

const METRIC: TbMeasureUnits<AmountOfSubstanceMetricUnits> = {
  units: {
    'mol': {
      name: 'unit.mole',
      tags: ['amount of substance', 'chemical amount', 'mole', 'mol'],
      to_anchor: 1,
    },
    'nmol': {
      name: 'unit.nanomole',
      tags: ['amount of substance', 'nanomole', 'nmol'],
      to_anchor: 0.000000001,
    },
    'μmol': {
      name: 'unit.micromole',
      tags: ['amount of substance', 'micromole', 'μmol'],
      to_anchor: 0.000001,
    },
    'mmol': {
      name: 'unit.millimole',
      tags: ['amount of substance', 'millimole', 'mmol'],
      to_anchor: 0.001,
    },
    'kmol': {
      name: 'unit.kilomole',
      tags: ['amount of substance', 'kilomole', 'kmol'],
      to_anchor: 1000,
    },
  },
};

const measure: TbMeasure<AmountOfSubstanceUnits> = {
  METRIC,
};

export default measure;
