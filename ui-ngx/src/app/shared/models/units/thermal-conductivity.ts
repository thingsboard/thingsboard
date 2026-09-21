// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ThermalConductivityUnits = 'W/(m·K)';

const METRIC: TbMeasureUnits<ThermalConductivityUnits> = {
  units: {
    'W/(m·K)': {
      name: 'unit.watt-per-meter-kelvin',
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<ThermalConductivityUnits> = {
  METRIC,
};

export default measure;
