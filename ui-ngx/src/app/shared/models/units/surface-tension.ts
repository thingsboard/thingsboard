import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type SurfaceTensionMetricUnits = 'N/m';

export type SurfaceTensionhUnits = SurfaceTensionMetricUnits;

const METRIC: TbMeasureUnits<SurfaceTensionMetricUnits> = {
  units: {
    'N/m': {
      name: 'unit.newton-per-meter',
      tags: ['linear density', 'force per unit length', 'newton per meter', 'N/m'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<SurfaceTensionhUnits> = {
  METRIC,
};

export default measure;
