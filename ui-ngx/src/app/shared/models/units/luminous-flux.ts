import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type LuminousFluxMetricUnits = 'lm';

export type LuminousFluxUnits = LuminousFluxMetricUnits;

const METRIC: TbMeasureUnits<LuminousFluxMetricUnits> = {
  units: {
    'lm': {
      name: 'unit.lumen',
      tags: ['luminous flux', 'total light output', 'lumen', 'lm'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<LuminousFluxUnits> = {
  METRIC,
};

export default measure;
