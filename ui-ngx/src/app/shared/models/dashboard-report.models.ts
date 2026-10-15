// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Timewindow } from '@shared/models/time/time.models';

export type DashboardReportType = 'pdf' | 'jpeg' | 'png';

export const dashboardReportTypes: DashboardReportType[] = ['pdf', 'jpeg', 'png'];

export const dashboardReportTypeNamesMap = new Map<DashboardReportType, string>(
  [
    ['pdf', 'PDF'],
    ['jpeg', 'JPEG'],
    ['png', 'PNG'],
  ]
);

export interface DashboardReportParams {
  type: DashboardReportType;
  timezone: string;
  state?: string;
  timewindow?: Timewindow;
}

export interface DashboardReportConfig extends DashboardReportParams {
  baseUrl: string;
  dashboardId: string;
  useDashboardTimewindow: boolean;
  namePattern: string;
  useCurrentUserCredentials: boolean;
  userId: string;
}
