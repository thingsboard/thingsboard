import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type RadioactivityMetricUnits = 'Bq' | 'Ci' | 'Rd' | 'dps' | 'cps';
export type RadioactivityUnits = RadioactivityMetricUnits;

const METRIC: TbMeasureUnits<RadioactivityMetricUnits> = {
  units: {
    'Bq': {
      name: 'unit.becquerel',
      tags: ['radioactivity', 'decay rate', 'becquerel', 'Bq'],
      to_anchor: 1,
    },
    'Ci': {
      name: 'unit.curie',
      tags: ['radioactivity', 'radiation', 'curie', 'Ci'],
      to_anchor: 3.7e10,
    },
    'Rd': {
      name: 'unit.rutherford',
      tags: ['radioactive decay', 'radioactivity', 'rutherford', 'Rd'],
      to_anchor: 1e6,
    },
    'dps': {
      name: 'unit.dps',
      tags: ['radioactive decay', 'radioactivity', 'disintegrations per second', 'dps'],
      to_anchor: 1,
    },
    cps: {
      name: 'unit.cps',
      tags: ['radiation detection', 'counts per second', 'cps'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<RadioactivityUnits> = {
  METRIC,
};

export default measure;
