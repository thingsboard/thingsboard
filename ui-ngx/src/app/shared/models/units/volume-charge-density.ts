import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type VolumeChargeDensityMetricUnits = 'C/m³';

export type VolumeChargeDensityUnits = VolumeChargeDensityMetricUnits;

const METRIC: TbMeasureUnits<VolumeChargeDensityMetricUnits> = {
  units: {
    'C/m³': {
      name: 'unit.coulomb-per-cubic-meter',
      tags: ['electric charge density', 'coulomb per cubic meter', 'C/m³'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<VolumeChargeDensityUnits> = {
  METRIC,
};

export default measure;
