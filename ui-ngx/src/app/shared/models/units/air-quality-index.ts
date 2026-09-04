// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type AirQualityIndexUnits = 'aqi';

const METRIC: TbMeasureUnits<AirQualityIndexUnits> = {
  units: {
    aqi: {
      name: 'unit.aqi',
      tags: ['pollutant concentration'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<AirQualityIndexUnits> = {
  METRIC,
};

export default measure;
