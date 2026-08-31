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

import { FilterParamInfo, ItemType } from '@shared/models/iot-hub/iot-hub-item.models';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';

export const IOT_HUB_FILTER_GROUPING_THRESHOLD = 11;
export const IOT_HUB_FILTER_POPULAR_LIMIT = 10;

/**
 * Primary action offered for an IoT Hub item:
 *  - `open`    — the item is built-in, the tenant already has it, so we navigate to the local copy;
 *  - `connect` — device packages are connected rather than installed;
 *  - `install` — everything else.
 * Dependent copy (button labels, dialog CTA, tooltips) is keyed by this mode, never by the verb itself.
 */
export type IotHubItemActionMode = 'open' | 'connect' | 'install';

/**
 * The `builtIn` flag is server-owned and not live on every Hub deployment yet, so an absent,
 * null or non-boolean value must read as "not built-in" instead of leaking into the UI.
 */
export const isBuiltInItem = (item?: MpItemVersionView | null): boolean => item?.builtIn === true;

export const iotHubItemActionMode = (item?: MpItemVersionView | null): IotHubItemActionMode => {
  if (isBuiltInItem(item)) {
    return 'open';
  }
  return item?.type === ItemType.DEVICE ? 'connect' : 'install';
};

/** Surface a CTA is rendered on: a catalogue card, the item detail footer, or an add-item picker. */
export type IotHubItemActionContext = 'card' | 'detail' | 'add';

const iotHubActionLabels: Record<IotHubItemActionContext, Record<IotHubItemActionMode, string>> = {
  card: {
    open: 'iot-hub.open',
    connect: 'iot-hub.connect',
    install: 'iot-hub.install'
  },
  // The detail footer has room to spell the device wording out.
  detail: {
    open: 'iot-hub.open',
    connect: 'iot-hub.connect-device',
    install: 'iot-hub.install'
  },
  // Picking an item to add to an entity or a dashboard is "Add", whether or not it is a device.
  add: {
    open: 'iot-hub.open',
    connect: 'action.add',
    install: 'action.add'
  }
};

/** Translation key of the primary CTA for an item on the given surface. */
export const iotHubItemActionLabel = (item: MpItemVersionView | null,
                                      context: IotHubItemActionContext): string =>
  iotHubActionLabels[context][iotHubItemActionMode(item)];

export interface IotHubFilterGroup {
  label: string;
  items: FilterParamInfo[];
}

export function filterIotHubItemsBySearch(items: FilterParamInfo[], search: string): FilterParamInfo[] {
  const normalized = (search || '').toLowerCase();
  if (!normalized) {
    return items;
  }
  return items.filter(item => item.key.toLowerCase().includes(normalized));
}

export function groupIotHubFilterItems(items: FilterParamInfo[], search: string): IotHubFilterGroup[] {
  const filtered = filterIotHubItemsBySearch(items, search);
  if (items.length < IOT_HUB_FILTER_GROUPING_THRESHOLD) {
    return [{ label: null, items: filtered }];
  }
  const topKeys = new Set(
    [...items]
      .sort((a, b) => b.totalInstallCount - a.totalInstallCount)
      .slice(0, IOT_HUB_FILTER_POPULAR_LIMIT)
      .map(i => i.key)
  );
  const popular = filtered.filter(i => topKeys.has(i.key));
  const rest = filtered.filter(i => !topKeys.has(i.key));
  const groups: IotHubFilterGroup[] = [];
  if (popular.length) {
    groups.push({ label: 'iot-hub.most-popular', items: popular });
  }
  if (rest.length) {
    groups.push({ label: 'iot-hub.all', items: rest });
  }
  return groups;
}

export function resolveIotHubItemImageUrl(item: MpItemVersionView, api: IotHubApiService): string | null {
  if (item.image) {
    return api.resolveResourceUrl(item.image);
  }
  const resource = item.resources?.find(r => r.type === 'SCREENSHOT') || item.resources?.find(r => r.type === 'ICON');
  if (resource) {
    return api.resolveResourceUrl(`/api/resources/${resource.id}`);
  }
  return null;
}
