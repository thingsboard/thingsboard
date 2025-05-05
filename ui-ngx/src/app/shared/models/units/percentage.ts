import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type PercentageMetricUnits = '%';
export type PercentageUnits = PercentageMetricUnits;

const METRIC: TbMeasureUnits<PercentageMetricUnits> = {
  units: {
    '%': {
      name: 'unit.percent',
      tags: ['power source', 'state of charge (SoC)', 'battery', 'battery level', 'level', 'humidity', 'moisture', 'percentage', 'relative humidity', 'water content', 'soil moisture', 'irrigation', 'water in soil', 'soil water content', 'VWC', 'Volumetric Water Content', 'Total Harmonic Distortion', 'THD', 'power quality', 'UV Transmittance', '%', 'capacity'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<PercentageUnits> = {
  METRIC,
};

export default measure;
