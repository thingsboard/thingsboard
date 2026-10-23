// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Direction } from '@shared/models/page/sort-order';

export enum ItemType {
  WIDGET = 'WIDGET',
  SOLUTION_TEMPLATE = 'SOLUTION_TEMPLATE',
  CALCULATED_FIELD = 'CALCULATED_FIELD',
  ALARM_RULE = 'ALARM_RULE',
  RULE_CHAIN = 'RULE_CHAIN',
  DEVICE = 'DEVICE'
}

export const itemTypeTranslations = new Map<ItemType, string>(
  [
    [ItemType.WIDGET, 'item.type-widget'],
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
  [ItemType.SOLUTION_TEMPLATE]: 'apps',
  [ItemType.CALCULATED_FIELD]: 'mdi:function-variant',
  [ItemType.RULE_CHAIN]: 'settings_ethernet',
  [ItemType.ALARM_RULE]: 'mdi:bell-cog',
  [ItemType.DEVICE]: 'devices_other'
};

export const getItemTypeIcon = (type?: string | null): string =>
  type && itemTypeIcons[type] ? itemTypeIcons[type] : 'category';

/** Colour for a type with no colour of its own. */
export const DEFAULT_ITEM_TYPE_COLOR = '#5f6368';

/** Colour per item type, wherever an item's type is marked with one. */
export const itemTypeColors: Record<string, string> = {
  [ItemType.WIDGET]: '#2c9755',
  [ItemType.SOLUTION_TEMPLATE]: '#2b6bb4',
  [ItemType.CALCULATED_FIELD]: '#3cb4e0',
  [ItemType.RULE_CHAIN]: '#a95ae2',
  [ItemType.ALARM_RULE]: '#d66f2e',
  [ItemType.DEVICE]: '#4b63cc'
};

export const getItemTypeColor = (type?: string | null): string =>
  type && itemTypeColors[type] ? itemTypeColors[type] : DEFAULT_ITEM_TYPE_COLOR;

/**
 * Item types discoverable to creators in the marketplace UI.
 */
export const CREATOR_VISIBLE_ITEM_TYPES: ItemType[] = [
  ItemType.WIDGET,
  ItemType.SOLUTION_TEMPLATE,
  ItemType.DEVICE,
  ItemType.CALCULATED_FIELD,
  ItemType.ALARM_RULE,
  ItemType.RULE_CHAIN,
];

/** Item types a cross-type surface (search page, popup) shows, in display order. */
export const CROSS_TYPE_ITEM_TYPES: ItemType[] = [
  ItemType.DEVICE,
  ItemType.SOLUTION_TEMPLATE,
  ItemType.WIDGET,
  ItemType.CALCULATED_FIELD,
  ItemType.ALARM_RULE,
  ItemType.RULE_CHAIN,
];

/** Types drawn as a coloured icon tile rather than a screenshot. */
export const isCompactItemType = (type?: string | null): boolean =>
  type === ItemType.CALCULATED_FIELD || type === ItemType.ALARM_RULE || type === ItemType.RULE_CHAIN;

/** Sort property served by relevance ranking. */
export const RELEVANCE_SORT_PROPERTY = 'relevance';

export interface SortOption {
  value: string;
  label: string;
  direction: Direction;
}

/**
 * The sort menu of every IoT Hub surface with a search field; the first entry is the default.
 * Relevance stays the default with an empty field, where it orders by install count.
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
