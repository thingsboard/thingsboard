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

import { TestBed } from '@angular/core/testing';
import { BrowserModule, DomSanitizer } from '@angular/platform-browser';
import {
  containsHtmlLike,
  sanitizeStringValue,
  sanitizeWithDiff,
  sanitizeWsMessage
} from './sanitize.utils';
import { CmdUpdateType } from '@shared/models/telemetry/telemetry.models';
import { afterAll, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';

describe('sanitize.utils', () => {

  let sanitizer: DomSanitizer;
  const originalWarn = console.warn;

  beforeAll(() => {
    console.warn = vi.fn();
  });

  afterAll(() => {
    console.warn = originalWarn;
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [BrowserModule]
    });
    sanitizer = TestBed.inject(DomSanitizer);
  });

  describe('containsHtmlLike', () => {

    it('should return false for plain strings', () => {
      expect(containsHtmlLike('hello')).toBe(false);
      expect(containsHtmlLike('simple text value')).toBe(false);
      expect(containsHtmlLike('')).toBe(false);
      expect(containsHtmlLike('temperature > 30')).toBe(false);
      expect(containsHtmlLike('3 < 5 but not html')).toBe(false);
    });

    it('should return false for numeric strings', () => {
      expect(containsHtmlLike('123.45')).toBe(false);
      expect(containsHtmlLike('0')).toBe(false);
      expect(containsHtmlLike('-1')).toBe(false);
      expect(containsHtmlLike('1e10')).toBe(false);
    });

    it('should return false for JSON strings', () => {
      expect(containsHtmlLike('{"key": "value"}')).toBe(false);
      expect(containsHtmlLike('[1, 2, 3]')).toBe(false);
    });

    it('should return true for HTML strings', () => {
      expect(containsHtmlLike('<script>alert(1)</script>')).toBe(true);
      expect(containsHtmlLike('<img src=x>')).toBe(true);
      expect(containsHtmlLike('<div>text</div>')).toBe(true);
      expect(containsHtmlLike('<b>bold</b>')).toBe(true);
      expect(containsHtmlLike('<!-- comment -->')).toBe(true);
      expect(containsHtmlLike('</div>')).toBe(true);
    });

    it('should return true for strings containing HTML tags', () => {
      expect(containsHtmlLike('value is <b>important</b>')).toBe(true);
      expect(containsHtmlLike('a<b>c')).toBe(true);
    });
  });

  describe('sanitizeStringValue', () => {

    it('should pass through plain strings unchanged', () => {
      expect(sanitizeStringValue(sanitizer, 'hello')).toBe('hello');
      expect(sanitizeStringValue(sanitizer, 'simple text')).toBe('simple text');
      expect(sanitizeStringValue(sanitizer, '')).toBe('');
    });

    it('should pass through numeric strings unchanged', () => {
      expect(sanitizeStringValue(sanitizer, '25.5')).toBe('25.5');
      expect(sanitizeStringValue(sanitizer, '0')).toBe('0');
      expect(sanitizeStringValue(sanitizer, '-1')).toBe('-1');
      expect(sanitizeStringValue(sanitizer, '1000000')).toBe('1000000');
    });

    it('should strip script tags', () => {
      const result = sanitizeStringValue(sanitizer, '<script>alert(1)</script>');
      expect(result).not.toContain('<script>');
      expect(result).not.toContain('alert');
    });

    it('should strip on* event handlers', () => {
      const result = sanitizeStringValue(sanitizer, '<img src="x" onerror="alert(1)">');
      expect(result).not.toContain('onerror');
      expect(result).not.toContain('alert');
    });

    it('should strip onclick handlers', () => {
      const result = sanitizeStringValue(sanitizer, '<div onclick="alert(1)">click me</div>');
      expect(result).not.toContain('onclick');
      expect(result).not.toContain('alert');
      expect(result).toContain('click me');
    });

    it('should strip onmouseover handlers', () => {
      const result = sanitizeStringValue(sanitizer, '<div onmouseover="alert(1)"><b>text</b></div>');
      expect(result).not.toContain('onmouseover');
      expect(result).toContain('text');
    });

    it('should strip iframe tags', () => {
      const result = sanitizeStringValue(sanitizer, '<iframe src="evil.com"></iframe>');
      expect(result).not.toContain('<iframe');
    });

    it('should strip object and embed tags', () => {
      const result = sanitizeStringValue(sanitizer, '<object data="evil.swf"></object><embed src="evil.swf">');
      expect(result).not.toContain('<object');
      expect(result).not.toContain('<embed');
    });

    it('should preserve safe HTML tags', () => {
      expect(sanitizeStringValue(sanitizer, '<b>bold</b>')).toContain('<b>bold</b>');
      expect(sanitizeStringValue(sanitizer, '<i>italic</i>')).toContain('<i>italic</i>');
      expect(sanitizeStringValue(sanitizer, '<span>text</span>')).toContain('<span>text</span>');
    });

    it('should handle HTML entities', () => {
      const result = sanitizeStringValue(sanitizer, '<span>&amp; &lt; &gt;</span>');
      expect(result).toContain('&amp;');
      expect(result).toContain('&lt;');
      expect(result).toContain('&gt;');
    });

    it('should handle SVG-based XSS', () => {
      const result = sanitizeStringValue(sanitizer, '<svg onload="alert(1)">');
      expect(result).not.toContain('onload');
      expect(result).not.toContain('alert');
    });

    it('should neutralize javascript: URLs', () => {
      const result = sanitizeStringValue(sanitizer, '<a href="javascript:alert(1)">click</a>');
      expect(result).toContain('unsafe:javascript:');
    });
  });

  describe('sanitizeWithDiff', () => {

    it('should report no change for plain strings', () => {
      const result = sanitizeWithDiff(sanitizer, 'hello');
      expect(result.changed).toBe(false);
      expect(result.sanitized).toBe('hello');
    });

    it('should report change for strings with XSS', () => {
      const result = sanitizeWithDiff(sanitizer, '<script>alert(1)</script>');
      expect(result.changed).toBe(true);
      expect(result.sanitized).not.toContain('<script>');
    });

    it('should report no change for numeric strings', () => {
      const result = sanitizeWithDiff(sanitizer, '42.5');
      expect(result.changed).toBe(false);
      expect(result.sanitized).toBe('42.5');
    });
  });

  describe('sanitizeWsMessage', () => {

    it('should sanitize SubscriptionUpdateMsg data values', () => {
      const msg: any = {
        subscriptionId: 1,
        errorCode: 0,
        errorMsg: '',
        data: {
          temperature: [[1709000000000, '25.5']],
          status: [[1709000000000, '<script>alert(1)</script>active']],
          name: [[1709000000000, 'Device A']]
        }
      };
      sanitizeWsMessage(sanitizer, msg);
      expect(msg.data.temperature[0][1]).toBe('25.5');
      expect(msg.data.status[0][1]).not.toContain('<script>');
      expect(msg.data.name[0][1]).toBe('Device A');
      expect(msg.subscriptionId).toBe(1);
    });

    it('should not modify structural fields in SubscriptionUpdateMsg', () => {
      const msg: any = {
        subscriptionId: 1,
        errorCode: 0,
        errorMsg: 'some error',
        data: {
          value: [[1709000000000, 'safe']]
        }
      };
      sanitizeWsMessage(sanitizer, msg);
      expect(msg.errorMsg).toBe('some error');
      expect(msg.subscriptionId).toBe(1);
      expect(msg.errorCode).toBe(0);
    });

    it('should sanitize EntityDataUpdateMsg TsValue.value in latest', () => {
      const msg: any = {
        cmdId: 1,
        cmdUpdateType: CmdUpdateType.ENTITY_DATA,
        data: {
          data: [{
            entityId: { id: 'abc', entityType: 'DEVICE' },
            latest: {
              ATTRIBUTE: {
                label: { ts: 1709000000000, value: '<script>xss</script>' },
                temperature: { ts: 1709000000000, value: '25.5' }
              }
            },
            timeseries: {}
          }]
        }
      };
      sanitizeWsMessage(sanitizer, msg);
      expect(msg.data.data[0].latest.ATTRIBUTE.label.value).not.toContain('<script>');
      expect(msg.data.data[0].latest.ATTRIBUTE.temperature.value).toBe('25.5');
      expect(msg.data.data[0].entityId.id).toBe('abc');
    });

    it('should sanitize EntityDataUpdateMsg TsValue.value in timeseries', () => {
      const msg: any = {
        cmdId: 1,
        cmdUpdateType: CmdUpdateType.ENTITY_DATA,
        data: {
          data: [{
            entityId: { id: 'abc', entityType: 'DEVICE' },
            latest: {},
            timeseries: {
              status: [
                { ts: 1709000000000, value: '<img onerror="alert(1)" src=x>' },
                { ts: 1709000001000, value: 'online' }
              ]
            }
          }]
        }
      };
      sanitizeWsMessage(sanitizer, msg);
      expect(msg.data.data[0].timeseries.status[0].value).not.toContain('onerror');
      expect(msg.data.data[0].timeseries.status[1].value).toBe('online');
    });

    it('should sanitize EntityDataUpdateMsg update array', () => {
      const msg: any = {
        cmdId: 1,
        cmdUpdateType: CmdUpdateType.ENTITY_DATA,
        update: [{
          entityId: { id: 'abc', entityType: 'DEVICE' },
          latest: {
            ATTRIBUTE: {
              name: { ts: 1709000000000, value: '<div onclick="steal()">device</div>' }
            }
          },
          timeseries: {}
        }]
      };
      sanitizeWsMessage(sanitizer, msg);
      expect(msg.update[0].latest.ATTRIBUTE.name.value).not.toContain('onclick');
      expect(msg.update[0].latest.ATTRIBUTE.name.value).toContain('device');
    });

    it('should sanitize AlarmDataUpdateMsg latest values and alarm type', () => {
      const msg: any = {
        cmdId: 2,
        cmdUpdateType: CmdUpdateType.ALARM_DATA,
        data: {
          data: [{
            createdTime: 1709000000000,
            type: '<img src=x onerror=alert(1)>',
            latest: {
              ATTRIBUTE: {
                name: { ts: 1709000000000, value: '<div onclick="steal()">device</div>' }
              }
            }
          }]
        }
      };
      sanitizeWsMessage(sanitizer, msg);
      expect(msg.data.data[0].type).not.toContain('onerror');
      expect(msg.data.data[0].latest.ATTRIBUTE.name.value).not.toContain('onclick');
      expect(msg.data.data[0].latest.ATTRIBUTE.name.value).toContain('device');
    });

    it('should sanitize NotificationsUpdateMsg subject and text', () => {
      const msg: any = {
        cmdId: 3,
        cmdUpdateType: CmdUpdateType.NOTIFICATIONS,
        totalUnreadCount: 1,
        sequenceNumber: 5,
        notifications: [{
          id: { id: 'notif1', entityType: 'NOTIFICATION' },
          subject: '<script>steal()</script>Important',
          text: '<img onerror="alert(1)" src=x>Message body',
          type: 'GENERAL'
        }]
      };
      sanitizeWsMessage(sanitizer, msg);
      expect(msg.notifications[0].subject).not.toContain('<script>');
      expect(msg.notifications[0].text).not.toContain('onerror');
      expect(msg.totalUnreadCount).toBe(1);
      expect(msg.notifications[0].type).toBe('GENERAL');
    });

    it('should sanitize NotificationsUpdateMsg update field', () => {
      const msg: any = {
        cmdId: 3,
        cmdUpdateType: CmdUpdateType.NOTIFICATIONS,
        totalUnreadCount: 1,
        sequenceNumber: 5,
        update: {
          id: { id: 'notif2', entityType: 'NOTIFICATION' },
          subject: '<iframe src="evil.com">Urgent</iframe>',
          text: 'Safe text',
          type: 'GENERAL'
        }
      };
      sanitizeWsMessage(sanitizer, msg);
      expect(msg.update.subject).not.toContain('<iframe');
      expect(msg.update.text).toBe('Safe text');
    });

    it('should not modify non-data messages (EntityCount, AlarmCount)', () => {
      const msg: any = {
        cmdId: 4,
        cmdUpdateType: CmdUpdateType.COUNT_DATA,
        count: 42
      };
      sanitizeWsMessage(sanitizer, msg);
      expect(msg.count).toBe(42);
      expect(msg.cmdId).toBe(4);
    });

    it('should preserve non-string values', () => {
      const msg: any = {
        subscriptionId: 1,
        errorCode: 0,
        errorMsg: '',
        data: {
          temperature: [[1709000000000, '25.5', 1]],
          active: [[1709000000000, 'true']]
        }
      };
      sanitizeWsMessage(sanitizer, msg);
      expect(msg.data.temperature[0][0]).toBe(1709000000000);
      expect(msg.data.temperature[0][1]).toBe('25.5');
      expect(msg.data.temperature[0][2]).toBe(1);
    });
  });

  describe('Change detection', () => {

    it('should detect and report changes for XSS payloads', () => {
      const testCases = [
        '<script>alert(1)</script>',
        '<img src=x onerror="alert(1)">',
        '<div onclick="steal()">text</div>',
        '<iframe src="evil.com"></iframe>',
        '<a href="javascript:alert(1)">click</a>',
        '<svg onload="alert(1)">',
      ];

      const changes: Array<{ input: string; output: string }> = [];
      for (const input of testCases) {
        const result = sanitizeWithDiff(sanitizer, input);
        if (result.changed) {
          changes.push({ input, output: result.sanitized });
        }
      }

      console.log('[CHANGE DETECTION] Changes detected:');
      for (const change of changes) {
        console.log(`  CHANGED: '${change.input}' → '${change.output}'`);
      }

      expect(changes.length).toBe(testCases.length);
    });

    it('should report no changes for safe strings', () => {
      const safeStrings = [
        'hello world',
        '25.5',
        '{"key": "value"}',
        'temperature > 30',
        '',
      ];

      for (const input of safeStrings) {
        const result = sanitizeWithDiff(sanitizer, input);
        expect(result.changed).toBe(false);
        expect(result.sanitized).toBe(input);
      }
    });

    it('should process array of mixed attributes and report changes', () => {
      const attributes = [
        { key: 'temperature', value: '25.5' },
        { key: 'status', value: '<script>alert("xss")</script>active' },
        { key: 'name', value: 'Device A' },
        { key: 'description', value: '<img src=x onerror=alert(1)>My device' },
        { key: 'label', value: 'normal label' },
        { key: 'config', value: '{"threshold": 30}' },
      ];

      const report: Array<{ key: string; before: string; after: string }> = [];
      for (const attr of attributes) {
        const result = sanitizeWithDiff(sanitizer, attr.value);
        if (result.changed) {
          report.push({ key: attr.key, before: attr.value, after: result.sanitized });
        }
      }

      console.log('[CHANGE DETECTION] Attribute changes:');
      for (const entry of report) {
        console.log(`  ${entry.key}: '${entry.before}' → '${entry.after}'`);
      }

      expect(report.length).toBe(2);
      expect(report[0].key).toBe('status');
      expect(report[1].key).toBe('description');
    });
  });
});
