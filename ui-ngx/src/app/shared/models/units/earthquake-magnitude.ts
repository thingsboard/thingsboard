// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type EarthquakeMagnitudeUnits = 'richter';

const METRIC: TbMeasureUnits<EarthquakeMagnitudeUnits> = {
  units: {
    richter: {
      name: 'unit.richter-scale',
      tags: ['seismic activity'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<EarthquakeMagnitudeUnits> = {
  METRIC,
};

export default measure;
