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
