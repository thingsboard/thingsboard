// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type CatalyticConcentrationUnits = 'kat/m³';

const METRIC: TbMeasureUnits<CatalyticConcentrationUnits> = {
  units: {
    'kat/m³': {
      name: 'unit.katal-per-cubic-metre',
      tags: ['enzyme concentration'],
      to_anchor: 1,
    }
  },
};

const measure: TbMeasure<CatalyticConcentrationUnits> = {
  METRIC,
};

export default measure;
