import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type CapacitanceUnits = 'F' | 'mF' | 'μF' | 'nF' | 'pF' | 'kF' | 'MF' | 'GF' | 'TF';

const METRIC: TbMeasureUnits<CapacitanceUnits> = {
  units: {
    'F': {
      name: 'unit.farad',
      tags: ['electric capacitance', 'capacitance', 'farad', 'F'],
      to_anchor: 1,
    },
    'mF': {
      name: 'unit.millifarad',
      tags: ['electric capacitance', 'capacitance', 'millifarad', 'mF'],
      to_anchor: 1e-3,
    },
    'μF': {
      name: 'unit.microfarad',
      tags: ['electric capacitance', 'capacitance', 'microfarad', 'μF'],
      to_anchor: 1e-6,
    },
    'nF': {
      name: 'unit.nanofarad',
      tags: ['electric capacitance', 'capacitance', 'nanofarad', 'nF'],
      to_anchor: 1e-9,
    },
    'pF': {
      name: 'unit.picofarad',
      tags: ['electric capacitance', 'capacitance', 'picofarad', 'pF'],
      to_anchor: 1e-12,
    },
    'kF': {
      name: 'unit.kilofarad',
      tags: ['electric capacitance', 'capacitance', 'kilofarad', 'kF'],
      to_anchor: 1e3,
    },
    'MF': {
      name: 'unit.megafarad',
      tags: ['electric capacitance', 'capacitance', 'megafarad', 'MF'],
      to_anchor: 1e6,
    },
    'GF': {
      name: 'unit.gigafarad',
      tags: ['electric capacitance', 'capacitance', 'gigafarad', 'GF'],
      to_anchor: 1e9,
    },
    'TF': {
      name: 'unit.terafarad',
      tags: ['electric capacitance', 'capacitance', 'terafarad', 'TF'],
      to_anchor: 1e12,
    },
  },
};

const measure: TbMeasure<CapacitanceUnits> = {
  METRIC,
};

export default measure;
