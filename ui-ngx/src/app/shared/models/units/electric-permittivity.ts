// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ElectricPermittivityUnits = 'F/m';

const METRIC: TbMeasureUnits<ElectricPermittivityUnits> = {
  units: {
    'F/m': {
      name: 'unit.farad-per-meter',
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<ElectricPermittivityUnits> = {
  METRIC,
};

export default measure;
