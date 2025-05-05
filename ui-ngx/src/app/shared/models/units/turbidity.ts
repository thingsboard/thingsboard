import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type TurbidityUnits = TurbidityMetricUnits;
export type TurbidityMetricUnits = 'NTU';

const METRIC: TbMeasureUnits<TurbidityMetricUnits> = {
  units: {
    NTU: {
      name: 'unit.turbidity',
      tags: ['water turbidity', 'water clarity', 'Nephelometric Turbidity Units', 'NTU'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<TurbidityUnits> = {
  METRIC,
};

export default measure;
