// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type RadioactiveDecayUnits = 'Bq/s' | 'Ci/s';

const METRIC: TbMeasureUnits<RadioactiveDecayUnits> = {
  units: {
    'Bq/s': {
      name: 'unit.becquerels-per-second',
      to_anchor: 1,
    },
    'Ci/s': {
      name: 'unit.curies-per-second',
      to_anchor: 3.7e10,
    },
  }
};

const measure: TbMeasure<RadioactiveDecayUnits> = {
  METRIC,
};

export default measure;
