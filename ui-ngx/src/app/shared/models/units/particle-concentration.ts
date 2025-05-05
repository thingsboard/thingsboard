import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ParticleConcentrationUnits = 'particles/mL';

const METRIC: TbMeasureUnits<ParticleConcentrationUnits> = {
  units: {
    'particles/mL': {
      name: 'unit.particle-density',
      tags: ['particle concentration', 'count', 'particles/mL'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<ParticleConcentrationUnits> = {
  METRIC,
};

export default measure;
