import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ThermalConductivityMetricUnits = 'W/(m·K)';

export type ThermalConductivityUnits = ThermalConductivityMetricUnits;

const METRIC: TbMeasureUnits<ThermalConductivityMetricUnits> = {
  units: {
    'W/(m·K)': {
      name: 'unit.watt-per-meter-kelvin',
      tags: ['thermal conductivity', 'watt per meter-kelvin', 'W/(m·K)'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<ThermalConductivityUnits> = {
  METRIC,
};

export default measure;
