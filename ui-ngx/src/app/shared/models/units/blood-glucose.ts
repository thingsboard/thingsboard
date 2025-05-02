import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type BloodGlucoseUnits = BloodGlucoseMetricUnits;
export type BloodGlucoseMetricUnits = 'mg/dL';

const METRIC: TbMeasureUnits<BloodGlucoseMetricUnits> = {
  units: {
    'mg/dL': {
      name: 'unit.milligrams-per-deciliter',
      tags: ['glucose', 'blood sugar', 'glucose level', 'mg/dL'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<BloodGlucoseUnits> = {
  METRIC,
};

export default measure;
