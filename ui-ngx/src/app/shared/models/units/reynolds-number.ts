import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ReynoldsNumberMetricUnits = 'Re';

export type ReynoldsNumberUnits = ReynoldsNumberMetricUnits;

const METRIC: TbMeasureUnits<ReynoldsNumberMetricUnits> = {
  units: {
    Re: {
      name: 'unit.reynolds',
      tags: ['fluid flow regime', 'fluid mechanics', 'reynolds', 'Re'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<ReynoldsNumberUnits> = {
  METRIC,
};

export default measure;
