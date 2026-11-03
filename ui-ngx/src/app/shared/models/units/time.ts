// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbMeasure, TbMeasureUnits } from '@shared/models/unit.models';

export type TimeUnits = TimeMetricUnits;

export type TimeMetricUnits =
  | 'ns'
  | 'μs'
  | 'ms'
  | 's'
  | 'min'
  | 'h'
  | 'd'
  | 'wk'
  | 'mo'
  | 'yr';

const daysInYear = 365.25;

const METRIC: TbMeasureUnits<TimeMetricUnits> = {
  units: {
    ns: {
      name: 'unit.nanosecond',
      tags: ['duration', 'interval'],
      to_anchor: 1 / 1000000000
    },
    μs: {
      name: 'unit.microsecond',
      tags: ['duration', 'interval'],
      to_anchor: 1 / 1000000
    },
    ms: {
      name: 'unit.millisecond',
      tags: ['duration', 'interval'],
      to_anchor: 1 / 1000
    },
    s: {
      name: 'unit.second',
      tags: ['duration', 'interval'],
      to_anchor: 1,
    },
    min: {
      name: 'unit.minute',
      tags: ['duration', 'interval'],
      to_anchor: 60,
    },
    h: {
      name: 'unit.hour',
      tags: ['duration', 'interval'],
      to_anchor: 60 * 60,
    },
    d: {
      name: 'unit.day',
      tags: ['duration', 'interval'],
      to_anchor: 60 * 60 * 24,
    },
    wk: {
      name: 'unit.week',
      tags: ['duration', 'interval'],
      to_anchor: 60 * 60 * 24 * 7,
    },
    mo: {
      name: 'unit.month',
      tags: ['duration', 'interval'],
      to_anchor: (60 * 60 * 24 * daysInYear) / 12,
    },
    yr: {
      name: 'unit.year',
      tags: ['duration', 'interval'],
      to_anchor: 60 * 60 * 24 * daysInYear,
    },
  }
};

const measure: TbMeasure<TimeUnits> = {
  METRIC,
};

export default measure;
