import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ElectricalConductivityMetricUnits = 'µS/cm' | 'mS/m' | 'S/m';
export type ElectricalConductivityUnits = ElectricalConductivityMetricUnits;

const METRIC: TbMeasureUnits<ElectricalConductivityMetricUnits> = {
  units: {
    'S/m': {
      name: 'unit.siemens-per-meter',
      tags: ['Electrical conductivity', 'water quality', 'soil quality', 'siemens per meter', 'S/m'],
      to_anchor: 1,
    },
    'µS/cm': {
      name: 'unit.microsiemens-per-centimeter',
      tags: ['Electrical conductivity', 'water quality', 'soil quality', 'microsiemens per centimeter', 'µS/cm'],
      to_anchor: 0.0001,
    },
    'mS/m': {
      name: 'unit.millisiemens-per-meter',
      tags: ['Electrical conductivity', 'water quality', 'soil quality', 'millisiemens per meter', 'mS/m'],
      to_anchor: 0.001,
    },
  },
};

const measure: TbMeasure<ElectricalConductivityUnits> = {
  METRIC,
};

export default measure;
