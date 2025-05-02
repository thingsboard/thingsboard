import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ElectricalConductanceUnits = 'S' | 'mS' | 'μS' | 'kS' | 'MS' | 'GS';

const METRIC: TbMeasureUnits<ElectricalConductanceUnits> = {
  units: {
    'S': {
      name: 'unit.siemens',
      tags: ['electrical conductance', 'conductance', 'siemens', 'S'],
      to_anchor: 1,
    },
    'mS': {
      name: 'unit.millisiemens',
      tags: ['electrical conductance', 'conductance', 'millisiemens', 'mS'],
      to_anchor: 1e-3,
    },
    'μS': {
      name: 'unit.microsiemens',
      tags: ['electrical conductance', 'conductance', 'microsiemens', 'μS'],
      to_anchor: 1e-6,
    },
    'kS': {
      name: 'unit.kilosiemens',
      tags: ['electrical conductance', 'conductance', 'kilosiemens', 'kS'],
      to_anchor: 1e3,
    },
    'MS': {
      name: 'unit.megasiemens',
      tags: ['electrical conductance', 'conductance', 'megasiemens', 'MS'],
      to_anchor: 1e6,
    },
    'GS': {
      name: 'unit.gigasiemens',
      tags: ['electrical conductance', 'conductance', 'gigasiemens', 'GS'],
      to_anchor: 1e9,
    },
  },
};

const measure: TbMeasure<ElectricalConductanceUnits> = {
  METRIC,
};

export default measure;
