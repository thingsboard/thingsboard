// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type RadioactivityConcentrationUnits = 'Bq/m³' | 'Ci/L';

const METRIC: TbMeasureUnits<RadioactivityConcentrationUnits> = {
  units: {
    'Bq/m³': {
      name: 'unit.becquerels-per-cubic-meter',
      tags: ['radiation'],
      to_anchor: 1,
    },
    'Ci/L': {
      name: 'unit.curies-per-liter',
      tags: ['radiation'],
      to_anchor: 3.7e10 * 1000,
    },
  },
};

const measure: TbMeasure<RadioactivityConcentrationUnits> = {
  METRIC,
};

export default measure;
