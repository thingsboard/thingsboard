import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type KinematicViscosityMetricUnits = 'm²/s' | 'cm²/s' | 'St' | 'cSt';
export type KinematicViscosityImperialUnits = 'ft²/s' | 'in²/s';

export type KinematicViscosityUnits = KinematicViscosityMetricUnits | KinematicViscosityImperialUnits;

const METRIC: TbMeasureUnits<KinematicViscosityMetricUnits> = {
  ratio: 10.7639104167097,
  units: {
    'm²/s': {
      name: 'unit.square-meter-per-second',
      tags: ['kinematic viscosity', 'm²/s'],
      to_anchor: 1,
    },
    'cm²/s': {
      name: 'unit.square-centimeter-per-second',
      tags: ['kinematic viscosity', 'cm²/s'],
      to_anchor: 1e-4,
    },
    'St': {
      name: 'unit.stoke',
      tags: ['kinematic viscosity', 'stokes', 'St'],
      to_anchor: 1e-4, // St to m²/s
    },
    'cSt': {
      name: 'unit.centistokes',
      tags: ['kinematic viscosity', 'centistokes', 'cSt'],
      to_anchor: 1e-6, // cSt to m²/s
    },
  },
};

const IMPERIAL: TbMeasureUnits<KinematicViscosityImperialUnits> = {
  ratio: 0.09290304,
  units: {
    'ft²/s': {
      name: 'unit.square-foot-per-second',
      tags: ['kinematic viscosity', 'ft²/s'],
      to_anchor: 0.09290304,
    },
    'in²/s': {
      name: 'unit.square-inch-per-second',
      tags: ['kinematic viscosity', 'in²/s'],
      to_anchor: 0.00064516,
    },
  },
};

const measure: TbMeasure<KinematicViscosityUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
