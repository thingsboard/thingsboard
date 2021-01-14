///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { BaseData } from '@shared/models/base-data';
import { DashboardId } from '@shared/models/id/dashboard-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { ShortCustomerInfo } from '@shared/models/customer.model';
import { Widget } from './widget.models';
import { Timewindow } from '@shared/models/time/time.models';
import { EntityAliases } from './alias.models';
import { Filters } from '@shared/models/query/query.models';

export interface DashboardInfo extends BaseData<DashboardId> {
  tenantId?: TenantId;
  title?: string;
  assignedCustomers?: Array<ShortCustomerInfo>;
}

export interface WidgetLayout {
  sizeX?: number;
  sizeY?: number;
  mobileHeight?: number;
  mobileOrder?: number;
  col?: number;
  row?: number;
}

export interface WidgetLayouts {
  [id: string]: WidgetLayout;
}

export interface GridSettings {
  backgroundColor?: string;
  color?: string;
  columns?: number;
  margin?: number;
  backgroundSizeMode?: string;
  backgroundImageUrl?: string;
  autoFillHeight?: boolean;
  mobileAutoFillHeight?: boolean;
  mobileRowHeight?: number;
  [key: string]: any;
}

export interface DashboardLayout {
  widgets: WidgetLayouts;
  gridSettings: GridSettings;
}

export interface DashboardLayoutInfo {
  widgetIds?: string[];
  widgetLayouts?: WidgetLayouts;
  gridSettings?: GridSettings;
}

export declare type DashboardLayoutId = 'main' | 'right';

export declare type DashboardStateLayouts = {[key in DashboardLayoutId]?: DashboardLayout};

export declare type DashboardLayoutsInfo = {[key in DashboardLayoutId]?: DashboardLayoutInfo};

export interface DashboardState {
  name: string;
  root: boolean;
  layouts: DashboardStateLayouts;
}

export declare type StateControllerId = 'entity' | 'default' | string;

export interface DashboardSettings {
  stateControllerId?: StateControllerId;
  showTitle?: boolean;
  showDashboardsSelect?: boolean;
  showEntitiesSelect?: boolean;
  showFilters?: boolean;
  showDashboardTimewindow?: boolean;
  showDashboardExport?: boolean;
  toolbarAlwaysOpen?: boolean;
  titleColor?: string;
}

export interface DashboardConfiguration {
  timewindow?: Timewindow;
  settings?: DashboardSettings;
  widgets?: {[id: string]: Widget } | Widget[];
  states?: {[id: string]: DashboardState };
  entityAliases?: EntityAliases;
  filters?: Filters;
  [key: string]: any;
}

export interface Dashboard extends DashboardInfo {
  configuration?: DashboardConfiguration;
}

export function isPublicDashboard(dashboard: DashboardInfo): boolean {
  if (dashboard && dashboard.assignedCustomers) {
    return dashboard.assignedCustomers
      .filter(customerInfo => customerInfo.public).length > 0;
  } else {
    return false;
  }
}

export function getDashboardAssignedCustomersText(dashboard: DashboardInfo): string {
  if (dashboard && dashboard.assignedCustomers && dashboard.assignedCustomers.length > 0) {
    return dashboard.assignedCustomers
      .filter(customerInfo => !customerInfo.public)
      .map(customerInfo => customerInfo.title)
      .join(', ');
  } else {
    return '';
  }
}

export function isCurrentPublicDashboardCustomer(dashboard: DashboardInfo, customerId: string): boolean {
  if (customerId && dashboard && dashboard.assignedCustomers) {
    return dashboard.assignedCustomers.filter(customerInfo => {
      return customerInfo.public && customerId === customerInfo.customerId.id;
    }).length > 0;
  } else {
    return false;
  }
}
