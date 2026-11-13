// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AiAction } from '@shared/models/ai-chat.models';

export interface AiDetailEntity {
  id?: string;
  entityType?: string;
  name?: string;
}

export type AiDetailParam = string | number | AiDetailEntity;

export interface AiDetailText {
  key: string;
  params?: Record<string, AiDetailParam>;
}

export interface AiDetailRecord {
  entityType: string;
  groupType?: string;
  id?: string;
  name: string;
  meta?: string;
  severity?: string;
}

export interface AiDetailHubItem {
  id: string;
  name: string;
  vendor?: string;
  description?: string;
}

export interface AiDetailValue {
  key: string;
  value: string;
  time?: number;
}

export interface AiDetailSeries {
  key: string;
  points: number;
  min: number;
  max: number;
  last: number;
  from: number;
  to: number;
  path: string;
}

export interface AiDetailFact {
  label: string;
  raw?: boolean;
  value?: string;
  entity?: AiDetailEntity;
  mono?: boolean;
  secret?: boolean;
}

export interface AiDetailCommand {
  label: string;
  text: string;
}

export interface AiDetailLink {
  title: string;
  url: string;
  from: string;
  snippet?: string;
}

export type AiDetailBlock =
  | {type: 'records'; label?: AiDetailText; rows: AiDetailRecord[]; total?: number}
  | {type: 'hub'; rows: AiDetailHubItem[]}
  | {type: 'values'; label?: AiDetailText; rows: AiDetailValue[]}
  | {type: 'series'; rows: AiDetailSeries[]}
  | {type: 'keys'; label?: AiDetailText; keys: string[]}
  | {type: 'facts'; rows: AiDetailFact[]}
  | {type: 'list'; label?: AiDetailText; rows: string[]}
  | {type: 'commands'; rows: AiDetailCommand[]}
  | {type: 'links'; rows: AiDetailLink[]}
  | {type: 'markdown'; text: string}
  | {type: 'code'; label: string; text: string}
  | {type: 'note'; text: AiDetailText}
  | {type: 'text'; text: string}
  | {type: 'json'; label: string; text: string};

export interface AiDetailView {
  hood?: AiDetailText;
  blocks: AiDetailBlock[];
}

export type AiDetailNames = ReadonlyMap<string, AiDetailEntity>;

const K = 'ai-assistant.detail.';

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const OK_ID = /^OK:\s*([0-9a-z_-]+)/i;

const MAX_ROWS = 20;

const SCOPES: Record<string, string> = {
  SERVER_SCOPE: 'server',
  SHARED_SCOPE: 'shared',
  CLIENT_SCOPE: 'client'
};

const SEARCHED: Record<string, string> = {
  DEVICE: 'devices',
  ASSET: 'assets',
  ENTITY_VIEW: 'entity-views',
  EDGE: 'edges',
  CUSTOMER: 'customers',
  USER: 'users',
  DASHBOARD: 'dashboards',
  TENANT: 'tenants',
  DEVICE_PROFILE: 'device-profiles',
  ASSET_PROFILE: 'asset-profiles',
  ENTITY_GROUP: 'groups'
};

const FILTER_TYPES: Record<string, string> = {
  deviceType: 'DEVICE',
  assetType: 'ASSET',
  entityViewType: 'ENTITY_VIEW',
  edgeType: 'EDGE',
  gateway: 'DEVICE',
  alarm: 'ALARM',
  alarmRule: 'ALARM_RULE',
  calculatedField: 'CALCULATED_FIELD',
  notificationTarget: 'NOTIFICATION_TARGET',
  notificationTemplate: 'NOTIFICATION_TEMPLATE',
  notificationRule: 'NOTIFICATION_RULE',
  image: 'IMAGE',
  entityGroupName: 'ENTITY_GROUP'
};

const FILTER_WORDS: Record<string, string> = {
  gateway: 'gateways',
  alarm: 'alarms',
  alarmRule: 'alarm-rules',
  calculatedField: 'calculated-fields',
  notificationTarget: 'notification-targets',
  notificationTemplate: 'notification-templates',
  notificationRule: 'notification-rules',
  image: 'images'
};

const DELETE_TYPES: Record<string, string> = {
  DASHBOARD_WIDGET: 'DASHBOARD_WIDGET',
  DASHBOARD_STATE: 'DASHBOARD_STATE',
  DASHBOARD_ENTITY_ALIAS: 'DASHBOARD_ENTITY_ALIAS',
  DASHBOARD_FILTER: 'DASHBOARD_FILTER'
};

type Values = Record<string, any>;

export function aiParse(value: unknown): unknown {
  if (typeof value !== 'string') {
    return value ?? undefined;
  }
  const text = value.trim();
  if (!text.startsWith('{') && !text.startsWith('[')) {
    return value;
  }
  try {
    return JSON.parse(text);
  } catch {
    return value;
  }
}

const isObject = (value: unknown): value is Values => !!value && typeof value === 'object' && !Array.isArray(value);

const str = (value: unknown): string | undefined => typeof value === 'string' && value.trim() ? value.trim() : undefined;

const idOf = (value: unknown): string | undefined => typeof value === 'string' ? value : isObject(value) ? str(value.id) : undefined;

const okId = (result: unknown): string | undefined => typeof result === 'string' ? OK_ID.exec(result.trim())?.[1] : undefined;

export function aiValueText(value: unknown): string {
  if (value === null || value === undefined) {
    return '—';
  }
  if (typeof value === 'number') {
    return Number.isInteger(value) ? String(value) : String(Math.round(value * 1000) / 1000);
  }
  if (typeof value === 'string') {
    const parsed = aiParse(value);
    return parsed !== value ? aiValueText(parsed) : value.length > 120 ? value.slice(0, 120) + '…' : value;
  }
  if (typeof value === 'boolean') {
    return String(value);
  }
  const json = JSON.stringify(value);
  return json.length > 120 ? json.slice(0, 120) + '…' : json;
}

export const aiHumanize = (key: string): string => {
  const words = key.replace(/([a-z0-9])([A-Z])/g, '$1 $2').replace(/[_-]+/g, ' ').trim().toLowerCase();
  return words ? words.charAt(0).toUpperCase() + words.slice(1) : key;
};

function entity(id: unknown, entityType: unknown, names: AiDetailNames): AiDetailEntity {
  const key = idOf(id);
  const type = str(entityType) ?? (isObject(id) ? str(id.entityType) : undefined);
  const known = key ? names.get(key) : undefined;
  return {id: key, entityType: type ?? known?.entityType, name: known?.name};
}

function text(key: string, params?: Record<string, AiDetailParam>): AiDetailText {
  return {key: K + key, params};
}

function facts(values: Values, skip: string[] = [], names?: AiDetailNames): AiDetailFact[] {
  const rows: AiDetailFact[] = [];
  for (const [key, value] of Object.entries(values)) {
    if (skip.includes(key) || value === null || value === undefined || value === '') {
      continue;
    }
    if (names && /Id$/.test(key) && (typeof value === 'string' || isObject(value)) && idOf(value)) {
      const ref = entity(value, isObject(value) ? value.entityType : undefined, names);
      rows.push(ref.name ? {label: aiHumanize(key.replace(/Id$/, '')), raw: true, entity: ref} : {label: aiHumanize(key), raw: true, value: idOf(value), mono: true});
      continue;
    }
    if (Array.isArray(value) && value.every(item => typeof item !== 'object')) {
      rows.push({label: aiHumanize(key), raw: true, value: value.map(aiValueText).join(', ')});
    } else if (typeof value === 'object') {
      rows.push({label: aiHumanize(key), raw: true, value: aiValueText(value), mono: true});
    } else {
      rows.push({label: aiHumanize(key), raw: true, value: aiValueText(value)});
    }
  }
  return rows;
}

function flat(value: unknown, depth = 0): boolean {
  if (!isObject(value)) {
    return false;
  }
  return Object.values(value).every(item => item === null || typeof item !== 'object'
    || (depth < 1 && (isObject(item) ? flat(item, depth + 1) : (item as unknown[]).every(entry => typeof entry !== 'object'))));
}

function json(label: string, value: unknown): AiDetailBlock[] {
  if (value === undefined || value === null || value === '') {
    return [];
  }
  if (typeof value === 'string') {
    return [{type: 'text', text: value}];
  }
  return [{type: 'json', label, text: JSON.stringify(value, null, 2)}];
}

function generic(value: unknown, label: string, skip: string[] = [], names?: AiDetailNames): AiDetailBlock[] {
  if (typeof value === 'string') {
    const trimmed = value.trim();
    return !trimmed || /^OK\.?$/i.test(trimmed) ? [] : [{type: 'text', text: trimmed.replace(/^OK:\s*/i, '')}];
  }
  if (isObject(value) && Object.keys(value).length <= 16 && flat(value)) {
    const rows = facts(value, skip, names);
    return rows.length ? [{type: 'facts', rows}] : [];
  }
  return json(label, value);
}

function timeOf(ts: unknown): number | undefined {
  const value = typeof ts === 'string' ? Number(ts) : ts;
  return typeof value === 'number' && Number.isFinite(value) && value > 1e11 ? value : undefined;
}

function series(key: string, points: Array<[number, number]>): AiDetailSeries {
  const sorted = [...points].sort((a, b) => a[0] - b[0]);
  const values = sorted.map(point => point[1]);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const from = sorted[0][0];
  const to = sorted[sorted.length - 1][0];
  const width = 96;
  const height = 20;
  const span = to - from || 1;
  const range = max - min || 1;
  const path = sorted.map(([ts, value], index) =>
    `${index ? 'L' : 'M'}${((ts - from) / span * width).toFixed(1)} ${(height - 1 - (value - min) / range * (height - 2)).toFixed(1)}`).join('');
  return {key, points: sorted.length, min, max, last: sorted[sorted.length - 1][1], from, to, path};
}

function telemetry(result: Values): AiDetailBlock[] {
  const latest: AiDetailValue[] = [];
  const history: AiDetailSeries[] = [];
  const other: AiDetailValue[] = [];
  for (const [key, data] of Object.entries(result)) {
    if (isObject(data) && 'value' in data) {
      latest.push({key, value: aiValueText(data.value), time: timeOf(data.ts)});
      continue;
    }
    if (Array.isArray(data) && data.every(point => isObject(point) && 'ts' in point)) {
      const numbers = data.map(point => [timeOf(point.ts), Number(point.value)] as [number, number]).filter(([ts, value]) => ts && Number.isFinite(value));
      if (numbers.length > 1) {
        history.push(series(key, numbers));
      } else if (data.length) {
        latest.push({key, value: aiValueText(data[0].value), time: timeOf(data[0].ts)});
      }
      continue;
    }
    if (!isObject(data)) {
      other.push({key, value: aiValueText(data)});
      continue;
    }
    const entries = Object.entries(data).filter(([ts]) => timeOf(ts));
    const numbers = entries.map(([ts, value]) => [timeOf(ts), Number(value)] as [number, number]).filter(([, value]) => Number.isFinite(value));
    if (entries.length > 1 && numbers.length === entries.length) {
      history.push(series(key, numbers));
    } else if (entries.length) {
      const [ts, value] = entries.sort((a, b) => Number(b[0]) - Number(a[0]))[0];
      latest.push({key, value: aiValueText(value), time: timeOf(ts)});
    } else {
      other.push({key, value: aiValueText(data)});
    }
  }
  const blocks: AiDetailBlock[] = [];
  if (history.length) {
    blocks.push({type: 'series', rows: history});
  }
  if (latest.length || other.length) {
    blocks.push({type: 'values', rows: [...latest, ...other]});
  }
  return blocks;
}

function searchSections(result: unknown): Array<{entityType?: string; page: Values}> {
  if (isObject(result)) {
    return [{page: result}];
  }
  if (typeof result !== 'string') {
    return [];
  }
  const sections: Array<{entityType?: string; page: Values}> = [];
  const pattern = /(?:^|\n\n)([A-Z_]+) entities[^\n]*:\n(\{[\s\S]*?\})(?=\n\n[A-Z_]+ entities|$)/g;
  let match: RegExpExecArray | null;
  while ((match = pattern.exec(result)) !== null) {
    const page = aiParse(match[2]);
    if (isObject(page)) {
      sections.push({entityType: match[1], page});
    }
  }
  return sections;
}

function searchType(values: Values): string | undefined {
  const filter = isObject(values.entityFilter) ? values.entityFilter : {};
  return FILTER_TYPES[values.filterType] ?? str(filter.entityType) ?? str(filter.singleEntity?.entityType) ?? str(filter.groupType && 'ENTITY_GROUP');
}

function alarmMeta(row: Values): string {
  return [row.severity, row.status, timeOf(row.startTs) ? new Date(timeOf(row.startTs)).toLocaleString() : undefined]
    .filter(Boolean).map(String).join(' · ');
}

function recordOf(row: Values, entityType: string): AiDetailRecord | null {
  if (entityType === 'ALARM') {
    const alarmName = str(row.type) ?? str(row.name);
    return alarmName ? {entityType, id: idOf(row.id), name: alarmName, meta: alarmMeta(row), severity: str(row.severity)} : null;
  }
  const name = str(row.name) ?? str(row.title) ?? str(row.email) ?? str(row.label);
  if (!name) {
    return null;
  }
  const meta = [row.label && row.label !== name ? row.label : undefined, entityType === 'ALARM_RULE' ? undefined : row.type, row.email && row.email !== name ? row.email : undefined]
    .filter(value => typeof value === 'string' && value.trim()).join(' · ');
  return {entityType: entityType === 'ALARM_RULE' ? 'ALARM_RULE' : entityType, id: idOf(row.id), name, meta: meta || undefined};
}

function searchEntities(values: Values, result: unknown, names: AiDetailNames): AiDetailView {
  const filter = isObject(values.entityFilter) ? values.entityFilter : {};
  const type = searchType(values);
  const what = FILTER_WORDS[values.filterType] ?? (type && SEARCHED[type]) ?? (values.filterType === 'entityType' && !filter.entityType ? 'devices-assets' : 'entities');
  const params: Record<string, AiDetailParam> = {what: K + 'what.' + what};
  let key = 'search';
  if (values.filterType === 'singleEntity' && filter.singleEntity) {
    key = 'search-one';
    params.entity = entity(filter.singleEntity.id, filter.singleEntity.entityType, names);
  } else if (values.filterType === 'relationsQuery' && filter.rootEntity) {
    key = filter.direction === 'TO' ? 'search-related-to' : 'search-related-from';
    params.entity = entity(filter.rootEntity.id, filter.rootEntity.entityType, names);
  } else if (str(filter.nameFilter)) {
    key = 'search-named';
    params.name = filter.nameFilter.replace(/%/g, '');
  } else if (Array.isArray(filter.types) && filter.types.length) {
    key = 'search-typed';
    params.types = filter.types.join(', ');
  } else if (str(values.pageLink?.textSearch)) {
    key = 'search-text';
    params.text = values.pageLink.textSearch;
  } else if (values.filterType === 'alarm' && Array.isArray(filter.originatorIds) && filter.originatorIds.length === 1) {
    key = 'search-alarms-of';
    params.entity = entity(filter.originatorIds[0], filter.originatorType, names);
  }
  const sections = searchSections(result);
  if (!sections.length) {
    return {hood: text(key, params), blocks: generic(result, 'result')};
  }
  const blocks: AiDetailBlock[] = [];
  for (const section of sections) {
    const entityType = section.entityType ?? type ?? 'DEVICE';
    const data: unknown[] = Array.isArray(section.page.data) ? section.page.data : [];
    if (!data.length) {
      if (sections.length === 1) {
        blocks.push({type: 'note', text: text('nothing-found')});
      }
      continue;
    }
    const groupType = entityType === 'ENTITY_GROUP' ? str(filter.groupType)?.toUpperCase() : undefined;
    const rows = data.filter(isObject).map(row => recordOf(row, entityType)).filter(Boolean).slice(0, MAX_ROWS)
      .map(row => groupType ? {...row, groupType} : row);
    const total = typeof section.page.totalElements === 'number' ? section.page.totalElements : data.length;
    blocks.push(rows.length
      ? {type: 'records', label: sections.length > 1 ? text('what-found.' + (SEARCHED[entityType] ?? 'entities')) : undefined, rows, total}
      : {type: 'json', label: 'result', text: JSON.stringify(data, null, 2)});
  }
  return {hood: text(key, params), blocks};
}

function relations(result: unknown): AiDetailBlock[] {
  if (!isObject(result)) {
    return generic(result, 'result');
  }
  const blocks: AiDetailBlock[] = [];
  for (const direction of ['from', 'to']) {
    const byType = result[direction];
    if (!isObject(byType)) {
      continue;
    }
    for (const [relationType, group] of Object.entries(byType)) {
      const related: unknown[] = Array.isArray(group?.relatedEntities) ? group.relatedEntities : [];
      const rows = related.filter(isObject).map(item => ({
        entityType: str(item.entityType) ?? 'DEVICE',
        id: idOf(item.entityId ?? item.id),
        name: str(item.name) ?? idOf(item.entityId ?? item.id) ?? '—'
      }));
      if (rows.length) {
        blocks.push({type: 'records', label: text('relation-' + direction, {type: relationType}), rows});
      }
    }
  }
  return blocks.length ? blocks : generic(result, 'result');
}

function attributes(result: unknown, label: 'values' | 'keys'): AiDetailBlock[] {
  if (!isObject(result)) {
    return generic(result, 'result');
  }
  const blocks: AiDetailBlock[] = [];
  for (const [scope, data] of Object.entries(result)) {
    const scopeLabel = text('scope.' + (SCOPES[scope] ?? 'other'));
    if (Array.isArray(data) && data.length) {
      blocks.push({type: 'keys', label: scopeLabel, keys: data.map(String)});
    } else if (isObject(data) && Object.keys(data).length) {
      blocks.push({
        type: 'values', label: scopeLabel, rows: Object.entries(data).map(([key, item]) => isObject(item) && 'value' in item
          ? {key, value: aiValueText(item.value), time: timeOf(item.ts)} : {key, value: aiValueText(item)})
      });
    }
  }
  return blocks.length ? blocks : [{type: 'note', text: text(label === 'keys' ? 'no-attribute-keys' : 'no-attributes')}];
}

const PROTOCOLS: Record<string, string> = {
  coap: 'CoAP',
  coaps: 'CoAPS',
  lwm2m: 'LwM2M',
  docker: 'Docker',
  windows: 'Windows',
  linux: 'Linux',
  macos: 'macOS'
};

function commands(value: unknown, prefix: string[] = [], rows: AiDetailCommand[] = []): AiDetailCommand[] {
  if (typeof value === 'string') {
    rows.push({label: prefix.map(part => PROTOCOLS[part.toLowerCase()] ?? (part.length <= 5 ? part.toUpperCase() : aiHumanize(part)))
      .filter((part, index, all) => all.indexOf(part) === index).join(' · '), text: value});
  } else if (isObject(value)) {
    Object.entries(value).forEach(([key, item]) => commands(item, [...prefix, key], rows));
  }
  return rows;
}

function webResults(result: unknown): AiDetailBlock[] {
  if (typeof result !== 'string' || !/^1\. /.test(result.trim())) {
    return generic(result, 'result');
  }
  const rows: AiDetailLink[] = [];
  for (const chunk of result.trim().split(/\n\n(?=\d+\. )/)) {
    const [head, url, ...rest] = chunk.split('\n');
    const title = head?.replace(/^\d+\.\s*/, '').trim();
    if (!title || !/^https?:\/\//.test(url ?? '')) {
      continue;
    }
    let from = url;
    try {
      from = new URL(url).hostname.replace(/^www\./, '');
    } catch {
    }
    rows.push({title, url: url.trim(), from, snippet: rest.join(' ').trim() || undefined});
  }
  return rows.length ? [{type: 'links', rows}] : generic(result, 'result');
}

function aliasFilter(filter: unknown, names: AiDetailNames): AiDetailFact[] {
  if (!isObject(filter)) {
    return [];
  }
  const rows: AiDetailFact[] = [{label: K + 'fact.resolves', value: aiHumanize(String(filter.type ?? ''))}];
  if (Array.isArray(filter.entityList)) {
    const list = filter.entityList.map((id: unknown) => names.get(String(id))?.name ?? String(id));
    rows.push({label: K + 'fact.entities', value: list.join(', ')});
  }
  for (const key of ['deviceTypes', 'assetTypes', 'entityViewTypes', 'deviceType', 'assetType', 'entityType', 'entityNameFilter', 'relationType', 'direction']) {
    if (filter[key] !== undefined && filter[key] !== '') {
      rows.push({label: aiHumanize(key), raw: true, value: Array.isArray(filter[key]) ? filter[key].join(', ') : aiValueText(filter[key])});
    }
  }
  const single = filter.singleEntity ?? filter.defaultStateEntity ?? filter.rootEntity;
  if (isObject(single)) {
    rows.push({label: K + 'fact.entity', entity: entity(single.id, single.entityType, names)});
  }
  rows.push({label: K + 'fact.multiple', value: filter.resolveMultiple ? K + 'yes' : K + 'no'});
  return rows;
}

function widgetFacts(widget: Values, names: AiDetailNames): AiDetailFact[] {
  const rows: AiDetailFact[] = [];
  if (str(widget.title)) {
    rows.push({label: K + 'fact.title', value: widget.title});
  }
  if (str(widget.widgetType)) {
    rows.push({label: K + 'fact.widget-type', value: aiHumanize(widget.widgetType)});
  }
  if (str(widget.stateId)) {
    rows.push({label: K + 'fact.state', value: widget.stateId, mono: true});
  }
  if (str(widget.entityAliasId)) {
    const alias = names.get(widget.entityAliasId)?.name;
    rows.push(alias ? {label: K + 'fact.alias', value: alias} : {label: K + 'fact.alias', value: widget.entityAliasId, mono: true});
  }
  const config = isObject(widget.widgetConfig) ? widget.widgetConfig : {};
  const keys = (Array.isArray(config.keys) ? config.keys : []).map((item: any) => typeof item === 'string' ? item : item?.key).filter(Boolean);
  ['latitudeKey', 'longitudeKey', 'valueKey', 'key'].forEach(field => str(config[field]) && keys.push(config[field]));
  if (keys.length) {
    rows.push({label: K + 'fact.keys', value: [...new Set(keys)].join(', '), mono: true});
  }
  if (widget.sizeX && widget.sizeY) {
    rows.push({label: K + 'fact.size', value: `${widget.sizeX} × ${widget.sizeY}` + (widget.col !== undefined ? ` at ${widget.col}, ${widget.row ?? 0}` : '')});
  }
  return rows;
}

function widgetRecords(widgets: unknown, result: unknown): AiDetailRecord[] {
  const ids = typeof result === 'string' ? [...result.matchAll(/widgets\[(\d+)\][^:]*:\s*([0-9a-f-]{36})/g)] : [];
  return (Array.isArray(widgets) ? widgets : []).filter(isObject).map((widget, index) => ({
    entityType: 'DASHBOARD_WIDGET',
    id: ids.find(match => Number(match[1]) === index)?.[2],
    name: str(widget.title) ?? aiHumanize(String(widget.widgetType ?? 'widget')),
    meta: [widget.widgetType ? aiHumanize(widget.widgetType) : undefined, widget.stateId,
      widget.sizeX && widget.sizeY ? `${widget.sizeX} × ${widget.sizeY}` : undefined].filter(Boolean).join(' · ')
  }));
}

function dashboardOverview(result: unknown, names: AiDetailNames): AiDetailBlock[] {
  if (!isObject(result) || !Array.isArray(result.states)) {
    return generic(result, 'result');
  }
  const blocks: AiDetailBlock[] = [];
  const rows: AiDetailFact[] = [];
  if (str(result.theme)) {
    rows.push({label: K + 'fact.theme', value: aiHumanize(result.theme)});
  }
  const window = result.timewindow?.realtime?.last ?? result.timewindow?.realtime?.timewindowMs;
  if (window) {
    rows.push({label: K + 'fact.time-window', value: String(window)});
  }
  if (rows.length) {
    blocks.push({type: 'facts', rows});
  }
  blocks.push({
    type: 'list', label: text('states'), rows: result.states.filter(isObject).map(state => {
      const widgets = Array.isArray(state.widgets) ? state.widgets.length : 0;
      return [str(state.name) ?? state.id, state.root ? '★' : '', `${widgets} widget${widgets === 1 ? '' : 's'}`, state.layout].filter(Boolean).join(' · ');
    })
  });
  const aliases = (Array.isArray(result.aliases) ? result.aliases : []).filter(isObject);
  if (aliases.length) {
    blocks.push({type: 'list', label: text('aliases'), rows: aliases.map(alias => [alias.alias ?? alias.name ?? alias.id, alias.filter?.type ? aiHumanize(alias.filter.type) : alias.type].filter(Boolean).join(' · '))});
  }
  const filters = (Array.isArray(result.filters) ? result.filters : []).filter(isObject);
  if (filters.length) {
    blocks.push({type: 'list', label: text('filters'), rows: filters.map(item => String(item.filter ?? item.name ?? item.id))});
  }
  return blocks;
}

function partialString(source: string, key: string): string | undefined {
  const start = source.indexOf(`"${key}":"`);
  if (start < 0) {
    return undefined;
  }
  let body = source.slice(start + key.length + 4);
  const end = /(?:^|[^\\])(?:\\\\)*"/.exec(body);
  body = end ? body.slice(0, end.index + end[0].length - 1) : body.replace(/…$/, '').replace(/\\(u[0-9a-fA-F]{0,3})?$/, '');
  try {
    return JSON.parse(`"${body}"`);
  } catch {
    return undefined;
  }
}

function htmlPage(result: unknown): AiDetailBlock[] {
  if (typeof result === 'string' && result.trim().startsWith('{')) {
    const html = partialString(result, 'html');
    if (html) {
      return [{type: 'code', label: 'HTML', text: html}, {type: 'note', text: text('cut-short')}];
    }
  }
  const content = isObject(result) ? (isObject(result.content) ? result.content : result) : undefined;
  if (!content || (!str(content.html) && !str(content.css) && !str(content.js))) {
    return generic(result, 'result');
  }
  return (['html', 'css', 'js'] as const).filter(part => str(content[part])).map(part => ({type: 'code', label: part.toUpperCase(), text: content[part]}));
}

function savedObject(values: Values, nested?: string): Values {
  const raw = nested ? values[nested] : values;
  const parsed = aiParse(raw);
  return isObject(parsed) ? parsed : values;
}

function savedFacts(saved: Values, names: AiDetailNames): AiDetailFact[] {
  const rows: AiDetailFact[] = [];
  const config = isObject(aiParse(saved.configuration)) ? aiParse(saved.configuration) as Values : {};
  if (str(config.type)) {
    rows.push({label: K + 'fact.type', value: aiHumanize(config.type)});
  }
  if (saved.entityId) {
    rows.push({label: K + 'fact.on', entity: entity(saved.entityId, saved.entityId?.entityType, names)});
  }
  if (isObject(config.arguments) && Object.keys(config.arguments).length) {
    rows.push({label: K + 'fact.arguments', value: Object.entries(config.arguments)
      .map(([name, arg]: [string, any]) => arg?.refEntityKey?.key && arg.refEntityKey.key !== name ? `${name} = ${arg.refEntityKey.key}` : name).join(', '), mono: true});
  }
  if (str(config.expression)) {
    rows.push({label: K + 'fact.expression', value: config.expression, mono: true});
  }
  if (isObject(config.output) && str(config.output.name)) {
    rows.push({label: K + 'fact.output', value: config.output.name, mono: true});
  }
  if (isObject(config.createRules)) {
    rows.push({label: K + 'fact.severities', value: Object.keys(config.createRules).map(aiHumanize).join(', ')});
  }
  if (config.clearRule) {
    rows.push({label: K + 'fact.clears', value: K + 'yes'});
  }
  for (const key of ['label', 'description', 'notificationType', 'triggerType', 'recipientsConfig', 'templateId']) {
    if (saved[key] !== undefined && saved[key] !== null && saved[key] !== '') {
      rows.push(...facts({[key]: saved[key]}, [], names));
    }
  }
  if (!rows.length) {
    rows.push(...facts(saved, ['id', 'name', 'title', 'configuration', 'additionalInfo', 'tenantId', 'version', 'createdTime'], names));
  }
  return rows;
}

function changes(values: Values, skip: string[], names: AiDetailNames): AiDetailBlock[] {
  const rows: AiDetailFact[] = [];
  for (const [key, value] of Object.entries(values)) {
    if (skip.includes(key) || value === undefined) {
      continue;
    }
    if (key === 'widgetConfig' && isObject(value)) {
      const shown = widgetFacts({widgetConfig: value}, names);
      rows.push(...(shown.length ? shown : [{label: K + 'fact.settings', value: Object.keys(value).join(', '), mono: true}]));
    } else if (key === 'filter') {
      rows.push(...aliasFilter(value, names));
    } else if (key === 'entityAliasId') {
      rows.push(...widgetFacts({entityAliasId: value}, names));
    } else {
      rows.push(...facts({[key]: value}, [], names));
    }
  }
  return rows.length ? [{type: 'facts', rows}] : [];
}

export function aiActionDetailView(action: AiAction, names: AiDetailNames): AiDetailView {
  const values: Values = isObject(aiParse(action.arguments)) ? aiParse(action.arguments) as Values : {};
  const result = aiParse(action.result);
  const failed = action.status === 'FAILURE';
  const view = describe(action.tool, values, result, names);
  if (failed) {
    const reason = typeof action.result === 'string' ? action.result.trim() : '';
    return {hood: view.hood, blocks: reason.includes('\n') ? [{type: 'text', text: reason}] : []};
  }
  return view;
}

function accessBlocks(report: unknown): AiDetailBlock[] {
  if (!isObject(report)) {
    return [];
  }
  const hidden = Array.isArray(report.notVisibleToCustomer) ? report.notVisibleToCustomer.filter(isObject) : [];
  const blocks: AiDetailBlock[] = [{type: 'facts', rows: [
    ...(str(report.dashboard) ? [{label: K + 'fact.dashboard', value: report.dashboard}] : []),
    {label: K + 'fact.entities-checked', value: String(report.entitiesChecked ?? 0)}
  ]}];
  if (hidden.length) {
    blocks.push({type: 'records', label: text('not-visible', {count: hidden.length}), rows: hidden.map(item => ({
      entityType: str(item.entityType) ?? 'DEVICE', id: str(item.id), name: str(item.name) ?? '—',
      meta: [str(item.owner), ...(Array.isArray(item.groups) ? item.groups.filter(str) : [])].filter(Boolean).join(' · ') || undefined
    }))});
  } else {
    blocks.push({type: 'note', text: text('all-visible')});
  }
  if (Array.isArray(report.aliasesResolvedOnlyOnTheDashboard) && report.aliasesResolvedOnlyOnTheDashboard.length) {
    blocks.push({type: 'note', text: text('state-aliases', {aliases: report.aliasesResolvedOnlyOnTheDashboard.filter(str).join(', ')})});
  }
  if (str(report.truncated)) {
    blocks.push({type: 'note', text: text('access-truncated')});
  }
  return blocks;
}

function grantMeta(grant: Values): string | undefined {
  return [str(grant.role), str(grant.appliesTo) ?? [str(grant.userGroup), str(grant.userGroupOwner)].filter(Boolean).join(' · ')]
    .filter(Boolean).join(' · ') || undefined;
}

function groupRows(groups: unknown): AiDetailRecord[] {
  return (Array.isArray(groups) ? groups.filter(isObject) : []).map(group => ({
    entityType: 'ENTITY_GROUP', id: str(group.id), name: str(group.name) ?? '—',
    meta: (Array.isArray(group.grants) ? group.grants.filter(isObject) : []).map(grantMeta).filter(Boolean).join('; ') || undefined
  }));
}

function readAccess(target: string | undefined, result: unknown): AiDetailBlock[] {
  if (!isObject(result)) {
    return generic(result, 'result');
  }
  const blocks: AiDetailBlock[] = [];
  const fact = (key: string, value: unknown) => str(value) ? [{label: K + 'fact.' + key, value: String(value)}] : [];
  switch (target) {
    case 'USER':
      blocks.push({type: 'facts', rows: [...fact('email', result.email), ...fact('owner', result.owner)]});
      blocks.push({type: 'records', label: text('user-groups'), rows: groupRows(result.userGroups)});
      break;
    case 'USER_GROUP':
      blocks.push({type: 'facts', rows: [...fact('owner', result.owner), ...fact('members', String(result.memberCount ?? ''))]});
      blocks.push({type: 'records', label: text('grants'), rows: (Array.isArray(result.grants) ? result.grants.filter(isObject) : [])
        .map(grant => ({entityType: 'ROLE', name: str(grant.role) ?? '—', meta: str(grant.appliesTo)}))});
      blocks.push({type: 'records', label: text('members'), rows: (Array.isArray(result.members) ? result.members.filter(isObject) : [])
        .map(member => ({entityType: 'USER', id: str(member.id), name: str(member.email) ?? '—'}))});
      break;
    case 'ENTITY_GROUP':
      blocks.push({type: 'facts', rows: [...fact('type', result.type), ...fact('owner', result.owner)]});
      blocks.push({type: 'records', label: text('shared-with'), rows: (Array.isArray(result.sharedWith) ? result.sharedWith.filter(isObject) : [])
        .map(grant => ({entityType: 'ENTITY_GROUP', name: str(grant.userGroup) ?? '—', meta: [str(grant.userGroupOwner), str(grant.role)].filter(Boolean).join(' · ') || undefined}))});
      break;
    case 'CUSTOMER':
      blocks.push({type: 'records', label: text('user-groups'), rows: groupRows(result.userGroups)});
      break;
    case 'ROLES':
      blocks.push({type: 'records', label: text('roles'), rows: (Array.isArray(result.roles) ? result.roles.filter(isObject) : []).map(role => ({
        entityType: 'ROLE', id: str(role.id), name: str(role.name) ?? '—',
        meta: [str(role.type), permissionsText(role.permissions)].filter(Boolean).join(' · ') || undefined
      }))});
      break;
    default:
      return generic(result, 'result');
  }
  return blocks.filter(block => block.type !== 'records' || block.rows.length);
}

function permissionsText(permissions: unknown): string | undefined {
  if (Array.isArray(permissions)) {
    return permissions.filter(str).join(', ');
  }
  if (isObject(permissions)) {
    return Object.entries(permissions).map(([resource, operations]) =>
      resource + ': ' + (Array.isArray(operations) ? operations.join(', ') : '')).join('; ');
  }
  return undefined;
}

function sentNotification(result: unknown): AiDetailBlock[] {
  if (!isObject(result) || !str(result.requestId)) {
    return generic(result, 'result');
  }
  const rows: AiDetailFact[] = [];
  if (str(result.status)) {
    rows.push({label: K + 'fact.status', value: K + 'notification-status.' + String(result.status).toLowerCase()});
  }
  if (str(result.sendsAt)) {
    rows.push({label: K + 'fact.sends-at', value: new Date(result.sendsAt).toLocaleString()});
  }
  if (str(result.usersTargetName)) {
    rows.push({label: K + 'fact.recipients', value: result.usersTargetName});
  }
  if (isObject(result.sent)) {
    rows.push({label: K + 'fact.delivered',
      value: Object.entries(result.sent).map(([method, count]) => `${aiHumanize(method)} ${count}`).join(', ')});
  }
  if (isObject(result.errors)) {
    Object.entries(result.errors).filter(([, byRecipient]) => isObject(byRecipient)).forEach(([method, byRecipient]) =>
      Object.entries(byRecipient as Record<string, unknown>).forEach(([recipient, error]) =>
        rows.push({label: aiHumanize(method), raw: true, value: `${recipient}: ${aiValueText(error)}`})));
  }
  if (str(result.error)) {
    rows.push({label: K + 'fact.error', value: result.error});
  }
  return rows.length ? [{type: 'facts', rows}] : [];
}

function createdUser(user: unknown): AiDetailBlock[] {
  if (!isObject(user)) {
    return [];
  }
  const activation = isObject(user.activation) ? user.activation : null;
  const rows: AiDetailFact[] = [];
  if (str(user.email)) {
    rows.push({label: K + 'fact.email', value: user.email});
  }
  if (Array.isArray(user.userGroups) && user.userGroups.length) {
    rows.push({label: K + 'fact.user-groups', value: user.userGroups.filter(str).join(', ')});
  }
  if (activation?.emailSent) {
    rows.push({label: K + 'fact.activation', value: K + 'activation-email'});
  } else if (str(activation?.link)) {
    rows.push({label: K + 'fact.activation-link', value: activation.link, mono: true});
  }
  return rows.length ? [{type: 'facts', rows}] : [];
}

function describe(tool: string, values: Values, result: unknown, names: AiDetailNames): AiDetailView {
  const of = (id: unknown, type?: unknown) => entity(id, type, names);
  const target = of(values.entityId, values.entityType);
  const dashboard = of(values.dashboardId, 'DASHBOARD');
  switch (tool) {
    case 'send_notification':
      return {blocks: sentNotification(result)};
    case 'check_dashboard_access':
      return {hood: text('check-dashboard-access', {dashboard, customer: of(values.customerId, 'CUSTOMER')}), blocks: accessBlocks(result)};
    case 'change_owner':
    case 'share_entity_group': {
      const outcome = isObject(result) && str(result.result) ? [{type: 'text' as const, text: result.result.replace(/^OK:\s*/i, '')}] : generic(result, 'result');
      const reports = isObject(result) ? (Array.isArray(result.dashboardAccess) ? result.dashboardAccess : result.dashboardAccess ? [result.dashboardAccess] : []) : [];
      return {blocks: [...outcome, ...reports.flatMap(accessBlocks)]};
    }
    case 'save_entity_group':
    case 'grant_role':
    case 'update_group_members':
      return {blocks: []};
    case 'save_role': {
      const permissions = permissionsText(values.permissions);
      return {blocks: permissions ? [{type: 'facts', rows: [{label: K + 'fact.permissions', value: permissions}]}] : []};
    }
    case 'read_access':
      return {blocks: readAccess(str(values.target)?.toUpperCase(), result)};
    case 'save_user':
      return {blocks: isObject(result) && str(result.userId) ? createdUser(result) : generic(result, 'result')};
    case 'save_customer':
      return {blocks: isObject(result) && str(result.customerId) ? [...createdUser(result.user),
        ...(str(result.userError) ? [{type: 'note' as const, text: text('user-error', {reason: result.userError})}] : [])] : generic(result, 'result')};
    case 'search_iot_hub_devices': {
      const data = isObject(result) && Array.isArray(result.data) ? result.data.filter(isObject) : null;
      const rows: AiDetailHubItem[] = (data ?? []).filter(item => str(item.id)).map(item => ({
        id: item.id, name: str(item.model) ?? str(item.name) ?? item.id, vendor: str(item.vendor), description: str(item.description)
      }));
      return {
        hood: text('iot-hub-query', {query: str(values.query) ?? '*'}),
        blocks: data ? (rows.length ? [{type: 'hub', rows}] : [{type: 'note', text: text('nothing-found')}]) : generic(result, 'result')
      };
    }
    case 'search_iot_hub':
    case 'list_installed_iot_hub_items': {
      const data = isObject(result) && Array.isArray(result.data) ? result.data.filter(isObject) : null;
      const rows: AiDetailHubItem[] = (data ?? []).filter(item => str(item.versionId)).map(item => ({
        id: item.versionId, name: str(item.name) ?? item.versionId, vendor: str(item.creator), description: str(item.description)
      }));
      const hood = tool === 'search_iot_hub'
        ? (str(values.query) ? text('iot-hub-query', {query: str(values.query)}) : text('iot-hub-browse'))
        : text('iot-hub-installed');
      return {hood, blocks: data ? (rows.length ? [{type: 'hub', rows}] : [{type: 'note', text: text('nothing-found')}]) : generic(result, 'result')};
    }
    case 'read_iot_hub_item': {
      const item = isObject(result) && str(result.versionId) ? result : null;
      return {
        hood: text('iot-hub-read', {name: str(item?.name) ?? '…'}),
        blocks: item ? [{type: 'hub', rows: [{id: item.versionId, name: str(item.name) ?? item.versionId, vendor: str(item.creator),
          description: str(item.description)}]}] : generic(result, 'result')
      };
    }
    case 'search_entities':
      return searchEntities(values, result, names);
    case 'read_entity': {
      const read = isObject(result) ? result : null;
      if (!read) {
        return {hood: text('read-entity', {entity: target}), blocks: generic(result, 'result')};
      }
      const record: AiDetailRecord = {entityType: target.entityType ?? 'DEVICE', id: target.id, name: str(read.name) ?? str(read.title) ?? target.name ?? '—',
        meta: [read.label && read.label !== read.name ? read.label : undefined, read.deviceProfileName ?? read.assetProfileName ?? read.type].filter(Boolean).join(' · ') || undefined};
      const configured = isObject(aiParse(read.configuration));
      const rows = [
        ...(configured ? savedFacts(read, names) : []),
        ...facts(read, ['id', 'name', 'title', 'label', 'createdTime', 'deviceProfileName', 'assetProfileName', 'tenantId', 'type', 'additionalInfo',
          'debugSettings', 'configuration', 'version', 'configurationVersion', ...(configured ? ['entityId', 'description'] : [])], names)
      ];
      if (read.createdTime) {
        rows.push({label: K + 'fact.created', value: new Date(read.createdTime).toLocaleString()});
      }
      return {hood: text('read-entity', {entity: {...target, name: record.name}}), blocks: [{type: 'records', rows: [record]}, ...(rows.length ? [{type: 'facts', rows} as AiDetailBlock] : [])]};
    }
    case 'read_entity_relations':
      return {hood: text('read-relations', {entity: target}), blocks: relations(result)};
    case 'read_timeseries': {
      const keys = Array.isArray(values.keys) ? values.keys.join(', ') : '';
      const hood = values.mode === 'HISTORY' || values.startTime
        ? text(keys ? 'read-history-keys' : 'read-history', {entity: target, keys, from: dateText(values.startTime), to: endText(values.endTime)})
        : text(keys ? 'read-latest-keys' : 'read-latest', {entity: target, keys});
      const blocks = isObject(result) ? telemetry(result) : generic(result, 'result');
      return {hood, blocks: isObject(result) && !Object.keys(result).length ? [{type: 'note', text: text('no-data')}] : blocks};
    }
    case 'read_timeseries_keys': {
      const data = isObject(result) ? (isObject(result.timeseries) ? result.timeseries : result) : null;
      let blocks: AiDetailBlock[];
      if (Array.isArray(data)) {
        blocks = [{type: 'keys', keys: data.map(String)}];
      } else if (data && Object.keys(data).length) {
        blocks = telemetry(data);
      } else {
        blocks = data ? [{type: 'note', text: text('no-data')}] : generic(result, 'result');
      }
      return {hood: text('read-keys', {entity: target}), blocks};
    }
    case 'read_attributes':
      return {hood: text(values.scope ? 'read-attributes-scope' : 'read-attributes', {entity: target, scope: K + 'scope.' + (SCOPES[values.scope] ?? 'other')}), blocks: attributes(result, 'values')};
    case 'discover_attribute_keys':
      return {hood: text('discover-attribute-keys', {entity: of(values.profileId, values.profileType)}), blocks: attributes(result, 'keys')};
    case 'save_attributes': {
      const saved = aiParse(values.attributes);
      const rows = isObject(saved) ? Object.entries(saved).map(([key, value]) => ({key, value: aiValueText(value)})) : [];
      return {
        hood: text('save-attributes', {entity: target, scope: K + 'scope.' + (SCOPES[values.scope] ?? 'other'), count: rows.length}),
        blocks: rows.length ? [{type: 'values', rows}, ...generic(result, 'result')] : generic(result, 'result')
      };
    }
    case 'delete_attributes': {
      const keys = Array.isArray(values.keys) ? values.keys.map(String) : [];
      return {hood: text('delete-attributes', {entity: target, scope: K + 'scope.' + (SCOPES[values.scope] ?? 'other')}), blocks: [...(keys.length ? [{type: 'keys', keys} as AiDetailBlock] : []), ...generic(result, 'result')]};
    }
    case 'read_device_credentials': {
      const device = of(values.deviceId, 'DEVICE');
      if (!isObject(result)) {
        return {hood: text('read-credentials', {entity: device}), blocks: generic(result, 'result')};
      }
      const rows: AiDetailFact[] = [];
      if (str(result.credentialsType)) {
        rows.push({label: K + 'fact.type', value: aiHumanize(result.credentialsType)});
      }
      for (const key of ['accessToken', 'credentialsId', 'clientId', 'userName', 'password', 'credentialsValue']) {
        if (str(result[key])) {
          rows.push({label: aiHumanize(key), raw: true, value: result[key], mono: true, secret: /token|password|value|credentialsId/i.test(key)});
        }
      }
      return {hood: text('read-credentials', {entity: device}), blocks: [{type: 'facts', rows}]};
    }
    case 'read_transport_endpoints': {
      if (!isObject(result)) {
        return {hood: text('read-endpoints'), blocks: generic(result, 'result')};
      }
      const rows: AiDetailFact[] = Object.entries(isObject(result.transports) ? result.transports : {})
        .map(([name, endpoint]: [string, any]) => ({label: name.toUpperCase(), raw: true, value: [endpoint?.host, endpoint?.port].filter(Boolean).join(':'), mono: true}));
      if (Array.isArray(result.disabled) && result.disabled.length) {
        rows.push({label: K + 'fact.disabled', value: result.disabled.map((name: string) => name.toUpperCase()).join(', ')});
      }
      const blocks: AiDetailBlock[] = [{type: 'facts', rows}];
      if (str(result.note)) {
        blocks.push({type: 'text', text: result.note});
      }
      return {hood: text('read-endpoints'), blocks};
    }
    case 'read_device_connect_commands': {
      const rows = commands(result);
      return {hood: text('read-connect-commands', {entity: of(values.deviceId, 'DEVICE')}), blocks: rows.length ? [{type: 'commands', rows}] : generic(result, 'result')};
    }
    case 'research_device_connectivity':
      return {
        hood: text(str(values.question) ? 'research-question' : 'research', {device: str(values.device) ?? '—', question: str(values.question) ?? ''}),
        blocks: typeof result === 'string' && result.trim() ? [{type: 'markdown', text: result.trim()}] : generic(result, 'result')
      };
    case 'search_web':
      return {hood: text('search-web', {query: str(values.query) ?? '—'}), blocks: webResults(result)};
    case 'fetch_web_page':
      return {
        hood: text('fetch-page', {url: str(values.url) ?? '—'}),
        blocks: typeof result === 'string' && result.trim() ? [{type: 'markdown', text: result.trim()}] : generic(result, 'result')
      };
    case 'create_dashboard':
      return {hood: text('create-dashboard', {name: str(values.title) ?? '—'}), blocks: []};
    case 'update_dashboard':
      return {hood: text('update-dashboard', {entity: dashboard}), blocks: changes(values, ['dashboardId'], names)};
    case 'read_dashboard_overview':
      return {hood: text('read-dashboard', {entity: dashboard}), blocks: dashboardOverview(result, names)};
    case 'add_dashboard_state': {
      const id = okId(result);
      return {hood: text('add-state', {name: str(values.name) ?? '—', entity: dashboard}), blocks: id ? [{type: 'facts', rows: [{label: K + 'fact.state-id', value: id, mono: true}]}] : generic(result, 'result')};
    }
    case 'update_dashboard_state':
      return {hood: text('update-state', {state: str(values.stateId) ?? '—', entity: dashboard}), blocks: changes(values, ['dashboardId', 'stateId'], names)};
    case 'add_dashboard_entity_alias':
    case 'update_dashboard_entity_alias':
      return {
        hood: text(tool === 'add_dashboard_entity_alias' ? 'add-alias' : 'update-alias', {name: str(values.alias) ?? names.get(values.aliasId)?.name ?? '—', entity: dashboard}),
        blocks: [{type: 'facts', rows: aliasFilter(values.filter, names)}]
      };
    case 'read_dashboard_entity_alias': {
      const read = isObject(result) ? result : null;
      return {
        hood: text('read-alias', {name: str(read?.alias) ?? names.get(values.aliasId)?.name ?? '—'}),
        blocks: read ? [{type: 'facts', rows: aliasFilter(read.filter, names)}] : generic(result, 'result')
      };
    }
    case 'add_dashboard_filter':
    case 'update_dashboard_filter':
    case 'read_dashboard_filter': {
      const name = str(values.filter) ?? str(values.name) ?? (isObject(result) ? str(result.filter) ?? str(result.name) : undefined) ?? '—';
      const shown = tool === 'read_dashboard_filter' ? generic(result, 'result', ['id'], names) : changes(values, ['dashboardId', 'filterId'], names);
      return {hood: text(tool.replace(/_dashboard_filter$/, '-filter'), {name, entity: dashboard}), blocks: shown};
    }
    case 'add_dashboard_widgets': {
      const rows = widgetRecords(values.widgets, result);
      return {hood: text(rows.length === 1 ? 'add-widget' : 'add-widgets', {count: rows.length, entity: dashboard}), blocks: rows.length ? [{type: 'records', rows}] : generic(result, 'result')};
    }
    case 'update_dashboard_widget':
      return {
        hood: text('update-widget', {name: str(values.title) ?? names.get(values.widgetId)?.name ?? '—', entity: dashboard}),
        blocks: changes(values, ['dashboardId', 'widgetId'], names)
      };
    case 'read_dashboard_widget': {
      const read = isObject(result) ? result : null;
      return {
        hood: text('read-widget', {name: str(read?.title) ?? names.get(values.widgetId)?.name ?? '—'}),
        blocks: read ? [{type: 'facts', rows: widgetFacts(read, names)}] : generic(result, 'result')
      };
    }
    case 'place_dashboard_widgets': {
      const rows = (Array.isArray(values.placements) ? values.placements : []).filter(isObject)
        .map(place => `${names.get(place.widgetId)?.name ?? place.widgetId} · ${place.sizeX} × ${place.sizeY} at ${place.col}, ${place.row}`);
      return {hood: text('place-widgets', {count: rows.length, entity: dashboard}), blocks: rows.length ? [{type: 'list', rows}] : generic(result, 'result')};
    }
    case 'set_dashboard_html_page': {
      const brief = isObject(values.brief) ? values.brief : {};
      const rows: AiDetailFact[] = [];
      for (const key of ['description', 'design', 'embeds', 'aliases']) {
        const value = brief[key];
        if (value !== undefined && value !== '' && !(Array.isArray(value) && !value.length)) {
          rows.push({label: K + 'fact.' + key, value: Array.isArray(value) ? value.join(', ') : String(value)});
        }
      }
      return {hood: text('set-html-page', {state: str(values.stateId) ?? 'default', entity: dashboard}), blocks: rows.length ? [{type: 'facts', rows}] : []};
    }
    case 'read_dashboard_html_page':
      return {hood: text('read-html-page', {state: str(values.stateId) ?? 'default', entity: dashboard}), blocks: htmlPage(result)};
    case 'delete': {
      if (values.targetType === 'IMAGE') {
        const key = String(values.targetId ?? '').split('/').pop() || '—';
        return {hood: text('delete', {entity: {entityType: 'IMAGE', name: key}}), blocks: generic(result, 'result')};
      }
      const type = DELETE_TYPES[values.targetType] ?? values.targetType;
      return {hood: text('delete', {entity: of(values.targetId, type)}), blocks: generic(result, 'result')};
    }
    case 'save_device': {
      const saved = savedObject(values);
      const rows: AiDetailFact[] = [];
      if (str(saved.label) && saved.label !== saved.name) {
        rows.push({label: K + 'fact.label', value: saved.label});
      }
      const profile = saved.profileId ?? saved.deviceProfileId;
      if (profile) {
        rows.push({label: K + 'fact.profile', entity: of(profile, 'DEVICE_PROFILE')});
      }
      if (saved.customerId) {
        rows.push({label: K + 'fact.customer', entity: of(saved.customerId, 'CUSTOMER')});
      }
      if (saved.additionalInfo?.gateway) {
        rows.push({label: K + 'fact.gateway', value: K + 'yes'});
      }
      return {hood: text(hasId(saved) ? 'update-device' : 'create-device', {name: str(saved.name) ?? '—'}), blocks: rows.length ? [{type: 'facts', rows}] : []};
    }
    case 'save_calculated_field':
    case 'save_alarm_rule':
    case 'save_notification_target':
    case 'save_notification_template':
    case 'save_notification_rule': {
      const nested = {save_alarm_rule: 'alarmRule', save_notification_target: 'target', save_notification_template: 'template', save_notification_rule: 'rule'}[tool];
      const saved = savedObject(values, nested);
      const kind = tool.replace(/^save_/, '').replace(/_/g, '-');
      return {hood: text('save', {what: K + 'kind.' + kind, name: str(saved.name) ?? str(saved.title) ?? '—'}), blocks: [{type: 'facts', rows: savedFacts(saved, names)}]};
    }
    case 'save_asset':
    case 'save_entity_view':
    case 'save_asset_profile':
    case 'save_secret':
    case 'save_ai_model':
    case 'save_resource': {
      const kind = tool.replace(/^save_/, '').replace(/_/g, '-');
      const saved = isObject(result) ? result : {};
      const name = str(values.name) ?? str(values.title) ?? str(saved.name) ?? str(saved.title) ?? '—';
      const rows: AiDetailFact[] = [];
      if (str(saved.reference)) {
        rows.push({label: K + 'fact.reference', value: saved.reference, mono: true});
      }
      if (str(saved.link)) {
        rows.push({label: K + 'fact.link', value: saved.link, mono: true});
      }
      if (saved.tested === true) {
        rows.push({label: K + 'fact.tested', value: K + 'yes'});
      }
      return {hood: text('save', {what: K + 'kind.' + kind, name}), blocks: rows.length ? [{type: 'facts', rows}] : generic(result, 'result')};
    }
    case 'save_relations': {
      if (!isObject(result)) {
        return {hood: text('save-relations'), blocks: generic(result, 'result')};
      }
      const blocks: AiDetailBlock[] = [];
      for (const [field, label] of [['added', 'relations-added'], ['removed', 'relations-removed']] as const) {
        const rows = Array.isArray(result[field]) ? result[field].map(String) : [];
        if (rows.length) {
          blocks.push({type: 'list', label: text(label), rows: rows.slice(0, MAX_ROWS)});
        }
      }
      const failed = Array.isArray(result.failed) ? result.failed.filter(isObject) : [];
      if (failed.length) {
        blocks.push({type: 'list', label: text('relations-failed'), rows: failed.slice(0, MAX_ROWS).map(row => `${row.relation}: ${row.error}`)});
      }
      return {hood: text('save-relations'), blocks};
    }
    case 'read_gateway_status': {
      if (!isObject(result)) {
        return {hood: text('read-gateway-status'), blocks: generic(result, 'result')};
      }
      const rows: AiDetailFact[] = [{label: K + 'fact.status', value: K + (result.online ? 'gateway-online' : 'gateway-offline')}];
      if (str(result.version)) {
        rows.push({label: K + 'fact.version', value: result.version});
      }
      if (str(result.lastConnect)) {
        rows.push({label: K + 'fact.last-connect', value: new Date(result.lastConnect).toLocaleString()});
      }
      const blocks: AiDetailBlock[] = [{type: 'facts', rows}];
      const connectors = Array.isArray(result.connectors) ? result.connectors.filter(isObject) : [];
      if (connectors.length) {
        blocks.push({type: 'list', label: text('gateway-connectors'), rows: connectors.map(connector => [
          `${connector.name} (${connector.type})`, connector.active ? '●' : '○',
          connector.applied === true ? '✓' : connector.applied === false ? '…' : undefined, str(connector.problem)
        ].filter(Boolean).join(' '))});
      }
      for (const [key, lines] of Object.entries(isObject(result.logs) ? result.logs : {})) {
        if (Array.isArray(lines) && lines.length) {
          blocks.push({type: 'code', label: key, text: lines.map(String).join('\n')});
        }
      }
      return {hood: text('read-gateway-status', {name: str(result.name) ?? '—'}), blocks};
    }
    case 'read_white_labeling':
      return {hood: text('read-white-labeling'), blocks: json('settings', result)};
    case 'save_white_labeling': {
      const {target, ...changes} = values;
      return {hood: text(target === 'LOGIN' ? 'save-white-labeling-login' : 'save-white-labeling'), blocks: [{type: 'facts', rows: facts(changes)}]};
    }
    case 'read_ota': {
      if (!isObject(result)) {
        return {hood: text('read-ota'), blocks: generic(result, 'result')};
      }
      if (Array.isArray(result.packages)) {
        const rows = result.packages.filter(isObject).slice(0, MAX_ROWS)
          .map(pkg => `${pkg.title} ${pkg.version} · ${pkg.type} · ${pkg.source}`);
        return {hood: text('read-ota-packages'), blocks: rows.length ? [{type: 'list', rows}] : []};
      }
      const states = isObject(result.states) ? result.states : {};
      const facts: AiDetailFact[] = Object.entries(states).map(([state, count]) => ({label: aiHumanize(state), raw: true, value: String(count)}));
      const rows = (Array.isArray(result.list) ? result.list.filter(isObject) : []).slice(0, MAX_ROWS)
        .map(device => [device.name, device.state, str(device.current), str(device.target) ? '→ ' + device.target : undefined].filter(Boolean).join(' · '));
      const blocks: AiDetailBlock[] = facts.length ? [{type: 'facts', rows: facts}] : [];
      if (rows.length) {
        blocks.push({type: 'list', rows});
      }
      return {hood: text('read-ota', {count: Number(result.devices) || 0}), blocks};
    }
    case 'save_ota_package':
      return {hood: text('save-ota-package', {name: [str(values.title), str(values.version)].filter(Boolean).join(' ') || '—'}), blocks: generic(result, 'result')};
    case 'assign_ota_package': {
      const updated = isObject(result) && Array.isArray(result.updated) ? result.updated.map(String) : [];
      const failed = isObject(result) && Array.isArray(result.failed) ? result.failed.filter(isObject) : [];
      const blocks: AiDetailBlock[] = [];
      if (updated.length) {
        blocks.push({type: 'list', label: text('alarms-done'), rows: updated.slice(0, MAX_ROWS)});
      }
      if (failed.length) {
        blocks.push({type: 'list', label: text('alarms-failed'), rows: failed.slice(0, MAX_ROWS).map(row => `${row.target}: ${row.error}`)});
      }
      return {hood: text(values.clear ? 'clear-ota-package' : 'assign-ota-package'), blocks: blocks.length ? blocks : generic(result, 'result')};
    }
    case 'read_versions': {
      const versions = isObject(result) && Array.isArray(result.versions) ? result.versions.filter(isObject) : undefined;
      if (!versions) {
        return {hood: text('read-versions'), blocks: generic(result, 'result')};
      }
      const rows = versions.slice(0, MAX_ROWS).map(version => [str(version.name), str(version.author),
        str(version.time) ? new Date(version.time).toLocaleString() : undefined].filter(Boolean).join(' · '));
      return {hood: text('read-versions'), blocks: rows.length ? [{type: 'list', rows}] : []};
    }
    case 'commit_version': {
      const saved = isObject(result) ? result : {};
      const rows: AiDetailFact[] = [];
      if (str(saved.versionId)) {
        rows.push({label: K + 'fact.version', value: saved.versionId, mono: true});
      }
      if (str(saved.branch)) {
        rows.push({label: K + 'fact.branch', value: saved.branch});
      }
      for (const field of ['added', 'modified', 'removed']) {
        if (typeof saved[field] === 'number') {
          rows.push({label: K + 'fact.' + field, value: String(saved[field])});
        }
      }
      return {hood: text('commit-version', {name: str(values.name) ?? '—'}), blocks: rows.length ? [{type: 'facts', rows}] : generic(result, 'result')};
    }
    case 'restore_version': {
      const types = isObject(result) && Array.isArray(result.result) ? result.result.filter(isObject) : undefined;
      const rows = (types ?? []).map(type => `${type.entityType}: +${type.created} · ~${type.updated} · −${type.deleted}`);
      return {hood: text('restore-version', {version: String(values.versionId ?? '').slice(0, 8)}), blocks: types ? [{type: 'list', rows}] : generic(result, 'result')};
    }
    case 'read_platform_settings':
      return {hood: text('read-platform-settings'), blocks: json('settings', result)};
    case 'save_platform_settings':
      return {hood: text('save-platform-settings'), blocks: json('settings', result)};
    case 'update_alarms': {
      const action = String(values.action ?? '').toLowerCase();
      const count = Array.isArray(values.alarmIds) ? values.alarmIds.length : 0;
      if (!isObject(result)) {
        return {hood: text('update-alarms.' + action, {count}), blocks: generic(result, 'result')};
      }
      const blocks: AiDetailBlock[] = [];
      const done = Array.isArray(result.done) ? result.done.map(String) : [];
      const skipped = Array.isArray(result.skipped) ? result.skipped.map(String) : [];
      const failed = Array.isArray(result.failed) ? result.failed.filter(isObject) : [];
      if (done.length) {
        blocks.push({type: 'list', label: text('alarms-done'), rows: done.slice(0, MAX_ROWS)});
      }
      if (skipped.length) {
        blocks.push({type: 'list', label: text('alarms-skipped'), rows: skipped.slice(0, MAX_ROWS)});
      }
      if (failed.length) {
        blocks.push({type: 'list', label: text('alarms-failed'), rows: failed.slice(0, MAX_ROWS).map(row => `${row.alarm}: ${row.error}`)});
      }
      if (str(values.comment)) {
        blocks.push({type: 'facts', rows: [{label: K + 'fact.comment', value: values.comment}]});
      }
      return {hood: text('update-alarms.' + action, {count}), blocks};
    }
    case 'read_api_usage':
      return {hood: text('read-api-usage'), blocks: apiUsageBlocks(result)};
    case 'read_audit_logs':
      return {hood: values.entityId ? text('read-audit-logs-of', {entity: target}) : text('read-audit-logs'), blocks: auditLogBlocks(result)};
    case 'read_tasks':
      return {hood: text('read-tasks'), blocks: taskBlocks(result)};
    case 'save_image': {
      const saved = isObject(result) ? result : {};
      const rows: AiDetailFact[] = str(saved.link) ? [{label: K + 'fact.link', value: saved.link, mono: true}] : [];
      return {hood: text('save-image', {name: str(saved.title) ?? str(values.title) ?? '—'}), blocks: rows.length ? [{type: 'facts', rows}] : generic(result, 'result')};
    }
    case 'reprocess_calculated_field':
      return {hood: text('reprocess', {entity: of(values.calculatedFieldId, 'CALCULATED_FIELD'), from: dateText(values.startTime ?? values.startTs), to: endText(values.endTime ?? values.endTs)}), blocks: generic(result, 'result')};
  }
  const request = Object.keys(values).length ? (flat(values) ? [{type: 'facts', rows: facts(values, [], names)} as AiDetailBlock] : json('request', values)) : [];
  return {blocks: [...request, ...generic(result, 'result', [], names)]};
}

const USAGE_LIMIT = (limit: unknown): string => limit === 'unlimited' || limit === undefined ? '∞' : aiValueText(limit);

function apiUsageBlocks(result: unknown): AiDetailBlock[] {
  if (!isObject(result)) {
    return generic(result, 'result');
  }
  const rows: AiDetailFact[] = [];
  const warned: string[] = [];
  for (const [name, entry] of Object.entries(isObject(result.entities) ? result.entities : {})) {
    if (isObject(entry)) {
      rows.push({label: K + 'usage.' + name, value: `${aiValueText(entry.count)} / ${USAGE_LIMIT(entry.limit)}`});
    }
  }
  for (const entry of Array.isArray(result.monthly) ? result.monthly : []) {
    if (isObject(entry) && str(entry.name)) {
      rows.push({label: K + 'usage.' + entry.name, value: `${aiValueText(entry.count)} / ${USAGE_LIMIT(entry.limit)}`});
      if (entry.state === 'WARNING' || entry.state === 'DISABLED') {
        warned.push(entry.name);
      }
    }
  }
  const blocks: AiDetailBlock[] = rows.length ? [{type: 'facts', rows}] : [];
  if (warned.length) {
    blocks.push({type: 'note', text: text('usage.limited', {count: warned.length})});
  }
  return blocks;
}

function auditLogBlocks(result: unknown): AiDetailBlock[] {
  const logs = isObject(result) && Array.isArray(result.logs) ? result.logs.filter(isObject) : undefined;
  if (!logs) {
    return generic(result, 'result');
  }
  const rows: AiDetailRecord[] = logs.slice(0, MAX_ROWS).map(log => {
    const target = isObject(log.entity) ? log.entity : {};
    const when = str(log.time) ? new Date(log.time).toLocaleString() : undefined;
    const meta = [str(log.action) ? aiHumanize(log.action) : undefined, str(log.user), when, log.status === 'FAILURE' ? '✕' : undefined]
      .filter(Boolean).join(' · ');
    return {entityType: str(target.type) ?? 'USER', id: str(target.id), name: str(target.name) ?? str(target.type) ?? '—', meta};
  });
  return [{type: 'records', rows, total: logs.length}];
}

function taskBlocks(result: unknown): AiDetailBlock[] {
  const tasks = isObject(result) && Array.isArray(result.tasks) ? result.tasks.filter(isObject) : undefined;
  if (!tasks) {
    return generic(result, 'result');
  }
  const rows: AiDetailRecord[] = tasks.slice(0, MAX_ROWS).map(task => {
    const target = isObject(task.entity) ? task.entity : {};
    const progress = isObject(task.progress) ? task.progress : undefined;
    const done = progress ? `${aiValueText(progress.successful)}${progress.total !== undefined ? ' / ' + aiValueText(progress.total) : ''}` : undefined;
    const meta = [str(task.status) ? aiHumanize(task.status) : undefined, done, str(task.error)].filter(Boolean).join(' · ');
    return {entityType: str(target.type) ?? 'CALCULATED_FIELD', id: str(target.id), name: str(target.name) ?? (str(task.type) ? aiHumanize(task.type) : '—'), meta};
  });
  return [{type: 'records', rows, total: tasks.length}];
}

function hasId(saved: Values): boolean {
  return !!idOf(saved.id);
}

function endText(value: unknown): string {
  const shown = dateText(value);
  return shown === '—' ? K + 'now' : shown;
}

function dateText(value: unknown): string {
  const parsed = typeof value === 'string' && /^\d+$/.test(value) ? Number(value) : value;
  const date = typeof parsed === 'number' ? new Date(parsed) : typeof parsed === 'string' ? new Date(parsed) : null;
  return date && !isNaN(date.getTime()) ? date.toLocaleString([], {dateStyle: 'medium', timeStyle: 'short'}) : '—';
}

const NAMES_CACHE = new Map<string, Map<string, AiDetailEntity>>();

const MAX_CACHED = 500;

export function aiDetailNames(messages: Array<{from: string; data?: any; references?: Array<{id: string; name: string; entityType: string}>}>,
                              previous?: AiDetailNames): AiDetailNames {
  const names = new Map<string, AiDetailEntity>();
  for (const message of messages) {
    let found: Map<string, AiDetailEntity>;
    if (message.from === 'ACTION' && message.data?.executionId) {
      const key = message.data.executionId + ':' + message.data.status;
      found = NAMES_CACHE.get(key);
      if (!found) {
        found = namesOf(message);
        if (NAMES_CACHE.size >= MAX_CACHED) {
          NAMES_CACHE.delete(NAMES_CACHE.keys().next().value);
        }
        NAMES_CACHE.set(key, found);
      }
    } else if (message.references?.length) {
      found = namesOf(message);
    } else {
      continue;
    }
    found.forEach((value, id) => !names.has(id) && names.set(id, value));
  }
  if (previous && previous.size === names.size && [...names].every(([id, value]) => {
    const old = previous.get(id);
    return old && old.name === value.name && old.entityType === value.entityType;
  })) {
    return previous;
  }
  return names;
}

function namesOf(message: {from: string; data?: any; references?: Array<{id: string; name: string; entityType: string}>}): Map<string, AiDetailEntity> {
  const names = new Map<string, AiDetailEntity>();
  const put = (id: unknown, name: unknown, entityType?: unknown) => {
    const key = idOf(id);
    const label = str(name);
    if (key && label && !names.has(key)) {
      names.set(key, {id: key, name: label, entityType: str(entityType) ?? (isObject(id) ? str(id.entityType) : undefined)});
    }
  };
  const walk = (value: unknown, entityType: string | undefined, depth: number) => {
    if (depth > 6 || !value || typeof value !== 'object') {
      return;
    }
    if (Array.isArray(value)) {
      value.forEach(item => walk(item, entityType, depth + 1));
      return;
    }
    const object = value as Values;
    const id = object.entityId ?? object.id;
    if (idOf(id) && UUID.test(idOf(id))) {
      put(id, object.name ?? object.title ?? object.alias, object.entityType ?? entityType);
    }
    Object.values(object).forEach(item => walk(item, undefined, depth + 1));
  };
  message.references?.forEach(reference => put(reference.id, reference.name, reference.entityType));
  if (message.from === 'ACTION' && message.data) {
    const action = message.data as AiAction;
    const values: Values = isObject(aiParse(action.arguments)) ? aiParse(action.arguments) as Values : {};
    const result = aiParse(action.result);
    const created = okId(result);
    switch (action.tool) {
      case 'create_dashboard':
        put(created, values.title, 'DASHBOARD');
        break;
      case 'save_device':
        put(created ?? savedObject(values).id, savedObject(values).name, 'DEVICE');
        break;
      case 'add_dashboard_entity_alias':
        put(created, values.alias, 'DASHBOARD_ENTITY_ALIAS');
        break;
      case 'add_dashboard_widgets':
        widgetRecords(values.widgets, result).forEach(row => put(row.id, row.name, 'DASHBOARD_WIDGET'));
        break;
      case 'read_entity':
        if (isObject(result)) {
          put(result.id ?? values.entityId, result.name ?? result.title, values.entityType);
        }
        break;
      case 'search_entities':
        for (const section of searchSections(result)) {
          walk(section.page.data, section.entityType ?? searchType(values), 0);
        }
        return names;
    }
    walk(result, undefined, 0);
  }
  return names;
}
