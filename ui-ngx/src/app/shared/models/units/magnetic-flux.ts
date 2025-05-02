import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type MagneticFluxUnits = 'Wb' | 'µWb' | 'mWb' | 'Mx' | 'G·cm²' | 'kG·cm²';

const METRIC: TbMeasureUnits<MagneticFluxUnits> = {
  units: {
    'Wb': {
      name: 'unit.weber',
      tags: ['magnetic flux', 'weber', 'Wb'],
      to_anchor: 1,
    },
    'µWb': {
      name: 'unit.microweber',
      tags: ['magnetic flux', 'microweber', 'µWb'],
      to_anchor: 1e-6,
    },
    'mWb': {
      name: 'unit.milliweber',
      tags: ['magnetic flux', 'milliweber', 'mWb'],
      to_anchor: 1e-3,
    },
    'Mx': {
      name: 'unit.maxwell',
      tags: ['magnetic flux', 'magnetic field', 'maxwell', 'Mx'],
      to_anchor: 1e-8,
    },
    'G·cm²': {
      name: 'unit.gauss-square-centimeter',
      tags: ['magnetic flux', 'gauss-square centimeter', 'G·cm²'],
      to_anchor: 1e-8,
    },
    'kG·cm²': {
      name: 'unit.kilogauss-square-centimeter',
      tags: ['magnetic flux', 'kilogauss-square centimeter', 'kG·cm²'],
      to_anchor: 1e-5,
    },
  },
};

const measure: TbMeasure<MagneticFluxUnits> = {
  METRIC,
};

export default measure;
