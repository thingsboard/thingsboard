import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type LuminousIntensityMetricUnits = 'cd';

export type LuminousIntensityUnits = LuminousIntensityMetricUnits;

const METRIC: TbMeasureUnits<LuminousIntensityMetricUnits> = {
  units: {
    'cd': {
      name: 'unit.candela',
      tags: ['luminous intensity', 'light intensity', 'candela', 'cd'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<LuminousIntensityUnits> = {
  METRIC,
};

export default measure;
