// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';

export const SHOW_RECORDS_TOOL = 'show_records';

export const MAX_SHOWN_RECORDS = 20;

export interface AiRecordState {
  tone: 'ok' | 'off' | 'warn' | 'wait';
  label: string;
}

export interface AiAlarmState {
  severity: string;
  cleared: boolean;
  acknowledged: boolean;
  tip: string;
}

export interface AiHubMeta {
  type: string;
  creator?: string;
  verified?: boolean;
  installs?: string;
}

export interface AiRecordFact {
  label: string;
  value: string;
}

export interface AiRecordView {
  key: string;
  entityType: string;
  alarmRule: boolean;
  name: string;
  meta: string;
  url?: string;
  state?: AiRecordState;
  alarm?: AiAlarmState;
  hub?: AiHubMeta;
  hubItem?: MpItemVersionView;
  description?: string;
  image?: string;
  imageUrl?: string;
  widgets?: number;
  square?: boolean;
  wide?: boolean;
}

export type AiRecordRef = {versionId: string} | {entityType: string; id: string};

export interface AiShownRecords {
  refs: AiRecordRef[];
  facts: AiRecordFact[];
  note?: string;
}

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const text = (value: unknown): string | null => typeof value === 'string' && value.trim() ? value.trim() : null;

export function aiRecordKey(ref: AiRecordRef): string {
  return 'versionId' in ref ? `HUB:${ref.versionId}` : `${ref.entityType}:${ref.id}`;
}

export function aiShownRecordsOf(args: unknown): AiShownRecords | null {
  const given = (args as {records?: unknown})?.records;
  if (!Array.isArray(given)) {
    return null;
  }
  const seen = new Set<string>();
  const refs: AiRecordRef[] = [];
  given.forEach(item => {
    const versionId = text(item?.iotHubVersionId);
    const id = text(item?.id);
    const entityType = text(item?.entityType)?.toUpperCase();
    const ref: AiRecordRef | null = versionId && UUID.test(versionId) ? {versionId}
      : id && entityType && UUID.test(id) ? {entityType: entityType === 'ALARM_RULE' ? 'CALCULATED_FIELD' : entityType, id} : null;
    if (ref && !seen.has(aiRecordKey(ref))) {
      seen.add(aiRecordKey(ref));
      refs.push(ref);
    }
  });
  if (!refs.length) {
    return null;
  }
  const single = refs.length === 1;
  const rawFacts = (args as {facts?: unknown})?.facts;
  const facts = single && Array.isArray(rawFacts)
    ? rawFacts.map(fact => ({label: text(fact?.label), value: text(fact?.value)}))
      .filter(fact => fact.label && fact.value).slice(0, 4) as AiRecordFact[]
    : [];
  const note = single ? text((args as {note?: unknown})?.note) ?? undefined : undefined;
  return {refs: refs.slice(0, MAX_SHOWN_RECORDS), facts, note};
}

export function aiFactParts(value: string): Array<{text: string; code: boolean}> {
  return value.split(/(`[^`]+`)/).filter(part => part.length)
    .map(part => part.length > 2 && part.startsWith('`') && part.endsWith('`') ? {text: part.slice(1, -1), code: true} : {text: part, code: false});
}
