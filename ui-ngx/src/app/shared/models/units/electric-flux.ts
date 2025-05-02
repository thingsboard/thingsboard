import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ElectricFluxMetricUnits = 'V·m' | 'kV·m' | 'MV·m' | 'µV·m' | 'mV·m' | 'nV·m';

export type ElectricFluxUnits = ElectricFluxMetricUnits;

const METRIC: TbMeasureUnits<ElectricFluxMetricUnits> = {
  units: {
    'V·m': {
      name: 'unit.volt-meter',
      tags: ['electric flux', 'volt-meter', 'V·m'],
      to_anchor: 1,
    },
    'kV·m': {
      name: 'unit.kilovolt-meter',
      tags: ['electric flux', 'kilovolt-meter', 'kV·m'],
      to_anchor: 1000,
    },
    'MV·m': {
      name: 'unit.megavolt-meter',
      tags: ['electric flux', 'megavolt-meter', 'MV·m'],
      to_anchor: 1000000,
    },
    'µV·m': {
      name: 'unit.microvolt-meter',
      tags: ['electric flux', 'microvolt-meter', 'µV·m'],
      to_anchor: 0.000001,
    },
    'mV·m': {
      name: 'unit.millivolt-meter',
      tags: ['electric flux', 'millivolt-meter', 'mV·m'],
      to_anchor: 0.001,
    },
    'nV·m': {
      name: 'unit.nanovolt-meter',
      tags: ['electric flux', 'nanovolt-meter', 'nV·m'],
      to_anchor: 0.000000001,
    },
  },
};

const measure: TbMeasure<ElectricFluxUnits> = {
  METRIC,
};

export default measure;
