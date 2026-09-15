// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
