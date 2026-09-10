// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { ItemType } from '@shared/models/iot-hub/iot-hub-item.models';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function isUUID(s: string | null | undefined): s is string {
  return !!s && UUID_RE.test(s);
}

export function typeSegment(t: ItemType): string | undefined {
  switch (t) {
    case ItemType.WIDGET: return 'widgets';
    case ItemType.DASHBOARD: return 'dashboards';
    case ItemType.SOLUTION_TEMPLATE: return 'solution-templates';
    case ItemType.CALCULATED_FIELD: return 'calculated-fields';
    case ItemType.ALARM_RULE: return 'alarm-rules';
    case ItemType.RULE_CHAIN: return 'rule-chains';
    case ItemType.DEVICE: return 'devices';
    default: return undefined;
  }
}

export function isPublished(v: MpItemVersionView): boolean {
  return !!v.publishedTime && v.publishedTime > 0;
}

export interface DeepLinkOpenItem {
  version: MpItemVersionView;
  preview: boolean;
}
