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

export enum ItemType {
  WIDGET = 'WIDGET',
  DASHBOARD = 'DASHBOARD',
  SOLUTION_TEMPLATE = 'SOLUTION_TEMPLATE',
  CALCULATED_FIELD = 'CALCULATED_FIELD',
  RULE_CHAIN = 'RULE_CHAIN',
  DEVICE = 'DEVICE'
}

export const itemTypeTranslations = new Map<ItemType, string>(
  [
    [ItemType.WIDGET, 'item.type-widget'],
    [ItemType.DASHBOARD, 'item.type-dashboard'],
    [ItemType.SOLUTION_TEMPLATE, 'item.type-solution-template'],
    [ItemType.CALCULATED_FIELD, 'item.type-calculated-field'],
    [ItemType.RULE_CHAIN, 'item.type-rule-chain'],
    [ItemType.DEVICE, 'item.type-device']
  ]
);

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
