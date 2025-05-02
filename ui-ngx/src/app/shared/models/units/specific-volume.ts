import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type SpecificVolumeMetricUnits = 'm³/kg';

export type SpecificVolumeUnits = SpecificVolumeMetricUnits;

const METRIC: TbMeasureUnits<SpecificVolumeMetricUnits> = {
  units: {
    'm³/kg': {
      name: 'unit.cubic-meter-per-kilogram',
      tags: ['specific volume', 'volume per unit mass', 'cubic meter per kilogram', 'm³/kg'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<SpecificVolumeUnits> = {
  METRIC,
};

export default measure;
