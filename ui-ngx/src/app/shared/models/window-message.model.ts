// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
export type WindowMessageType = 'widgetException' | 'widgetEditModeInited' | 'widgetEditUpdated' | 'openDashboardMessage'
  | 'reloadUserMessage' | 'toggleDashboardLayout' | 'widgetEditModeToggle';

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
