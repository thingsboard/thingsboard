import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type PHUnits = 'pH';

const METRIC: TbMeasureUnits<PHUnits> = {
  units: {
    pH: {
      name: 'unit.ph-level',
      tags: ['acidity', 'alkalinity', 'neutral', 'acid', 'base', 'pH', 'soil pH', 'water quality', 'water pH'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<PHUnits> = {
  METRIC,
};

export default measure;
