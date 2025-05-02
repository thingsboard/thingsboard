import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type RadiationDoseMetricUnits = 'Gy' | 'Sv' | 'Rad' | 'Rem' | 'R' | 'C/kg' | 'Gy/s';
export type RadiationDoseUnits = RadiationDoseMetricUnits;

const METRIC: TbMeasureUnits<RadiationDoseMetricUnits> = {
  units: {
    'Sv': {
      name: 'unit.sievert',
      tags: ['radiation dose', 'sievert', 'radiation dose equivalent', 'Sv'],
      to_anchor: 1,
    },
    'Gy': {
      name: 'unit.gray',
      tags: ['radiation dose', 'absorbed dose', 'gray', 'Gy'],
      to_anchor: 1,
    },
    'Rad': {
      name: 'unit.rad',
      tags: ['radiation dose', 'rad'],
      to_anchor: 0.01,
    },
    'Rem': {
      name: 'unit.rem',
      tags: ['radiation dose equivalent', 'rem'],
      to_anchor: 0.01,
    },
    'R': {
      name: 'unit.roentgen',
      tags: ['radiation exposure', 'roentgen', 'R'],
      to_anchor: 0.0093,
    },
    'C/kg': {
      name: 'unit.coulombs-per-kilogram',
      tags: ['radiation exposure', 'dose', 'coulombs per kilogram', 'electric charge-to-mass ratio', 'C/kg'],
      to_anchor: 34,
    },
    'Gy/s': {
      name: 'unit.gy-per-second',
      tags: ['absorbed dose rate', 'radiation dose rate', 'gray per second', 'Gy/s'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<RadiationDoseUnits> = {
  METRIC,
};

export default measure;
