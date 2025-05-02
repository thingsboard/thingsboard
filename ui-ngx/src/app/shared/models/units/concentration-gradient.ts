import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ConcentrationMetricUnits = 'mol/m³' | 'mg/mL' | 'mg/m³' | 'µg/m³' | 'particles/mL';
export type ConcentrationUnits = ConcentrationMetricUnits;

const METRIC: TbMeasureUnits<ConcentrationMetricUnits> = {
  units: {
    'mol/m³': {
      name: 'unit.mole-per-cubic-meter',
      tags: ['concentration', 'amount of substance per unit volume', 'mole per cubic meter', 'mol/m³'],
      to_anchor: 1,
    },
    'mg/mL': {
      name: 'unit.milligram-per-milliliter',
      tags: ['concentration', 'mass per unit volume', 'milligram per milliliter', 'mg/mL'],
      to_anchor: 1,
    },
    'mg/m³': {
      name: 'unit.milligram-per-cubic-meter',
      tags: ['concentration', 'mass per unit volume', 'milligram per cubic meter', 'mg/m³'],
      to_anchor: 1,
    },
    'µg/m³': {
      name: 'unit.micrograms-per-cubic-meter',
      tags: ['concentration', 'air quality', 'particulate matter', 'PM2.5', 'PM10', 'micrograms per cubic meter', 'µg/m³'],
      to_anchor: 1,
    },
    'particles/mL': {
      name: 'unit.particle-density',
      tags: ['concentration', 'particle density', 'particles per milliliter', 'particles/mL'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<ConcentrationUnits> = {
  METRIC,
};

export default measure;
