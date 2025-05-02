import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ConcentrationVolumeMetricUnits =
  'mol/m³'
  | 'µg/m³'
  | 'mg/m³'
  | 'g/m³'
  | 'mg/L'
  | 'mg/mL'
  | 'kat/m³'
  | '°Bx';
export type ConcentrationVolumeUnits = ConcentrationVolumeMetricUnits;

const METRIC: TbMeasureUnits<ConcentrationVolumeMetricUnits> = {
  units: {
    'mol/m³': {
      name: 'unit.mole-per-cubic-meter',
      tags: ['concentration', 'amount of substance', 'mole per cubic meter', 'mol/m³'],
      to_anchor: 1,
    },
    'µg/m³': {
      name: 'unit.micrograms-per-cubic-meter',
      tags: ['coarse particulate matter', 'pm10', 'fine particulate matter', 'pm2.5', 'aqi', 'air quality', 'total volatile organic compounds', 'tvoc', 'micrograms per cubic meter', 'µg/m³'],
      to_anchor: 1e-9,
    },
    'mg/m³': {
      name: 'unit.milligram-per-cubic-meter',
      tags: ['concentration', 'mass per volume', 'mg/m³'],
      to_anchor: 1e-6,
    },
    'g/m³': {
      name: 'unit.gram-per-cubic-meter',
      tags: ['humidity', 'moisture', 'absolute humidity', 'g/m³'],
      to_anchor: 1 / 1000,
    },
    'mg/L': {
      name: 'unit.mg-per-liter',
      tags: ['dissolved oxygen', 'water quality', 'mg/L'],
      to_anchor: 1e-6,
    },
    'mg/mL': {
      name: 'unit.milligram-per-milliliter',
      tags: ['concentration', 'mass per volume', 'mg/mL'],
      to_anchor: 1 / 1000,
    },
    'kat/m³': {
      name: 'unit.katal-per-cubic-metre',
      tags: ['catalytic activity concentration', 'enzyme concentration', 'kat/m³'],
      to_anchor: 1,
    },
    '°Bx': {
      name: 'unit.degrees-brix',
      tags: ['sugar content', 'fruit ripeness', 'Bx'],
      to_anchor: 10.04 * 1e-3,
    },
  },
};

const measure: TbMeasure<ConcentrationVolumeUnits> = {
  METRIC,
};

export default measure;
