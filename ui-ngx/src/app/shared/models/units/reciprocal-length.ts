// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ReciprocalLengthUnits = 'm⁻¹';

const METRIC: TbMeasureUnits<ReciprocalLengthUnits> = {
  units: {
    'm⁻¹': {
      name: 'unit.reciprocal-metre',
      tags: ['wavenumber', 'wave density', 'wave frequency'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<ReciprocalLengthUnits> = {
  METRIC,
};

export default measure;
