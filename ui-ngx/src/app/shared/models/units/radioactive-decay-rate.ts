import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type RadioactiveDecayRateUnits = 'Bq/s' | 'Ci/s';

const METRIC: TbMeasureUnits<RadioactiveDecayRateUnits> = {
  transform: (Bq_s) => Bq_s / 3.7e10, // Convert Bq/s to Ci/s
  units: {
    'Bq/s': {
      name: 'unit.becquerels-per-second',
      tags: ['radioactive decay rate', 'becquerels per second', 'Bq/s'],
      to_anchor: 1,
    },
    'Ci/s': {
      name: 'unit.curies-per-second',
      tags: ['radioactive decay rate', 'curies per second', 'Ci/s'],
      to_anchor: 3.7e10,
    },
  }
};

const measure: TbMeasure<RadioactiveDecayRateUnits> = {
  METRIC,
};

export default measure;
