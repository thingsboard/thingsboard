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
  installCount: number;
  totalInstallCount: number;
}

export class MpItemVersionQuery {
  constructor(
    public pageLink: PageLink,
    public type?: string,
    public peOnly?: boolean,
    public creatorId?: string,
    public categories?: string[],
    public useCases?: string[],
    public cfTypes?: string[],
    public widgetTypes?: string[],
    public ruleChainTypes?: string[],
    public tbVersion?: number
  ) {}

  public toQuery(): string {
    let query = this.pageLink.toQuery();
    if (this.type) {
      query += `&type=${this.type}`;
    }
    if (this.peOnly != null) {
      query += `&peOnly=${this.peOnly}`;
    }
    if (this.creatorId) {
      query += `&creatorId=${this.creatorId}`;
    }
    if (this.categories?.length) {
      query += this.categories.map(c => `&categories=${encodeURIComponent(c)}`).join('');
    }
    if (this.useCases?.length) {
      query += this.useCases.map(u => `&useCases=${encodeURIComponent(u)}`).join('');
    }
    if (this.cfTypes?.length) {
      query += this.cfTypes.map(t => `&cfTypes=${encodeURIComponent(t)}`).join('');
    }
    if (this.widgetTypes?.length) {
      query += this.widgetTypes.map(t => `&widgetTypes=${encodeURIComponent(t)}`).join('');
    }
    if (this.ruleChainTypes?.length) {
      query += this.ruleChainTypes.map(t => `&ruleChainTypes=${encodeURIComponent(t)}`).join('');
    }
    if (this.tbVersion != null) {
      query += `&tbVersion=${this.tbVersion}`;
    }
    return query;
  }
}
