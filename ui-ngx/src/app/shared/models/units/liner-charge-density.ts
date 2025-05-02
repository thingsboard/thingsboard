import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type LinerChargeDensityMetricUnits = 'C/m';

export type LinerChargeDensityUnits = LinerChargeDensityMetricUnits;

const METRIC: TbMeasureUnits<LinerChargeDensityMetricUnits> = {
  units: {
    'C/m': {
      name: 'unit.coulomb-per-meter',
      tags: ['electric displacement field per length', 'coulomb per meter', 'C/m'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<LinerChargeDensityUnits> = {
  METRIC,
};

export default measure;
