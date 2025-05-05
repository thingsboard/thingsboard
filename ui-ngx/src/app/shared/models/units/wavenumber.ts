import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type WavenumberMetricUnits = 'm⁻¹';

export type WavenumberUnits = WavenumberMetricUnits;

const METRIC: TbMeasureUnits<WavenumberMetricUnits> = {
  units: {
    'm⁻¹': {
      name: 'unit.reciprocal-metre',
      tags: ['wavenumber', 'wave density', 'wave frequency', 'm⁻¹'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<WavenumberUnits> = {
  METRIC,
};

export default measure;
