import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type DensityMetricUnits = 'kg/m³' | 'g/cm³';
export type DensityImperialUnits = 'lb/ft³' | 'oz/in³' | 'ton/yd³';

export type DensityUnits = DensityMetricUnits | DensityImperialUnits;

const METRIC: TbMeasureUnits<DensityMetricUnits> = {
  ratio: 0.062428,
  units: {
    'kg/m³': {
      name: 'unit.kilogram-per-cubic-meter',
      tags: ['density', 'mass per unit volume', 'kg/m³'],
      to_anchor: 1, // Base unit: kg/m³
    },
    'g/cm³': {
      name: 'unit.gram-per-cubic-centimeter',
      tags: ['density', 'mass per unit volume', 'g/cm³'],
      to_anchor: 1000, // 1 g/cm³ = 10³ kg/m³
    },
  },
};

const IMPERIAL: TbMeasureUnits<DensityImperialUnits> = {
  ratio: 1 / 0.062428,
  units: {
    'lb/ft³': {
      name: 'unit.pound-per-cubic-foot',
      tags: ['density', 'mass per unit volume', 'lb/ft³'],
      to_anchor: 1,
    },
    'oz/in³': {
      name: 'unit.ounces-per-cubic-inch',
      tags: ['density', 'mass per unit volume', 'oz/in³'],
      to_anchor: 1728,
    },
    'ton/yd³': {
      name: 'unit.tons-per-cubic-yard',
      tags: ['density', 'mass per unit volume', 'ton/yd³'],
      to_anchor: 74.074,
    },
  },
};

const measure: TbMeasure<DensityUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
