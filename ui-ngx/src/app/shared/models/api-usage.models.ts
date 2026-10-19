// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
export enum ApiUsageStateValue {
  ENABLED = 'ENABLED',
  WARNING = 'WARNING',
  DISABLED = 'DISABLED'
}

export const ApiUsageStateValueTranslationMap = new Map<ApiUsageStateValue, string>([
  [ApiUsageStateValue.ENABLED, 'notification.enabled'],
  [ApiUsageStateValue.WARNING, 'notification.warning'],
  [ApiUsageStateValue.DISABLED, 'notification.disabled'],
]);

export enum ApiFeature {
  TRANSPORT = 'TRANSPORT',
  DB = 'DB',
  RE = 'RE',
  JS = 'JS',
  TBEL = 'TBEL',
  EMAIL = 'EMAIL',
  SMS = 'SMS',
  ALARM = 'ALARM'
}

export const ApiFeatureTranslationMap = new Map<ApiFeature, string>([
  [ApiFeature.TRANSPORT, 'api-usage.device-api'],
  [ApiFeature.DB, 'api-usage.telemetry-persistence'],
  [ApiFeature.RE, 'api-usage.rule-engine-executions'],
  [ApiFeature.JS, 'api-usage.javascript-executions'],
  [ApiFeature.TBEL, 'api-usage.tbel-executions'],
  [ApiFeature.EMAIL, 'api-usage.email-messages'],
  [ApiFeature.SMS, 'api-usage.sms-messages'],
  [ApiFeature.ALARM, 'api-usage.alarm'],
]);
