import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type AirQualityIndexUnits = 'aqi';

const METRIC: TbMeasureUnits<AirQualityIndexUnits> = {
  units: {
    aqi: {
      name: 'unit.aqi',
      tags: ['AQI', 'air quality index'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<AirQualityIndexUnits> = {
  METRIC,
};

export default measure;
