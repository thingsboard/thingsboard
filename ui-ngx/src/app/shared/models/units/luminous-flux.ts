// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type LuminousFluxUnits = 'lm';

const METRIC: TbMeasureUnits<LuminousFluxUnits> = {
  units: {
    lm: {
      name: 'unit.lumen',
      tags: ['total light output'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<LuminousFluxUnits> = {
  METRIC,
};

export default measure;
