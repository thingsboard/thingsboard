import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type SolidAngleMetricUnits = 'sr';

export type SolidAngleUnits = SolidAngleMetricUnits;

const METRIC: TbMeasureUnits<SolidAngleMetricUnits> = {
  units: {
    'sr': {
      name: 'unit.steradian',
      tags: ['solid angle', 'spatial extent', 'steradian', 'sr'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<SolidAngleUnits> = {
  METRIC,
};

export default measure;
