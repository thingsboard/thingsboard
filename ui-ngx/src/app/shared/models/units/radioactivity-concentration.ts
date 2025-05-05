import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type RadioactivityConcentrationMetricUnits = 'Bq/m³' | 'Ci/L';
export type RadioactivityConcentrationUnits = RadioactivityConcentrationMetricUnits;

const METRIC: TbMeasureUnits<RadioactivityConcentrationMetricUnits> = {
  units: {
    'Bq/m³': {
      name: 'unit.becquerels-per-cubic-meter',
      tags: ['radioactivity', 'radiation', 'becquerels per cubic meter', 'Bq/m³'],
      to_anchor: 1,
    },
    'Ci/L': {
      name: 'unit.curies-per-liter',
      tags: ['radioactivity', 'radiation', 'curies per liter', 'Ci/L'],
      to_anchor: 3.7e10 * 1000,
    },
  },
};

const measure: TbMeasure<RadioactivityConcentrationUnits> = {
  METRIC,
};

export default measure;
