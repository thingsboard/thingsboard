import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type RadianceUnits = 'W/(m²·sr)';

const METRIC: TbMeasureUnits<RadianceUnits> = {
  units: {
    'W/(m²·sr)': {
      name: 'unit.watt-per-square-metre-steradian',
      tags: ['radiance', 'radiant flux density', 'wTape per square metre-steradian', 'W/(m²·sr)'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<RadianceUnits> = {
  METRIC,
};

export default measure;
