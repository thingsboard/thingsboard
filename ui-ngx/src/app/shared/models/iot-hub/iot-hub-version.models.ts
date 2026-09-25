// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { ItemType } from './iot-hub-item.models';
import { PageLink } from '@shared/models/page/page-link';

export const widgetTypeTranslations = new Map<string, string>([
  ['timeseries', 'item-data.widget-type-timeseries'],
  ['latest', 'item-data.widget-type-latest'],
  ['rpc', 'item-data.widget-type-rpc'],
  ['alarm', 'item-data.widget-type-alarm'],
  ['static', 'item-data.widget-type-static'],
]);

export const cfTypeTranslations = new Map<string, string>([
  ['SIMPLE', 'item-data.cf-type-simple'],
  ['SCRIPT', 'item-data.cf-type-script'],
  ['GEOFENCING', 'item-data.cf-type-geofencing'],
  ['ALARM', 'item-data.cf-type-alarm'],
  ['PROPAGATION', 'item-data.cf-type-propagation'],
  ['RELATED_ENTITIES_AGGREGATION', 'item-data.cf-type-related-entities-aggregation'],
  ['ENTITY_AGGREGATION', 'item-data.cf-type-entity-aggregation'],
]);

export const cfTypeIcons = new Map<string, string>([
  ['SIMPLE', 'calculate'],
  ['SCRIPT', 'code'],
  ['GEOFENCING', 'share_location'],
  ['ALARM', 'notification_important'],
  ['PROPAGATION', 'account_tree'],
  ['RELATED_ENTITIES_AGGREGATION', 'hub'],
  ['ENTITY_AGGREGATION', 'functions'],
]);

export enum NodeComponentType {
  ENRICHMENT = 'ENRICHMENT',
  FILTER = 'FILTER',
  TRANSFORMATION = 'TRANSFORMATION',
  ACTION = 'ACTION',
  ANALYTICS = 'ANALYTICS',
  EXTERNAL = 'EXTERNAL',
  FLOW = 'FLOW',
  UNKNOWN = 'UNKNOWN'
}

export const nodeComponentTypeTranslations = new Map<NodeComponentType, string>([
  [NodeComponentType.ENRICHMENT, 'item-data.node-type-enrichment'],
  [NodeComponentType.FILTER, 'item-data.node-type-filter'],
  [NodeComponentType.TRANSFORMATION, 'item-data.node-type-transformation'],
  [NodeComponentType.ACTION, 'item-data.node-type-action'],
  [NodeComponentType.ANALYTICS, 'item-data.node-type-analytics'],
  [NodeComponentType.EXTERNAL, 'item-data.node-type-external'],
  [NodeComponentType.FLOW, 'item-data.node-type-flow'],
  [NodeComponentType.UNKNOWN, 'item-data.node-type-unknown'],
]);

export interface NodeInfo {
  name: string;
  type: NodeComponentType;
}

export const ruleChainTypeTranslations = new Map<string, string>([
  ['CORE', 'item-data.rule-chain-type-core'],
  ['EDGE', 'item-data.rule-chain-type-edge'],
]);

export interface MpItemVersionResource {
  id: string;
  type: string;
}

export interface MpItemVersionView {
  id: string;
  createdTime: number;
  version: string;
  publishedTime: number;
  changelog: string;
  dataDescriptor: any;
  image: string;
  icon: string;
  color: string;
  description: string;
  categories: string[];
  useCases: string[];
  itemId: string;
  creatorId: string;
  name: string;
  type: ItemType;
  peOnly: boolean;
  tags: string[];
  creatorDisplayName: string;
  creatorWebsite: string;
  creatorContactEmail: string;
  creatorDescription: string;
  creatorAvatarUrl: string;
  creatorVerified: boolean;
  installCount: number;
  totalInstallCount: number;
  /**
   * Server-owned, read-only marker for content that already ships inside ThingsBoard
   * (a bundled widget, a SCADA symbol) instead of being installed by the IoT Hub.
   * Optional on purpose: older Hub deployments simply omit the field, so it must never
   * be read directly — use `isBuiltInItem()` from `@home/components/iot-hub/iot-hub-utils`.
   * Never send it back to the server.
   */
  builtIn?: boolean;
  resources: MpItemVersionResource[];
  relatedItems?: string[];
  checksum?: string;
}

/**
 * One item type's share of a grouped search: the rows to show and how many exist behind them.
 * A grouped answer is a list of these, in the server's section order, and a type nothing matched
 * has no section — so a "+N more" header is `total - items.length`.
 */
export interface MpItemVersionSection {
  itemType: ItemType;
  total: number;
  items: MpItemVersionView[];
}

// 404 body shapes returned by the public listing item-version endpoint
// (GET /api/listings/public/by-slug/{slug}/item-version) when no
// version matches the caller's edition / platform combination.
export interface ListingItemVersionNotFound {
  noMatchingVersions?: boolean;
  peRequired?: boolean;
  minTbVersionRequired?: number;
}

export interface MpItemVersionQueryOptions {
  /** Single item type, for a surface pinned to one (the type pages, the add-item dialog). */
  type?: string;
  /**
   * Several item types at once, for a cross-type surface whose Type facet is multi-select.
   * Emitted as a repeated `type` parameter, which is the shape the backend reads
   * (`@RequestParam List<ItemType> type`). Kept separate from `type` rather than widening it,
   * so a caller that means "exactly this type" cannot be handed an array by accident.
   */
  types?: string[];
  peOnly?: boolean;
  creatorId?: string;
  categories?: string[];
  useCases?: string[];
  cfTypes?: string[];
  widgetTypes?: string[];
  ruleChainTypes?: string[];
  tbVersion?: number;
  hardwareTypes?: string[];
  connectivity?: string[];
  vendors?: string[];
  scadaFirst?: boolean;
}

/** Every filter, as `&name=value` pairs - shared by both query shapes below. */
function filtersToQuery(o: MpItemVersionQueryOptions): string {
  let query = '';
  if (o.type) {
    query += `&type=${encodeURIComponent(o.type)}`;
  }
  if (o.types?.length) {
    query += o.types.map(t => `&type=${encodeURIComponent(t)}`).join('');
  }
  if (o.peOnly != null) {
    query += `&peOnly=${o.peOnly}`;
  }
  if (o.creatorId) {
    query += `&creatorId=${encodeURIComponent(o.creatorId)}`;
  }
  if (o.categories?.length) {
    query += o.categories.map(c => `&categories=${encodeURIComponent(c)}`).join('');
  }
  if (o.useCases?.length) {
    query += o.useCases.map(u => `&useCases=${encodeURIComponent(u)}`).join('');
  }
  if (o.cfTypes?.length) {
    query += o.cfTypes.map(t => `&cfTypes=${encodeURIComponent(t)}`).join('');
  }
  if (o.widgetTypes?.length) {
    query += o.widgetTypes.map(t => `&widgetTypes=${encodeURIComponent(t)}`).join('');
  }
  if (o.ruleChainTypes?.length) {
    query += o.ruleChainTypes.map(t => `&ruleChainTypes=${encodeURIComponent(t)}`).join('');
  }
  if (o.tbVersion != null) {
    query += `&tbVersion=${o.tbVersion}`;
  }
  if (o.hardwareTypes?.length) {
    query += o.hardwareTypes.map(ht => `&hardwareTypes=${encodeURIComponent(ht)}`).join('');
  }
  if (o.connectivity?.length) {
    query += o.connectivity.map(c => `&connectivity=${encodeURIComponent(c)}`).join('');
  }
  if (o.vendors?.length) {
    query += o.vendors.map(v => `&vendors=${encodeURIComponent(v)}`).join('');
  }
  if (o.scadaFirst != null) {
    query += `&scadaFirst=${o.scadaFirst}`;
  }
  return query;
}

/** A paged read: the filters, and the page to cut out of them. */
export class MpItemVersionQuery {
  constructor(public pageLink: PageLink, public options: MpItemVersionQueryOptions = {}) {}

  public toQuery(): string {
    return this.pageLink.toQuery() + filtersToQuery(this.options);
  }
}

/**
 * A sectioned read: the filters, what to search for, and which key orders a section.
 *
 * No `PageLink`, because the grouped endpoint takes no page and no page size - it sizes its own
 * answer from the section limit - and no sort direction either, each key's direction being fixed
 * by the chain the server switches on. A page link here could only carry values nobody sends.
 */
export class MpItemVersionGroupedQuery {
  constructor(public options: MpItemVersionQueryOptions = {},
              public textSearch?: string,
              public sortProperty?: string) {}

  public toQuery(): string {
    const parts: string[] = [];
    const text = this.textSearch?.trim();
    if (text?.length) {
      parts.push(`textSearch=${encodeURIComponent(text)}`);
    }
    if (this.sortProperty) {
      parts.push(`sortProperty=${this.sortProperty}`);
    }
    // filtersToQuery() emits leading ampersands, which is what a page link needs in front of it;
    // here they may be the whole query string, so the first one is dropped.
    return `?${`${parts.join('&')}${filtersToQuery(this.options)}`.replace(/^&/, '')}`;
  }
}
