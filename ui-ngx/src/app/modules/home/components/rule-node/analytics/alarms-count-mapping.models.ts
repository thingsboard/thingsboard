// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AlarmSeverity, AlarmStatus } from '@shared/public-api';

export interface AlarmsCountMapping {
  target: string;
  typesList?: string[];
  severityList?: AlarmSeverity[];
  statusList?: AlarmStatus[];
  latestInterval: number;
}
