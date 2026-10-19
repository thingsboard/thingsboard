// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type MolarHeatCapacityUnits = 'J/(mol·K)';

const METRIC: TbMeasureUnits<MolarHeatCapacityUnits> = {
  units: {
    'J/(mol·K)': {
      name: 'unit.joule-per-mole-kelvin',
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<MolarHeatCapacityUnits> = {
  METRIC,
};

export default measure;
