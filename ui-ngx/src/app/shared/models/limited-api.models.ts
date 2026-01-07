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

export enum LimitedApi {
  ENTITY_EXPORT = 'ENTITY_EXPORT',
  ENTITY_IMPORT = 'ENTITY_IMPORT',
  NOTIFICATION_REQUESTS = 'NOTIFICATION_REQUESTS',
  NOTIFICATION_REQUESTS_PER_RULE = 'NOTIFICATION_REQUESTS_PER_RULE',
  REST_REQUESTS_PER_TENANT = 'REST_REQUESTS_PER_TENANT',
  REST_REQUESTS_PER_CUSTOMER = 'REST_REQUESTS_PER_CUSTOMER',
  WS_UPDATES_PER_SESSION = 'WS_UPDATES_PER_SESSION',
  CASSANDRA_WRITE_QUERIES_CORE = 'CASSANDRA_WRITE_QUERIES_CORE',
  CASSANDRA_READ_QUERIES_CORE = 'CASSANDRA_READ_QUERIES_CORE',
  CASSANDRA_WRITE_QUERIES_RULE_ENGINE = 'CASSANDRA_WRITE_QUERIES_RULE_ENGINE',
  CASSANDRA_READ_QUERIES_RULE_ENGINE = 'CASSANDRA_READ_QUERIES_RULE_ENGINE',
  CASSANDRA_WRITE_QUERIES_MONOLITH = 'CASSANDRA_WRITE_QUERIES_MONOLITH',
  CASSANDRA_READ_QUERIES_MONOLITH = 'CASSANDRA_READ_QUERIES_MONOLITH',
  TRANSPORT_MESSAGES_PER_TENANT = 'TRANSPORT_MESSAGES_PER_TENANT',
  TRANSPORT_MESSAGES_PER_DEVICE = 'TRANSPORT_MESSAGES_PER_DEVICE',
  TRANSPORT_MESSAGES_PER_GATEWAY = 'TRANSPORT_MESSAGES_PER_GATEWAY',
  TRANSPORT_MESSAGES_PER_GATEWAY_DEVICE = 'TRANSPORT_MESSAGES_PER_GATEWAY_DEVICE',
  EDGE_EVENTS = 'EDGE_EVENTS',
  EDGE_EVENTS_PER_EDGE = 'EDGE_EVENTS_PER_EDGE',
  EDGE_UPLINK_MESSAGES = 'EDGE_UPLINK_MESSAGES',
  EDGE_UPLINK_MESSAGES_PER_EDGE = 'EDGE_UPLINK_MESSAGES_PER_EDGE'
}

export const LimitedApiTranslationMap = new Map<LimitedApi, string>(
  [
    [LimitedApi.ENTITY_EXPORT, 'api-limit.entity-version-creation'],
    [LimitedApi.ENTITY_IMPORT, 'api-limit.entity-version-load'],
    [LimitedApi.NOTIFICATION_REQUESTS, 'api-limit.notification-requests'],
    [LimitedApi.NOTIFICATION_REQUESTS_PER_RULE, 'api-limit.notification-requests-per-rule'],
    [LimitedApi.REST_REQUESTS_PER_TENANT, 'api-limit.rest-api-requests'],
    [LimitedApi.REST_REQUESTS_PER_CUSTOMER, 'api-limit.rest-api-requests-per-customer'],
    [LimitedApi.WS_UPDATES_PER_SESSION, 'api-limit.ws-updates-per-session'],
    [LimitedApi.CASSANDRA_WRITE_QUERIES_CORE, 'api-limit.cassandra-write-queries-core'],
    [LimitedApi.CASSANDRA_READ_QUERIES_CORE, 'api-limit.cassandra-read-queries-core'],
    [LimitedApi.CASSANDRA_WRITE_QUERIES_RULE_ENGINE, 'api-limit.cassandra-write-queries-rule-engine'],
    [LimitedApi.CASSANDRA_READ_QUERIES_RULE_ENGINE, 'api-limit.cassandra-read-queries-rule-engine'],
    [LimitedApi.CASSANDRA_WRITE_QUERIES_MONOLITH, 'api-limit.cassandra-write-queries-monolith'],
    [LimitedApi.CASSANDRA_READ_QUERIES_MONOLITH, 'api-limit.cassandra-read-queries-monolith'],
    [LimitedApi.TRANSPORT_MESSAGES_PER_TENANT, 'api-limit.transport-messages'],
    [LimitedApi.TRANSPORT_MESSAGES_PER_DEVICE, 'api-limit.transport-messages-per-device'],
    [LimitedApi.TRANSPORT_MESSAGES_PER_GATEWAY, 'api-limit.transport-messages-per-gateway'],
    [LimitedApi.TRANSPORT_MESSAGES_PER_GATEWAY_DEVICE, 'api-limit.transport-messages-per-gateway-device'],
    [LimitedApi.EDGE_EVENTS, 'api-limit.edge-events'],
    [LimitedApi.EDGE_EVENTS_PER_EDGE, 'api-limit.edge-events-per-edge'],
    [LimitedApi.EDGE_UPLINK_MESSAGES, 'api-limit.edge-uplink-messages'],
    [LimitedApi.EDGE_UPLINK_MESSAGES_PER_EDGE, 'api-limit.edge-uplink-messages-per-edge']
  ]
);
