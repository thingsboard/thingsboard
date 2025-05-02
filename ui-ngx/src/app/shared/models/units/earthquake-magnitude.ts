import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type EarthquakeMagnitudeUnits = 'richter';

const METRIC: TbMeasureUnits<EarthquakeMagnitudeUnits> = {
  units: {
    richter: {
      name: 'unit.richter-scale',
      tags: ['earthquake', 'seismic activity', 'richter'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<EarthquakeMagnitudeUnits> = {
  METRIC,
};

export default measure;
