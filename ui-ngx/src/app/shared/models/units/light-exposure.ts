// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type LightExposureUnits = 'lx·s';

const METRIC: TbMeasureUnits<LightExposureUnits> = {
  units: {
    'lx·s': {
      name: 'unit.lux-second',
      tags: ['illuminance over time'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<LightExposureUnits> = {
  METRIC,
};

export default measure;
