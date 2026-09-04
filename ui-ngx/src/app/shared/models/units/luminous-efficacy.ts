// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type LuminousEfficacyUnits = 'lm/W';

const METRIC: TbMeasureUnits<LuminousEfficacyUnits> = {
  units: {
    'lm/W': {
      name: 'unit.lumens-per-watt',
      tags: ['lighting efficiency'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<LuminousEfficacyUnits> = {
  METRIC,
};

export default measure;
