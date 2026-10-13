// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
export type WindowMessageType = 'openReport' | 'clearReport' | 'reportResult' | 'waitReportWidgets'  | 'widgetException' | 'widgetEditModeInited'
  | 'widgetEditUpdated' | 'dashboardStateSelected' | 'openDashboardMessage'
  | 'reloadUserMessage' | 'toggleDashboardLayout' | 'resetRecaptcha' | 'widgetEditModeToggle';

export interface WindowMessage {
  type: WindowMessageType;
  data?: any;
}

export interface OpenDashboardMessage {
  dashboardId: string;
  state?: string;
  hideToolbar?: boolean;
  embedded?: boolean;
}

export interface ReloadUserMessage {
  accessToken: string;
  refreshToken: string;
}

export interface OpenReportMessage {
  dashboardId: string;
  timeout: number;
  accessToken?: string;
  publicId?: string;
  state?: string;
  reportTimewindow?: object;
}

export interface WaitWidgetsMessage {
  timeout: number;
}

export interface ReportResultMessage {
  success: boolean;
  error?: string;
  pageHeight?: number;
}

