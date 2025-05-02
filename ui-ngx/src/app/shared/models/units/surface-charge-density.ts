import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type SurfaceChargeDensityMetricUnits = 'C/m²';

export type SurfaceChargeDensityUnits = SurfaceChargeDensityMetricUnits;

const METRIC: TbMeasureUnits<SurfaceChargeDensityMetricUnits> = {
  units: {
    'C/m²': {
      name: 'unit.coulomb-per-square-meter',
      tags: ['electric surface charge density', 'coulomb per square meter', 'C/m²'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<SurfaceChargeDensityUnits> = {
  METRIC,
};

export default measure;
