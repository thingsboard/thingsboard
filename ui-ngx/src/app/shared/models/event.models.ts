///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { BaseData } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { EntityId } from '@shared/models/id/entity-id';
import { EventId } from './id/event-id';
import { ContentType } from '@shared/models/constants';

export enum EventType {
  ERROR = 'ERROR',
  LC_EVENT = 'LC_EVENT',
  STATS = 'STATS',
  EDGE_EVENT = 'EDGE_EVENT'
}

export enum DebugEventType {
  DEBUG_RULE_NODE = 'DEBUG_RULE_NODE',
  DEBUG_RULE_CHAIN = 'DEBUG_RULE_CHAIN'
}
//TODO deaflynx filter CE only event types
export enum EdgeEventType {
  DASHBOARD = "DASHBOARD",
  ASSET = "ASSET",
  DEVICE = "DEVICE",
  ENTITY_VIEW = "ENTITY_VIEW",
  ALARM = "ALARM",
  RULE_CHAIN = "RULE_CHAIN",
  RULE_CHAIN_METADATA = "RULE_CHAIN_METADATA",
  EDGE = "EDGE",
  USER = "USER",
  CUSTOMER = "CUSTOMER",
  RELATION = "RELATION",
  ENTITY_GROUP = "ENTITY_GROUP",
  SCHEDULER_EVENT = "SCHEDULER_EVENT",
  WHITE_LABELING = "WHITE_LABELING",
  LOGIN_WHITE_LABELING = "LOGIN_WHITE_LABELING",
  CUSTOM_TRANSLATION = "CUSTOM_TRANSLATION",
  WIDGETS_BUNDLE = "WIDGETS_BUNDLE",
  WIDGET_TYPE = "WIDGET_TYPE",
  ADMIN_SETTINGS = "ADMIN_SETTINGS"
}

export enum EdgeEventStatusColor {
  DEPLOYED = "DEPLOYED",
  PENDING = "PENDING"
}

export const edgeEventStatusColor = new Map<EdgeEventStatusColor, string> (
  [
    [EdgeEventStatusColor.DEPLOYED, '#000000'],
    [EdgeEventStatusColor.PENDING, '#9e9e9e']
  ]
);

export const eventTypeTranslations = new Map<EventType | DebugEventType, string>(
  [
    [EventType.ERROR, 'event.type-error'],
    [EventType.LC_EVENT, 'event.type-lc-event'],
    [EventType.STATS, 'event.type-stats'],
    [EventType.EDGE_EVENT, 'event.type-edge-event'],
    [DebugEventType.DEBUG_RULE_NODE, 'event.type-debug-rule-node'],
    [DebugEventType.DEBUG_RULE_CHAIN, 'event.type-debug-rule-chain'],
  ]
);

export interface BaseEventBody {
  server: string;
}

export interface ErrorEventBody extends BaseEventBody {
  method: string;
  error: string;
}

export interface LcEventEventBody extends BaseEventBody {
  event: string;
  success: boolean;
  error: string;
}

export interface StatsEventBody extends BaseEventBody {
  messagesProcessed: number;
  errorsOccurred: number;
}

export interface EdgeEventBody extends BaseEventBody {
  type: string;
  action: string;
  entityId: string;
}

export interface DebugRuleNodeEventBody extends BaseEventBody {
  type: string;
  entityId: string;
  entityName: string;
  msgId: string;
  msgType: string;
  relationType: string;
  dataType: ContentType;
  data: string;
  metadata: string;
  error: string;
}

export type EventBody = ErrorEventBody & LcEventEventBody & StatsEventBody & DebugRuleNodeEventBody & EdgeEventBody;

export interface Event extends BaseData<EventId> {
  tenantId: TenantId;
  entityId: EntityId;
  type: string;
  uid: string;
  body: EventBody;
  action: string; //TODO: refactor edgeEvents - move parameters to the entity.body
}
