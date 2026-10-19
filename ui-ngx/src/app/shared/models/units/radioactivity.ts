// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type RadioactivityMetricUnits = 'Bq' | 'Ci' | 'Rd' | 'dps';
export type RadioactivityUnits = RadioactivityMetricUnits;

const METRIC: TbMeasureUnits<RadioactivityMetricUnits> = {
  units: {
    Bq: {
      name: 'unit.becquerel',
      tags: ['radioactivity', 'decay rate'],
      to_anchor: 1,
    },
    Ci: {
      name: 'unit.curie',
      tags: ['radiation'],
      to_anchor: 3.7e10,
    },
    Rd: {
      name: 'unit.rutherford',
      tags: ['radioactive decay'],
      to_anchor: 3.7e4,
    },
    dps: {
      name: 'unit.dps',
      tags: ['radioactive decay'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<RadioactivityUnits> = {
  METRIC,
};

export default measure;
