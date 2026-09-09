// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type AcidityUnits = 'pH';

const METRIC: TbMeasureUnits<AcidityUnits> = {
  units: {
    pH: {
      name: 'unit.ph-level',
      tags: [ 'alkalinity', 'neutral', 'acid', 'base', 'soil pH', 'water quality', 'water pH'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<AcidityUnits> = {
  METRIC,
};

export default measure;
