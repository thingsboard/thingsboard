// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type AngularAccelerationMetricUnits = 'rad/s²';
export type AngularAccelerationImperialUnits = 'rpm/s';

export type AngularAccelerationUnits = AngularAccelerationMetricUnits | AngularAccelerationImperialUnits;

const METRIC: TbMeasureUnits<AngularAccelerationMetricUnits> = {
  ratio: 30 / Math.PI,
  units: {
    'rad/s²': {
      name: 'unit.radian-per-second-squared',
      tags: ['rotation rate of change'],
      to_anchor: 1,
    }
  }
};

const IMPERIAL: TbMeasureUnits<AngularAccelerationImperialUnits> = {
  ratio: Math.PI / 30,
  units: {
    'rpm/s': {
      name: 'unit.revolutions-per-minute-per-second',
      tags: ['rotation rate of change'],
      to_anchor: 1
    }
  }
}

const measure: TbMeasure<AngularAccelerationUnits> = {
  METRIC,
  IMPERIAL
};

export default measure;
