import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ElectricFieldStrengthUnits = 'V/m' | 'mV/m' | 'kV/m';

const METRIC: TbMeasureUnits<ElectricFieldStrengthUnits> = {
  units: {
    'V/m': {
      name: 'unit.volts-per-meter',
      tags: ['electric field strength', 'volts per meter', 'V/m'],
      to_anchor: 1,
    },
    'mV/m': {
      name: 'unit.millivolts-per-meter',
      tags: ['electric field strength', 'millivolts per meter', 'mV/m'],
      to_anchor: 1e-3,
    },
    'kV/m': {
      name: 'unit.kilovolts-per-meter',
      tags: ['electric field strength', 'kilovolts per meter', 'kV/m'],
      to_anchor: 1e3,
    },
  },
};

const measure: TbMeasure<ElectricFieldStrengthUnits> = {
  METRIC,
};

export default measure;
