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

import { SecurityContext } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import {
  isAlarmDataUpdateMsg,
  isEntityDataUpdateMsg,
  isNotificationsUpdateMsg,
  WebsocketDataMsg
} from '@shared/models/telemetry/telemetry.models';
import { TsValue } from '@shared/models/query/query.models';

export function containsHtmlLike(val: string): boolean {
  return /<[a-zA-Z/!]/.test(val);
}

export function sanitizeStringValue(sanitizer: DomSanitizer, val: string): string {
  if (!containsHtmlLike(val)) return val;
  return sanitizer.sanitize(SecurityContext.HTML, val) ?? '';
}

export function sanitizeWithDiff(sanitizer: DomSanitizer, val: string): { sanitized: string; changed: boolean } {
  const sanitized = sanitizeStringValue(sanitizer, val);
  return { sanitized, changed: sanitized !== val };
}

function sanitizeTsValue(sanitizer: DomSanitizer, tsValue: TsValue): void {
  if (typeof tsValue.value === 'string' && containsHtmlLike(tsValue.value)) {
    tsValue.value = sanitizer.sanitize(SecurityContext.HTML, tsValue.value) ?? '';
  }
}

function sanitizeLatest(sanitizer: DomSanitizer, latest: {[entityKeyType: string]: {[key: string]: TsValue}}): void {
  for (const entityKeyType of Object.keys(latest)) {
    const keyTypeValues = latest[entityKeyType];
    for (const key of Object.keys(keyTypeValues)) {
      sanitizeTsValue(sanitizer, keyTypeValues[key]);
    }
  }
}

function sanitizeTimeseries(sanitizer: DomSanitizer, timeseries: {[key: string]: Array<TsValue>}): void {
  for (const key of Object.keys(timeseries)) {
    for (const tsValue of timeseries[key]) {
      sanitizeTsValue(sanitizer, tsValue);
    }
  }
}

export function sanitizeWsMessage(sanitizer: DomSanitizer, message: WebsocketDataMsg): void {
  if ('subscriptionId' in message && message.data) {
    // SubscriptionUpdateMsg: data[key] = Array<[ts, value, count?]>
    for (const key of Object.keys(message.data)) {
      for (const entry of message.data[key]) {
        if (typeof entry[1] === 'string' && containsHtmlLike(entry[1])) {
          entry[1] = sanitizer.sanitize(SecurityContext.HTML, entry[1]) ?? '';
        }
      }
    }
  } else if (isEntityDataUpdateMsg(message)) {
    // EntityDataUpdateMsg: data.data[].latest + timeseries
    const entities = [...(message.data?.data || []), ...(message.update || [])];
    for (const entity of entities) {
      if (entity.latest) {
        sanitizeLatest(sanitizer, entity.latest);
      }
      if (entity.timeseries) {
        sanitizeTimeseries(sanitizer, entity.timeseries);
      }
    }
  } else if (isAlarmDataUpdateMsg(message)) {
    // AlarmDataUpdateMsg: data.data[].latest + alarm string fields
    const alarms = [...(message.data?.data || []), ...(message.update || [])];
    for (const alarm of alarms) {
      if (alarm.latest) {
        sanitizeLatest(sanitizer, alarm.latest);
      }
      if (typeof alarm.type === 'string' && containsHtmlLike(alarm.type)) {
        alarm.type = sanitizer.sanitize(SecurityContext.HTML, alarm.type) ?? '';
      }
    }
  } else if (isNotificationsUpdateMsg(message)) {
    // NotificationsUpdateMsg: notifications[].subject + text, update.subject + text
    const notifications = [...(message.notifications || [])];
    if (message.update) {
      notifications.push(message.update);
    }
    for (const notification of notifications) {
      if (typeof notification.subject === 'string' && containsHtmlLike(notification.subject)) {
        (notification as any).subject = sanitizer.sanitize(SecurityContext.HTML, notification.subject) ?? '';
      }
      if (typeof notification.text === 'string' && containsHtmlLike(notification.text)) {
        (notification as any).text = sanitizer.sanitize(SecurityContext.HTML, notification.text) ?? '';
      }
    }
  }
}
