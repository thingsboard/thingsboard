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

import { EntityType } from '@shared/models/entity-type.models';
import { IotHubInstalledItemDescriptor } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { getEntityDetailsPageURL } from '@core/utils';

export const ITEM_TYPE_TO_ENTITY_TYPE: Record<string, EntityType> = {
  'WIDGET': EntityType.WIDGET_TYPE,
  'DASHBOARD': EntityType.DASHBOARD,
  'CALCULATED_FIELD': EntityType.CALCULATED_FIELD,
  'RULE_CHAIN': EntityType.RULE_CHAIN,
  'DEVICE': EntityType.DEVICE_PROFILE
};

export function resolveEntityDetailsUrl(descriptor: IotHubInstalledItemDescriptor, itemType: string): string | null {
  if (!descriptor) {
    return null;
  }
  const entityType = ITEM_TYPE_TO_ENTITY_TYPE[itemType];
  if (!entityType) {
    return null;
  }
  let entityId: string | null = null;
  switch (descriptor.type) {
    case 'WIDGET': entityId = descriptor.widgetTypeId?.id; break;
    case 'DASHBOARD': entityId = descriptor.dashboardId?.id; break;
    case 'CALCULATED_FIELD': entityId = descriptor.calculatedFieldId?.id; break;
    case 'RULE_CHAIN': entityId = descriptor.ruleChainId?.id; break;
  }
  if (!entityId) {
    return null;
  }
  return getEntityDetailsPageURL(entityId, entityType) || null;
}
