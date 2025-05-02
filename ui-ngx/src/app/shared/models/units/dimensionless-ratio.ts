import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type DimensionlessRatioMetricUnits = 'm/m';

export type DimensionRatioUnits = DimensionlessRatioMetricUnits;

const METRIC: TbMeasureUnits<DimensionlessRatioMetricUnits> = {
  units: {
    'm/m': {
      name: 'unit.meter-per-meter',
      tags: ['ratio of length to length', 'meter per meter', 'm/m'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<DimensionRatioUnits> = {
  METRIC,
};

export default measure;
