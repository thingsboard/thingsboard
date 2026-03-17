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
import { sanitizeStringValue, containsHtmlLike } from './sanitize.utils';
import realData from './__fixtures__/real-telemetry-data.json';
import { writeFileSync } from 'fs';
import { resolve } from 'path';

interface RealEntry {
  entityId: string;
  ts: string;
  value: string;
}

function escapeCsvField(val: string): string {
  if (val.includes('"') || val.includes(',') || val.includes('\n') || val.includes('\r')) {
    return '"' + val.replace(/"/g, '""') + '"';
  }
  return val;
}

describe('Generate sanitize report CSV', () => {

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

  it('should generate CSV with changed values', () => {
    const allEntries: RealEntry[] = [
      ...realData.cleanSample,
      ...realData.htmlSample,
      ...realData.largeSample,
    ];

    const rows: string[] = [];
    rows.push(['entityId', 'timestamp', 'original_value', 'sanitized_value', 'value_length_before', 'value_length_after', 'length_diff', 'containsHtmlLike'].join(','));

    let changedCount = 0;

    for (const entry of allEntries) {
      const original = entry.value;
      const sanitized = sanitizeStringValue(sanitizer, original);

      if (sanitized !== original) {
        changedCount++;
        rows.push([
          escapeCsvField(entry.entityId),
          escapeCsvField(entry.ts),
          escapeCsvField(original),
          escapeCsvField(sanitized),
          String(original.length),
          String(sanitized.length),
          String(sanitized.length - original.length),
          String(containsHtmlLike(original)),
        ].join(','));
      }
    }

    const outputPath = resolve(__dirname, '../../../../..', 'sanitize-changes-report.csv');
    writeFileSync(outputPath, rows.join('\n'), 'utf-8');

    console.log('');
    console.log(`=== Sanitize Changes Report ===`);
    console.log(`  Total values analyzed: ${allEntries.length}`);
    console.log(`  Values changed:        ${changedCount}`);
    console.log(`  Values unchanged:      ${allEntries.length - changedCount}`);
    console.log(`  Report saved to:       ${outputPath}`);
    console.log('');

    expect(changedCount).toBeGreaterThan(0);
  });
});
