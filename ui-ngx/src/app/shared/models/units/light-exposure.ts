import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type LightExposureMetricUnits = 'lx·s';

export type LightExposureUnits = LightExposureMetricUnits;

const METRIC: TbMeasureUnits<LightExposureMetricUnits> = {
  units: {
    'lx·s': {
      name: 'unit.lux-second',
      tags: ['light exposure', 'illuminance over time', 'lux-second', 'lx·s'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<LightExposureUnits> = {
  METRIC,
};

export default measure;
