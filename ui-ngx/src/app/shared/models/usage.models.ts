// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
export interface UsageInfo {
  devices: number;
  maxDevices: number;
  assets: number;
  maxAssets: number;
  customers: number;
  maxCustomers: number;
  users: number;
  maxUsers: number;
  dashboards: number;
  maxDashboards: number;

  transportMessages: number;
  maxTransportMessages: number;
  jsExecutions: number;
  maxJsExecutions: number;
  emails: number;
  maxEmails: number;
  sms: number;
  maxSms: number;
  alarms: number;
  maxAlarms: number;
  aiCredits: number;
  maxAiCredits: number;
}
