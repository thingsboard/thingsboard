// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type ElectricalConductivityUnits = 'µS/cm' | 'mS/m' | 'S/m';

const METRIC: TbMeasureUnits<ElectricalConductivityUnits> = {
  units: {
    'S/m': {
      name: 'unit.siemens-per-meter',
      tags: [ 'water quality', 'soil quality'],
      to_anchor: 1,
    },
    'µS/cm': {
      name: 'unit.microsiemens-per-centimeter',
      tags: ['water quality', 'soil quality'],
      to_anchor: 0.0001,
    },
    'mS/m': {
      name: 'unit.millisiemens-per-meter',
      tags: ['water quality', 'soil quality'],
      to_anchor: 0.001,
    },
  },
};

const measure: TbMeasure<ElectricalConductivityUnits> = {
  METRIC,
};

export default measure;
