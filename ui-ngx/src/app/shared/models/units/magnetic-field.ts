import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type MagneticFieldMetricUnits = 'T' | 'mT' | 'μT' | 'nT' | 'kT' | 'MT' | 'G' | 'kG' | 'γ' | 'A/m' | 'Oe';

export type MagneticFieldUnits = MagneticFieldMetricUnits;

const METRIC: TbMeasureUnits<MagneticFieldMetricUnits> = {
  units: {
    'T': {
      name: 'unit.tesla',
      tags: ['magnetic field', 'magnetic field strength', 'tesla', 'T', 'magnetic flux density'],
      to_anchor: 1,
    },
    'mT': {
      name: 'unit.millitesla',
      tags: ['magnetic field', 'magnetic field strength', 'millitesla', 'mT'],
      to_anchor: 0.001,
    },
    'μT': {
      name: 'unit.microtesla',
      tags: ['magnetic field', 'magnetic field strength', 'microtesla', 'μT'],
      to_anchor: 0.000001,
    },
    'nT': {
      name: 'unit.nanotesla',
      tags: ['magnetic field', 'magnetic field strength', 'nanotesla', 'nT'],
      to_anchor: 0.000000001,
    },
    'kT': {
      name: 'unit.kilotesla',
      tags: ['magnetic field', 'magnetic field strength', 'kilotesla', 'kT'],
      to_anchor: 1000,
    },
    'MT': {
      name: 'unit.megatesla',
      tags: ['magnetic field', 'magnetic field strength', 'megatesla', 'MT'],
      to_anchor: 1000000,
    },
    'G': {
      name: 'unit.gauss',
      tags: ['magnetic field', 'magnetic field strength', 'gauss', 'G', 'magnetic flux density'],
      to_anchor: 0.0001,
    },
    'kG': {
      name: 'unit.kilogauss',
      tags: ['magnetic field', 'magnetic field strength', 'kilogauss', 'kG', 'magnetic flux density'],
      to_anchor: 0.1,
    },
    'γ': {
      name: 'unit.gamma',
      tags: ['magnetic flux density', 'gamma', 'γ'],
      to_anchor: 0.000000001,
    },
    'A/m': {
      name: 'unit.ampere-per-meter',
      tags: ['magnetic field strength', 'magnetic field intensity', 'ampere per meter', 'A/m'],
      to_anchor: 0.00000125663706143591,
    },
    'Oe': {
      name: 'unit.oersted',
      tags: ['magnetic field', 'oersted', 'Oe'],
      to_anchor: 0.0001,
    },
  },
};

const measure: TbMeasure<MagneticFieldUnits> = {
  METRIC,
};

export default measure;
