import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type LuminousEfficacyMetricUnits = 'lm/W';

export type LuminousEfficacyUnits = LuminousEfficacyMetricUnits;

const METRIC: TbMeasureUnits<LuminousEfficacyMetricUnits> = {
  units: {
    'lm/W': {
      name: 'unit.lumens-per-watt',
      tags: ['luminous efficacy', 'lighting efficiency', 'lumens per watt', 'lm/W'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<LuminousEfficacyUnits> = {
  METRIC,
};

export default measure;
