import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type AirQualityIndexUnits = 'kat';

const METRIC: TbMeasureUnits<AirQualityIndexUnits> = {
  units: {
    kat: {
      name: 'unit.katal',
      tags: ['catalytic activity', 'enzyme activity', 'kat'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<AirQualityIndexUnits> = {
  METRIC,
};

export default measure;
