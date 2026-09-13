// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ElectricChargeDensityUnits = 'C/m³';

const METRIC: TbMeasureUnits<ElectricChargeDensityUnits> = {
  units: {
    'C/m³': {
      name: 'unit.coulomb-per-cubic-meter',
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<ElectricChargeDensityUnits> = {
  METRIC,
};

export default measure;
