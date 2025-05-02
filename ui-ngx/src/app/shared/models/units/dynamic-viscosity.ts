import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type DynamicViscosityMetricUnits = 'Pa·s' | 'cP' | 'P' | 'N·s/m²' | 'dyn·s/cm²' | 'kg/(m·s)';
export type DynamicViscosityImperialUnits = 'lb/(ft·h)';

export type DynamicViscosityUnits = DynamicViscosityMetricUnits | DynamicViscosityImperialUnits;

const METRIC: TbMeasureUnits<DynamicViscosityMetricUnits> = {
  ratio: 2419.0883293091,
  units: {
    'Pa·s': {
      name: 'unit.pascal-second',
      tags: ['dynamic viscosity', 'viscosity', 'fluid mechanics', 'Pa·s'],
      to_anchor: 1,
    },
    'cP': {
      name: 'unit.centipoise',
      tags: ['viscosity', 'dynamic viscosity', 'fluid viscosity', 'centipoise', 'cP'],
      to_anchor: 0.001,
    },
    'P': {
      name: 'unit.poise',
      tags: ['viscosity', 'dynamic viscosity', 'fluid viscosity', 'poise', 'P'],
      to_anchor: 0.1,
    },
    'N·s/m²': {
      name: 'unit.newton-second-per-square-meter',
      tags: ['newton second per square meter', 'N·s/m²'],
      to_anchor: 1,
    },
    'dyn·s/cm²': {
      name: 'unit.dyne-second-per-square-centimeter',
      tags: ['dyne second per square centimeter', 'dyn·s/cm²'],
      to_anchor: 0.1,
    },
    'kg/(m·s)': {
      name: 'unit.kilogram-per-meter-second',
      tags: ['kilogram per meter-second', 'kg/(m·s)'],
      to_anchor: 1,
    },
  },
};

const IMPERIAL: TbMeasureUnits<DynamicViscosityImperialUnits> = {
  ratio: 0.00041337887,
  units: {
    'lb/(ft·h)': {
      name: 'unit.pound-per-foot-hour',
      tags: ['pound per foot-hour', 'lb/(ft·h)'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<DynamicViscosityUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
