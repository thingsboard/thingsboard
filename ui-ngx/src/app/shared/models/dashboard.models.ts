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

import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { DashboardId } from '@shared/models/id/dashboard-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { ShortCustomerInfo } from '@shared/models/customer.model';
import { Widget } from './widget.models';
import { Timewindow } from '@shared/models/time/time.models';
import { EntityAliases } from './alias.models';
import { Filters } from '@shared/models/query/query.models';
import { MatDialogRef } from '@angular/material/dialog';
import { HasTenantId, HasVersion } from '@shared/models/entity.models';

export interface DashboardInfo extends BaseData<DashboardId>, HasTenantId, HasVersion, ExportableEntity<DashboardId> {
  tenantId?: TenantId;
  title?: string;
  image?: string;
  assignedCustomers?: Array<ShortCustomerInfo>;
  mobileHide?: boolean;
  mobileOrder?: number;
}

export interface WidgetLayout {
  sizeX?: number;
  sizeY?: number;
  desktopHide?: boolean;
  mobileHide?: boolean;
  mobileHeight?: number;
  mobileOrder?: number;
  col?: number;
  row?: number;
  resizable?: boolean;
  preserveAspectRatio?: boolean;
}

export interface WidgetLayouts {
  [id: string]: WidgetLayout;
}

export enum LayoutType {
  default = 'default',
  scada = 'scada',
  divider = 'divider',
}

export const layoutTypes = Object.keys(LayoutType) as LayoutType[];

export const layoutTypeTranslationMap = new Map<LayoutType, string>(
  [
    [ LayoutType.default, 'dashboard.layout-type-default' ],
    [ LayoutType.scada, 'dashboard.layout-type-scada' ],
    [ LayoutType.divider, 'dashboard.layout-type-divider' ],
  ]
);

export enum ViewFormatType {
  grid = 'grid',
  list = 'list',
}

export const viewFormatTypes = Object.keys(ViewFormatType) as ViewFormatType[];

export const viewFormatTypeTranslationMap = new Map<ViewFormatType, string>(
  [
    [ ViewFormatType.grid, 'dashboard.view-format-type-grid' ],
    [ ViewFormatType.list, 'dashboard.view-format-type-list' ],
  ]
);

export interface GridSettings {
  layoutType?: LayoutType;
  backgroundColor?: string;
  columns?: number;
  minColumns?: number;
  margin?: number;
  outerMargin?: boolean;
  viewFormat?: ViewFormatType;
  backgroundSizeMode?: string;
  backgroundImageUrl?: string;
  autoFillHeight?: boolean;
  rowHeight?: number;
  mobileAutoFillHeight?: boolean;
  mobileRowHeight?: number;
  mobileDisplayLayoutFirst?: boolean;
  layoutDimension?: LayoutDimension;
}

export interface DashboardLayout {
  widgets: WidgetLayouts;
  gridSettings: GridSettings;
  breakpoints?: {[breakpointId in BreakpointId]?: Omit<DashboardLayout, 'breakpoints'>};
}

export declare type DashboardLayoutInfo = {[breakpointId in BreakpointId]?: BreakpointLayoutInfo};

export interface BreakpointLayoutInfo {
  widgetIds?: string[];
  widgetLayouts?: WidgetLayouts;
  gridSettings?: GridSettings;
}

export declare type BreakpointSystemId = 'default' | 'xs' | 'sm' | 'md' | 'lg' | 'xl';
export declare type BreakpointId = BreakpointSystemId | string;

export interface BreakpointInfo {
  id: BreakpointId;
  maxWidth?: number;
  minWidth?: number;
  value?: string;
}

export const breakpointIdTranslationMap = new Map<BreakpointId, string>([
  ['default', 'dashboard.breakpoints-id.default'],
  ['xs', 'dashboard.breakpoints-id.xs'],
  ['sm', 'dashboard.breakpoints-id.sm'],
  ['md', 'dashboard.breakpoints-id.md'],
  ['lg', 'dashboard.breakpoints-id.lg'],
  ['xl', 'dashboard.breakpoints-id.xl'],
]);

export const breakpointIdIconMap = new Map<BreakpointId, string>([
  ['default', 'desktop_windows'],
  ['xs', 'phone_iphone'],
  ['sm', 'tablet_mac'],
  ['md', 'computer'],
  ['lg', 'monitor'],
  ['xl', 'desktop_windows'],
]);

export interface LayoutDimension {
  type?: LayoutDimensionType;
  fixedWidth?: number;
  fixedLayout?: DashboardLayoutId;
  leftWidthPercentage?: number;
}

export declare type DashboardLayoutId = 'main' | 'right';

export declare type LayoutDimensionType = 'percentage' | 'fixed';

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
  showDashboardLogo?: boolean;
  dashboardLogoUrl?: string;
  showDashboardTimewindow?: boolean;
  showDashboardExport?: boolean;
  showUpdateDashboardImage?: boolean;
  toolbarAlwaysOpen?: boolean;
  hideToolbar?: boolean;
  titleColor?: string;
  dashboardCss?: string;
}

export interface DashboardConfiguration {
  timewindow?: Timewindow;
  settings?: DashboardSettings;
  widgets?: {[id: string]: Widget } | Widget[];
  states?: {[id: string]: DashboardState };
  entityAliases: EntityAliases;
  filters: Filters;
  [key: string]: any;
}

export interface Dashboard extends DashboardInfo {
  configuration?: DashboardConfiguration;
  dialogRef?: MatDialogRef<any>;
  resources?: Array<any>;
}

export interface HomeDashboard extends Dashboard {
  hideDashboardToolbar: boolean;
}

export interface HomeDashboardInfo {
  dashboardId: DashboardId;
  hideDashboardToolbar: boolean;
}

export interface DashboardSetup extends Dashboard {
  assignedCustomerIds?: Array<string>;
}

export const isPublicDashboard = (dashboard: DashboardInfo): boolean => {
  if (dashboard && dashboard.assignedCustomers) {
    return dashboard.assignedCustomers
      .filter(customerInfo => customerInfo.public).length > 0;
  } else {
    return false;
  }
};

export const getDashboardAssignedCustomersText = (dashboard: DashboardInfo): string => {
  if (dashboard && dashboard.assignedCustomers && dashboard.assignedCustomers.length > 0) {
    return dashboard.assignedCustomers
      .filter(customerInfo => !customerInfo.public)
      .map(customerInfo => customerInfo.title)
      .join(', ');
  } else {
    return '';
  }
};

export const isCurrentPublicDashboardCustomer = (dashboard: DashboardInfo, customerId: string): boolean => {
  if (customerId && dashboard && dashboard.assignedCustomers) {
    return dashboard.assignedCustomers.filter(customerInfo =>
      customerInfo.public && customerId === customerInfo.customerId.id).length > 0;
  } else {
    return false;
  }
};
