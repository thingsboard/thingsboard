import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type SugarContentUnits = '°Bx';

const METRIC: TbMeasureUnits<SugarContentUnits> = {
  units: {
    '°Bx': {
      name: 'unit.degrees-brix',
      tags: ['sugar content', 'fruit ripeness', 'Bx'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<SugarContentUnits> = {
  METRIC,
};

export default measure;
