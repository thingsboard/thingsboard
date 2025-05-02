import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ElectricPermittivityMetricUnits = 'F/m';

export type ElectricPermittivityUnits = ElectricPermittivityMetricUnits;

const METRIC: TbMeasureUnits<ElectricPermittivityMetricUnits> = {
  units: {
    'F/m': {
      name: 'unit.farad-per-meter',
      tags: ['electric permittivity', 'farad per meter', 'F/m'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<ElectricPermittivityUnits> = {
  METRIC,
};

export default measure;
