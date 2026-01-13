///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

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
