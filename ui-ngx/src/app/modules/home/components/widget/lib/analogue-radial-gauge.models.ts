// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { AnalogueGaugeSettings } from '@home/components/widget/lib/analogue-gauge.models';

export interface AnalogueRadialGaugeSettings extends AnalogueGaugeSettings {
  startAngle: number;
  ticksAngle: number;
  needleCircleSize: number;
}
