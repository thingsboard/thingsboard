import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type RadiantIntensityUnits = RadiantIntensityMetricUnits;
export type RadiantIntensityMetricUnits = 'W/sr';

const METRIC: TbMeasureUnits<RadiantIntensityMetricUnits> = {
  units: {
    'W/sr': {
      name: 'unit.watt-per-steradian',
      tags: ['radiant intensity', 'power per unit solid angle', 'watt per steradian', 'W/sr'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<RadiantIntensityUnits> = {
  METRIC,
};

export default measure;
