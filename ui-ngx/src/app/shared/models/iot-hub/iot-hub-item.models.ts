// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Direction } from '@shared/models/page/sort-order';

export enum ItemType {
  WIDGET = 'WIDGET',
  DASHBOARD = 'DASHBOARD',
  SOLUTION_TEMPLATE = 'SOLUTION_TEMPLATE',
  CALCULATED_FIELD = 'CALCULATED_FIELD',
  ALARM_RULE = 'ALARM_RULE',
  RULE_CHAIN = 'RULE_CHAIN',
  DEVICE = 'DEVICE'
}

export const itemTypeTranslations = new Map<ItemType, string>(
  [
    [ItemType.WIDGET, 'item.type-widget'],
    [ItemType.DASHBOARD, 'item.type-dashboard'],
    [ItemType.SOLUTION_TEMPLATE, 'item.type-solution-template'],
    [ItemType.CALCULATED_FIELD, 'item.type-calculated-field'],
    [ItemType.ALARM_RULE, 'item.type-alarm-rule'],
    [ItemType.RULE_CHAIN, 'item.type-rule-chain'],
    [ItemType.DEVICE, 'item.type-device']
  ]
);

// Canonical icon lookup per item type. Values are tb-icon
// identifiers (Material symbol names or `mdi:*` strings) and should
// be used everywhere an icon is rendered for an item type so the
// mapping stays consistent across the app.
export const itemTypeIcons: Record<string, string> = {
  [ItemType.WIDGET]: 'widgets',
  [ItemType.DASHBOARD]: 'dashboard',
  [ItemType.SOLUTION_TEMPLATE]: 'apps',
  [ItemType.CALCULATED_FIELD]: 'mdi:function-variant',
  [ItemType.RULE_CHAIN]: 'settings_ethernet',
  [ItemType.ALARM_RULE]: 'mdi:bell-cog',
  [ItemType.DEVICE]: 'devices_other'
};

export const getItemTypeIcon = (type?: string | null): string =>
  type && itemTypeIcons[type] ? itemTypeIcons[type] : 'category';

/**
 * Item types discoverable to creators in the marketplace UI.
 * DASHBOARD is intentionally absent (IoT Hub no longer accepts Dashboard contributions).
 * Defensive code paths (item card, detail dialog descriptor switch, installed-items table,
 * install handler, /iot-hub/dashboards route) remain functional for already-installed items.
 */
export const CREATOR_VISIBLE_ITEM_TYPES: ItemType[] = [
  ItemType.WIDGET,
  ItemType.SOLUTION_TEMPLATE,
  ItemType.DEVICE,
  ItemType.CALCULATED_FIELD,
  ItemType.ALARM_RULE,
  ItemType.RULE_CHAIN,
];

/**
 * Item types a surface that mixes them lays out, in the order they are shown. Distinct from
 * CREATOR_VISIBLE_ITEM_TYPES, which is the same six as a membership test and carries the type
 * tabs' own order: here DEVICE leads, matching the hero popup's sections and the website.
 */
export const CROSS_TYPE_ITEM_TYPES: ItemType[] = [
  ItemType.DEVICE,
  ItemType.SOLUTION_TEMPLATE,
  ItemType.WIDGET,
  ItemType.CALCULATED_FIELD,
  ItemType.ALARM_RULE,
  ItemType.RULE_CHAIN,
];

/** Sort property served by relevance ranking. */
export const RELEVANCE_SORT_PROPERTY = 'relevance';

export interface SortOption {
  value: string;
  label: string;
  direction: Direction;
}

/**
 * The sort menu every IoT Hub surface carrying a search field offers, and the order it offers
 * them in: the first entry is the default each surface opens on.
 *
 * Relevance is that default in both states. With text it ranks the answer; with none the backend
 * substitutes the install count, so a surface opens on the order it opened on before and nothing
 * switches on the field state. Agreed with the product owner and recorded on TBIOH-33, which
 * supersedes that ticket's SCOPE-IN 2, SCOPE-IN 4 and AC-4.
 *
 * One list rather than one per surface: a new key must reach all of them, and the surface that
 * missed it would keep a different default without failing.
 */
export const IOT_HUB_SORT_OPTIONS: SortOption[] = [
  { value: RELEVANCE_SORT_PROPERTY, label: 'iot-hub.sort-most-relevant', direction: Direction.DESC },
  { value: 'totalInstallCount', label: 'iot-hub.sort-most-installed', direction: Direction.DESC },
  { value: 'publishedTime', label: 'iot-hub.sort-newest', direction: Direction.DESC },
  { value: 'name', label: 'iot-hub.sort-name', direction: Direction.ASC }
];

export interface FilterParamInfo {
  key: string;
  totalItems: number;
  totalInstallCount: number;
}

export interface ItemTypeFilterInfo {
  types: FilterParamInfo[];
  categories: FilterParamInfo[];
  useCases: FilterParamInfo[];
  vendors: FilterParamInfo[];
  hardwareTypes: FilterParamInfo[];
  connectivities: Record<string, FilterParamInfo[]>;
}

export interface WidgetCategory {
  name: string;
  image: string;
}
