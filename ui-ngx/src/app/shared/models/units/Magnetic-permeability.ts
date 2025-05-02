import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type MagneticPermeabilityMetricUnits = 'H/m';
export type MagneticPermeabilityImperialUnits = 'G/Oe';

export type MagneticPermeabilityUnits =
  | MagneticPermeabilityMetricUnits
  | MagneticPermeabilityImperialUnits;

const METRIC: TbMeasureUnits<MagneticPermeabilityMetricUnits> = {
  transform: (Hm) => Hm * 795774.715,
  units: {
    'H/m': {
      name: 'unit.henry-per-meter',
      tags: ['magnetic permeability', 'henry per meter', 'H/m'],
      to_anchor: 1,
    },
  },
};

const IMPERIAL: TbMeasureUnits<MagneticPermeabilityImperialUnits> = {
  transform: (GOe) => GOe / 795774.715,
  units: {
    'G/Oe': {
      name: 'unit.gauss-per-oersted',
      tags: ['magnetic field', 'Gauss per Oersted', 'G/Oe'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<MagneticPermeabilityUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
