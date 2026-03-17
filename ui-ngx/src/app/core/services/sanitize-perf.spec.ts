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
import { afterAll, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { containsHtmlLike, sanitizeStringValue, sanitizeWsMessage } from './sanitize.utils';
import {
  generateSubscriptionUpdate,
  generateEntityDataUpdate,
  generateAlarmDataUpdate,
  generateNotificationsUpdate,
} from './__fixtures__/generate-mock-data';
import realData from './__fixtures__/real-telemetry-data.json';

// --- Benchmark infrastructure ---

interface BenchmarkStats {
  label: string;
  runs: number;
  min: number;
  max: number;
  mean: number;
  median: number;
  p95: number;
  stddev: number;
}

const ITERATIONS = 50;
const WARMUP = 5;

function deepClone<T>(obj: T): T {
  return JSON.parse(JSON.stringify(obj));
}

function calcStats(label: string, durations: number[]): BenchmarkStats {
  const sorted = [...durations].sort((a, b) => a - b);
  const n = sorted.length;
  const sum = sorted.reduce((a, b) => a + b, 0);
  const mean = sum / n;
  const median = n % 2 === 0
    ? (sorted[n / 2 - 1] + sorted[n / 2]) / 2
    : sorted[Math.floor(n / 2)];
  const p95 = sorted[Math.ceil(n * 0.95) - 1];
  const variance = sorted.reduce((acc, d) => acc + (d - mean) ** 2, 0) / n;
  const stddev = Math.sqrt(variance);
  return { label, runs: n, min: sorted[0], max: sorted[n - 1], mean, median, p95, stddev };
}

function runBenchmark(
  label: string,
  dataFactory: () => any,
  workFn: (data: any) => void,
): BenchmarkStats {
  for (let i = 0; i < WARMUP; i++) {
    workFn(dataFactory());
  }
  const durations: number[] = [];
  for (let i = 0; i < ITERATIONS; i++) {
    const data = dataFactory();
    const start = performance.now();
    workFn(data);
    durations.push(performance.now() - start);
  }
  return calcStats(label, durations);
}

function fmt(ms: number): string {
  return ms < 0.001 ? '<0.001' : ms.toFixed(3);
}

function printComparison(scenario: string, baseline: BenchmarkStats, sanitized: BenchmarkStats): void {
  const overheadMedian = sanitized.median - baseline.median;
  const overheadMean = sanitized.mean - baseline.mean;
  const overheadPct = baseline.median > 0.001 ? (overheadMedian / baseline.median) * 100 : NaN;

  console.log('');
  console.log(`=== ${scenario} (${ITERATIONS} iterations, ${WARMUP} warmup) ===`);
  console.log(`${''.padEnd(12)} | ${'median'.padStart(10)} | ${'mean'.padStart(10)} | ${'p95'.padStart(10)} | ${'stddev'.padStart(10)} | ${'min'.padStart(8)} | ${'max'.padStart(8)}`);
  console.log(`${'─'.repeat(12)}-+-${'─'.repeat(10)}-+-${'─'.repeat(10)}-+-${'─'.repeat(10)}-+-${'─'.repeat(10)}-+-${'─'.repeat(8)}-+-${'─'.repeat(8)}`);
  console.log(`${'baseline'.padEnd(12)} | ${fmt(baseline.median).padStart(10)} | ${fmt(baseline.mean).padStart(10)} | ${fmt(baseline.p95).padStart(10)} | ${fmt(baseline.stddev).padStart(10)} | ${fmt(baseline.min).padStart(8)} | ${fmt(baseline.max).padStart(8)}`);
  console.log(`${'sanitized'.padEnd(12)} | ${fmt(sanitized.median).padStart(10)} | ${fmt(sanitized.mean).padStart(10)} | ${fmt(sanitized.p95).padStart(10)} | ${fmt(sanitized.stddev).padStart(10)} | ${fmt(sanitized.min).padStart(8)} | ${fmt(sanitized.max).padStart(8)}`);
  console.log(`${'overhead'.padEnd(12)} | ${('+' + fmt(overheadMedian)).padStart(10)} | ${('+' + fmt(overheadMean)).padStart(10)} |            |            |          |`);
  if (!isNaN(overheadPct)) {
    console.log(`${'overhead %'.padEnd(12)} | ${(overheadPct >= 0 ? '+' : '') + overheadPct.toFixed(1) + '%'} |`);
  }
  console.log('');
}

// --- Build WS message from real data entries ---

interface RealEntry {
  entityId: string;
  ts: string;
  value: string;
}

function buildSubscriptionUpdateFromReal(entries: RealEntry[]): any {
  const data: any = {};
  entries.forEach((e, i) => {
    data[`key_${i}`] = [[Number(e.ts) || Date.now(), e.value]];
  });
  return { subscriptionId: 1, errorCode: 0, errorMsg: '', data };
}

function buildEntityDataUpdateFromReal(entries: RealEntry[]): any {
  const entities: any[] = [];
  for (let i = 0; i < entries.length; i += 5) {
    const chunk = entries.slice(i, i + 5);
    const latest: any = {};
    chunk.forEach((e, j) => {
      latest[`attr_${j}`] = { ts: Number(e.ts) || Date.now(), value: e.value };
    });
    entities.push({
      entityId: { id: chunk[0].entityId, entityType: 'DEVICE' },
      latest: { ATTRIBUTE: latest },
      timeseries: {},
    });
  }
  return { cmdId: 1, cmdUpdateType: 'ENTITY_DATA', data: { data: entities } };
}

// --- Tests ---

describe('Sanitization Performance Benchmark', { timeout: 60000 }, () => {

  let sanitizer: DomSanitizer;

  beforeAll(() => {
    console.warn = vi.fn();
  });

  afterAll(() => {
    vi.restoreAllMocks();
  });

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [BrowserModule] });
    sanitizer = TestBed.inject(DomSanitizer);
  });

  // =============================================
  // SECTION 1: Generated mock data benchmarks
  // =============================================

  describe('Generated data', () => {

    describe('SubscriptionUpdateMsg', () => {
      it('1000 keys, clean (0% HTML)', () => {
        const template = generateSubscriptionUpdate(1000, 0);
        const baseline = runBenchmark('baseline', () => deepClone(template), () => {});
        const sanitized = runBenchmark('sanitized', () => deepClone(template), (msg) => sanitizeWsMessage(sanitizer, msg));
        printComparison('SubscriptionUpdate 1000 keys (0% HTML)', baseline, sanitized);
        expect(sanitized.median).toBeLessThan(10);
      });

      it('1000 keys, 5% HTML (realistic attack)', () => {
        const template = generateSubscriptionUpdate(1000, 5);
        const baseline = runBenchmark('baseline', () => deepClone(template), () => {});
        const sanitized = runBenchmark('sanitized', () => deepClone(template), (msg) => sanitizeWsMessage(sanitizer, msg));
        printComparison('SubscriptionUpdate 1000 keys (5% HTML)', baseline, sanitized);
        expect(sanitized.median).toBeLessThan(50);
      });

      it('200 keys, 100% HTML (worst case)', () => {
        const template = generateSubscriptionUpdate(200, 100);
        const baseline = runBenchmark('baseline', () => deepClone(template), () => {});
        const sanitized = runBenchmark('sanitized', () => deepClone(template), (msg) => sanitizeWsMessage(sanitizer, msg));
        printComparison('SubscriptionUpdate 200 keys (100% HTML)', baseline, sanitized);
        expect(sanitized.median).toBeLessThan(500);
      });
    });

    describe('EntityDataUpdateMsg', () => {
      it('200 entities x 5 attrs, clean', () => {
        const template = generateEntityDataUpdate(200, 5, 0, 0);
        const baseline = runBenchmark('baseline', () => deepClone(template), () => {});
        const sanitized = runBenchmark('sanitized', () => deepClone(template), (msg) => sanitizeWsMessage(sanitizer, msg));
        printComparison('EntityDataUpdate 200×5 attrs (0% HTML)', baseline, sanitized);
        expect(sanitized.median).toBeLessThan(10);
      });

      it('500 entities x 10 attrs, clean (stress)', () => {
        const template = generateEntityDataUpdate(500, 10, 0, 0);
        const baseline = runBenchmark('baseline', () => deepClone(template), () => {});
        const sanitized = runBenchmark('sanitized', () => deepClone(template), (msg) => sanitizeWsMessage(sanitizer, msg));
        printComparison('EntityDataUpdate 500×10 attrs (0% HTML, stress)', baseline, sanitized);
        expect(sanitized.median).toBeLessThan(30);
      });
    });

    describe('AlarmDataUpdateMsg', () => {
      it('100 alarms, clean', () => {
        const template = generateAlarmDataUpdate(100, 0);
        const baseline = runBenchmark('baseline', () => deepClone(template), () => {});
        const sanitized = runBenchmark('sanitized', () => deepClone(template), (msg) => sanitizeWsMessage(sanitizer, msg));
        printComparison('AlarmDataUpdate 100 alarms (0% HTML)', baseline, sanitized);
        expect(sanitized.median).toBeLessThan(10);
      });
    });

    describe('NotificationsUpdateMsg', () => {
      it('50 notifications, clean', () => {
        const template = generateNotificationsUpdate(50, 0);
        const baseline = runBenchmark('baseline', () => deepClone(template), () => {});
        const sanitized = runBenchmark('sanitized', () => deepClone(template), (msg) => sanitizeWsMessage(sanitizer, msg));
        printComparison('NotificationsUpdate 50 (0% HTML)', baseline, sanitized);
        expect(sanitized.median).toBeLessThan(5);
      });
    });
  });

  // =============================================
  // SECTION 2: Real production data benchmarks
  // =============================================

  describe('Real production data', () => {

    it('SubscriptionUpdate — 2000 clean values from production', () => {
      const template = buildSubscriptionUpdateFromReal(realData.cleanSample);
      const baseline = runBenchmark('baseline', () => deepClone(template), () => {});
      const sanitized = runBenchmark('sanitized', () => deepClone(template), (msg) => sanitizeWsMessage(sanitizer, msg));
      printComparison(`Real SubscriptionUpdate (${realData.cleanSample.length} clean values)`, baseline, sanitized);
      expect(sanitized.median).toBeLessThan(10);
    });

    it('SubscriptionUpdate — 200 HTML-like values from production', () => {
      const template = buildSubscriptionUpdateFromReal(realData.htmlSample);
      const baseline = runBenchmark('baseline', () => deepClone(template), () => {});
      const sanitized = runBenchmark('sanitized', () => deepClone(template), (msg) => sanitizeWsMessage(sanitizer, msg));
      printComparison(`Real SubscriptionUpdate (${realData.htmlSample.length} HTML-like values)`, baseline, sanitized);
      expect(sanitized.median).toBeLessThan(500);
    });

    it('SubscriptionUpdate — 100 large values from production', () => {
      const template = buildSubscriptionUpdateFromReal(realData.largeSample);
      const baseline = runBenchmark('baseline', () => deepClone(template), () => {});
      const sanitized = runBenchmark('sanitized', () => deepClone(template), (msg) => sanitizeWsMessage(sanitizer, msg));
      printComparison(`Real SubscriptionUpdate (${realData.largeSample.length} large values)`, baseline, sanitized);
      expect(sanitized.median).toBeLessThan(500);
    });

    it('EntityDataUpdate — 400 entities from clean production data', () => {
      const template = buildEntityDataUpdateFromReal(realData.cleanSample);
      const baseline = runBenchmark('baseline', () => deepClone(template), () => {});
      const sanitized = runBenchmark('sanitized', () => deepClone(template), (msg) => sanitizeWsMessage(sanitizer, msg));
      printComparison(`Real EntityDataUpdate (${Math.ceil(realData.cleanSample.length / 5)} entities, clean)`, baseline, sanitized);
      expect(sanitized.median).toBeLessThan(10);
    });

    it('EntityDataUpdate — entities with HTML-like values', () => {
      const template = buildEntityDataUpdateFromReal(realData.htmlSample);
      const baseline = runBenchmark('baseline', () => deepClone(template), () => {});
      const sanitized = runBenchmark('sanitized', () => deepClone(template), (msg) => sanitizeWsMessage(sanitizer, msg));
      printComparison(`Real EntityDataUpdate (${Math.ceil(realData.htmlSample.length / 5)} entities, HTML)`, baseline, sanitized);
      expect(sanitized.median).toBeLessThan(500);
    });
  });

  // =============================================
  // SECTION 3: Change detection on real data
  // =============================================

  describe('Change detection on real production data', () => {

    it('should report which clean values change after sanitization', () => {
      const changes: Array<{ index: number; before: string; after: string }> = [];
      realData.cleanSample.forEach((entry: RealEntry, i: number) => {
        const after = sanitizeStringValue(sanitizer, entry.value);
        if (after !== entry.value) {
          changes.push({ index: i, before: entry.value.substring(0, 120), after: after.substring(0, 120) });
        }
      });

      console.log('');
      console.log(`=== Change detection: clean sample (${realData.cleanSample.length} values) ===`);
      console.log(`  Changed: ${changes.length} / ${realData.cleanSample.length}`);
      if (changes.length > 0) {
        console.log('  First 10 changes:');
        for (const c of changes.slice(0, 10)) {
          console.log(`    [${c.index}] BEFORE: ${c.before}`);
          console.log(`         AFTER:  ${c.after}`);
        }
      } else {
        console.log('  No changes — all values pass through unchanged');
      }
      console.log('');

      // Clean data should ideally have 0 changes
      // but some values may contain accidental HTML-like patterns
      expect(changes.length).toBeLessThan(realData.cleanSample.length * 0.01);
    });

    it('should report which HTML-like values change after sanitization', () => {
      const changes: Array<{ before: string; after: string; containsHtml: boolean }> = [];
      const unchanged: string[] = [];

      realData.htmlSample.forEach((entry: RealEntry) => {
        const before = entry.value;
        const after = sanitizeStringValue(sanitizer, before);
        if (after !== before) {
          changes.push({
            before: before.substring(0, 150),
            after: after.substring(0, 150),
            containsHtml: containsHtmlLike(before),
          });
        } else {
          unchanged.push(before.substring(0, 100));
        }
      });

      console.log('');
      console.log(`=== Change detection: HTML-like sample (${realData.htmlSample.length} values) ===`);
      console.log(`  Changed:   ${changes.length}`);
      console.log(`  Unchanged: ${unchanged.length}`);

      if (changes.length > 0) {
        console.log('');
        console.log('  Changed values (first 15):');
        for (const c of changes.slice(0, 15)) {
          console.log(`    BEFORE: ${c.before}`);
          console.log(`    AFTER:  ${c.after}`);
          console.log('');
        }
      }

      if (unchanged.length > 0) {
        console.log('  Unchanged values with "<" (first 10):');
        for (const u of unchanged.slice(0, 10)) {
          console.log(`    ${u}`);
        }
      }
      console.log('');

      expect(changes.length).toBeGreaterThan(0);
    });

    it('should report which large values change after sanitization', () => {
      const changes: Array<{ lengthBefore: number; lengthAfter: number; preview: string }> = [];

      realData.largeSample.forEach((entry: RealEntry) => {
        const after = sanitizeStringValue(sanitizer, entry.value);
        if (after !== entry.value) {
          changes.push({
            lengthBefore: entry.value.length,
            lengthAfter: after.length,
            preview: entry.value.substring(0, 100),
          });
        }
      });

      console.log('');
      console.log(`=== Change detection: large values (${realData.largeSample.length} values) ===`);
      console.log(`  Changed: ${changes.length} / ${realData.largeSample.length}`);

      if (changes.length > 0) {
        console.log('  Changes (first 10):');
        for (const c of changes.slice(0, 10)) {
          console.log(`    len ${c.lengthBefore} → ${c.lengthAfter} (${c.lengthAfter > c.lengthBefore ? '+' : ''}${c.lengthAfter - c.lengthBefore}) | ${c.preview}`);
        }
      }
      console.log('');
    });

    it('should show overall statistics of containsHtmlLike gate', () => {
      const allSamples = [...realData.cleanSample, ...realData.htmlSample, ...realData.largeSample];
      let gateTriggered = 0;
      let actuallyChanged = 0;

      for (const entry of allSamples) {
        const val = (entry as RealEntry).value;
        if (containsHtmlLike(val)) {
          gateTriggered++;
          const after = sanitizeStringValue(sanitizer, val);
          if (after !== val) {
            actuallyChanged++;
          }
        }
      }

      console.log('');
      console.log(`=== containsHtmlLike gate statistics (${allSamples.length} total values) ===`);
      console.log(`  Gate triggered (regex matched):  ${gateTriggered} (${(gateTriggered / allSamples.length * 100).toFixed(2)}%)`);
      console.log(`  Actually changed by sanitizer:   ${actuallyChanged} (${(actuallyChanged / allSamples.length * 100).toFixed(2)}%)`);
      console.log(`  False positives (gate yes, no change): ${gateTriggered - actuallyChanged}`);
      console.log(`  Values skipped by fast gate:     ${allSamples.length - gateTriggered} (${((allSamples.length - gateTriggered) / allSamples.length * 100).toFixed(2)}%)`);
      console.log('');
    });
  });
});
