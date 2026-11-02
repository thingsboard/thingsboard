// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
import { FilterParamInfo, ItemType } from '@shared/models/iot-hub/iot-hub-item.models';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { TranslateService } from '@ngx-translate/core';

export const IOT_HUB_FILTER_GROUPING_THRESHOLD = 11;
export const IOT_HUB_FILTER_POPULAR_LIMIT = 10;
/** Above this many options a filter list scrolls, and the search page's facet gets a search box. */
export const IOT_HUB_FILTER_SCROLL_THRESHOLD = 8;

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

export function filterByLabel<T>(items: T[], search: string, labelOf: (item: T) => string): T[] {
  const normalized = (search || '').trim().toLowerCase();
  if (!normalized) {
    return items;
  }
  return items.filter(item => labelOf(item).toLowerCase().includes(normalized));
}

export function filterIotHubItemsBySearch(items: FilterParamInfo[], search: string): FilterParamInfo[] {
  return filterByLabel(items, search, item => item.key);
}

/** Column count of a CSS grid, from its computed tracks (`repeat(...)` before first layout). */
export function measureGridColumns(el: HTMLElement): number {
  void el.offsetWidth;
  const tracks = getComputedStyle(el).gridTemplateColumns;
  if (!tracks || tracks === 'none') {
    return 1;
  }
  if (tracks.startsWith('repeat(')) {
    const match = tracks.match(/^repeat\(\s*(\d+)\s*,/);
    return match ? parseInt(match[1], 10) : 1;
  }
  return Math.max(1, tracks.trim().split(/\s+/).filter(t => t.length > 0).length);
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

// ThingsBoard error codes whose message is written for the user: validation, permission, not found, limits
const SHOWN_ERROR_CODES = new Set([20, 30, 31, 32, 33, 34, 35, 40, 41]);
const MAX_SHOWN_ERROR_LENGTH = 200;

/** The message as it is when it reads like one written for the user, null for an internal one (a stack trace, a URL, too long). */
export const readableMessage = (message: unknown): string | null =>
  typeof message === 'string' && message.trim() && message.length <= MAX_SHOWN_ERROR_LENGTH &&
  !/exception|java\.|https?:\/\/|\bat [\w.$]+\(/i.test(message) ? message : null;

/**
 * What the user sees for a failed request: ThingsBoard's validation and limit messages as they are,
 * anything else (a server error, the network, a bug) as a common message, with the details in the console.
 */
export const requestFailureMessage = (error: any, translate: TranslateService): string => {
  const body = error?.error;
  const message = SHOWN_ERROR_CODES.has(body?.errorCode) ? readableMessage(body.message) : null;
  if (message) {
    return message;
  }
  console.error('Device package request failed', error);
  return translate.instant('iot-hub.device-install-unexpected-error');
};

/** A translated text with a link written as <a>…</a>, which the translation may put anywhere in it, split around the link. */
export const splitTextWithLink = (text: string): { before: string; link: string; after: string } => {
  const match = /^([\s\S]*)<a>([\s\S]*)<\/a>([\s\S]*)$/.exec(text);
  return match ? { before: match[1], link: match[2], after: match[3] } : { before: text, link: '', after: '' };
};

/** Saves an entity as a JSON file named after it. */
export const downloadEntityJson = (entity: any, name: string): void => {
  const blob = new Blob([JSON.stringify(entity, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${name.replace(/[^\w\- ]+/g, '_')}.json`;
  link.click();
  // Revoked right away, the URL may be gone before the download starts
  setTimeout(() => URL.revokeObjectURL(url), 0);
};
