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

import { FilterParamInfo } from '@shared/models/iot-hub/iot-hub-item.models';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';

export const IOT_HUB_FILTER_GROUPING_THRESHOLD = 11;
export const IOT_HUB_FILTER_POPULAR_LIMIT = 10;

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
