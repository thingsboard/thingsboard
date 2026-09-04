// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ElectricalConductanceUnits = 'S' | 'mS' | 'μS' | 'kS' | 'MS' | 'GS';

const METRIC: TbMeasureUnits<ElectricalConductanceUnits> = {
  units: {
    S: {
      name: 'unit.siemens',
      to_anchor: 1,
    },
    mS: {
      name: 'unit.millisiemens',
      to_anchor: 1e-3,
    },
    μS: {
      name: 'unit.microsiemens',
      to_anchor: 1e-6,
    },
    kS: {
      name: 'unit.kilosiemens',
      to_anchor: 1e3,
    },
    MS: {
      name: 'unit.megasiemens',
      to_anchor: 1e6,
    },
    GS: {
      name: 'unit.gigasiemens',
      to_anchor: 1e9,
    },
  },
};

const measure: TbMeasure<ElectricalConductanceUnits> = {
  METRIC,
};

export default measure;
