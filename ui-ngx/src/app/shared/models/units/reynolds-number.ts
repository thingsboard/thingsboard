// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ReynoldsNumberUnits = 'Re';

const METRIC: TbMeasureUnits<ReynoldsNumberUnits> = {
  units: {
    Re: {
      name: 'unit.reynolds',
      tags: ['fluid flow regime', 'fluid mechanics'],
      to_anchor: 1,
    },
  },
};

const measure: TbMeasure<ReynoldsNumberUnits> = {
  METRIC,
};

export default measure;
