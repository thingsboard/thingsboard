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

import { normalizeLinkLabel, toStandardizedLinkLabel, toStandardizedLinkLabels } from './rule-node.models';

describe('toStandardizedLinkLabel', () => {
  it('preserves an all-caps acronym and lowercases connector words', () => {
    expect(toStandardizedLinkLabel('RPC Request from Device')).toBe('RPC Request from Device');
  });

  it('capitalizes every word except connector words, even when the source is all lowercase', () => {
    expect(toStandardizedLinkLabel('entity assigned to tenant')).toBe('Entity Assigned to Tenant');
  });

  it('title-cases a mixed-case custom label, not preserving internal capitalization', () => {
    expect(toStandardizedLinkLabel('the IoT gateway')).toBe('The Iot Gateway');
  });

  it('returns an empty string for an empty label', () => {
    expect(toStandardizedLinkLabel('')).toBe('');
  });

  it('returns an empty string instead of throwing for a null or undefined label', () => {
    expect(toStandardizedLinkLabel(undefined)).toBe('');
    expect(toStandardizedLinkLabel(null)).toBe('');
  });
});

describe('normalizeLinkLabel', () => {
  it('standardizes the label name while leaving the value untouched', () => {
    expect(normalizeLinkLabel({name: 'entity assigned to tenant', value: 'entity assigned to tenant'}))
      .toEqual({name: 'Entity Assigned to Tenant', value: 'entity assigned to tenant'});
  });
});

describe('toStandardizedLinkLabels', () => {
  const allowedLabels = {
    success: {name: 'Success', value: 'success'},
    'post telemetry': {name: 'Post Telemetry', value: 'post telemetry'}
  };

  it('standardizes only the labels that are well-known and renders custom labels verbatim', () => {
    expect(toStandardizedLinkLabels(['post telemetry', 'checkTemperature'], allowedLabels))
      .toBe('Post Telemetry / checkTemperature');
  });

  it('renders every label verbatim when none are in the allowed set', () => {
    expect(toStandardizedLinkLabels(['gRPC', 'IoT data'], allowedLabels)).toBe('gRPC / IoT data');
  });

  it('treats every label as custom when allowedLabels is undefined', () => {
    expect(toStandardizedLinkLabels(['success'], undefined)).toBe('success');
  });

  it('returns an empty string for an empty labels array', () => {
    expect(toStandardizedLinkLabels([], allowedLabels)).toBe('');
  });
});
