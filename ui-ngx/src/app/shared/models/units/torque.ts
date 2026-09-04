// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type TorqueUnits = TorqueMetricUnits | TorqueImperialUnits;

export type TorqueMetricUnits = 'Nm';
export type TorqueImperialUnits = 'lbf-ft' | 'in·lbf';

const METRIC: TbMeasureUnits<TorqueMetricUnits> = {
  ratio: 1 / 1.355818,
  units: {
    Nm: {
      name: 'unit.newton-meter',
      tags: ['rotational force', 'newton meter', 'Nm'],
      to_anchor: 1,
    },
  },
};

const IMPERIAL: TbMeasureUnits<TorqueImperialUnits> = {
  ratio: 1.355818,
  units: {
    'lbf-ft': {
      name: 'unit.foot-pounds',
      tags: ['rotational force'],
      to_anchor: 1,
    },
    'in·lbf': {
      name: 'unit.inch-pounds',
      tags: ['rotational force'],
      to_anchor: 1 / 12,
    },
  },
};

const measure: TbMeasure<TorqueUnits> = {
  METRIC,
  IMPERIAL,
};

export default measure;
