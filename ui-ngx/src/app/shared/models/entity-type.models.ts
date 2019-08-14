import {TenantId} from './id/tenant-id';

///
/// Copyright © 2016-2019 The Thingsboard Authors
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

export enum EntityType {
  TENANT = 'TENANT',
  CUSTOMER = 'CUSTOMER',
  USER = 'USER',
  DASHBOARD = 'DASHBOARD',
  ASSET = 'ASSET',
  DEVICE = 'DEVICE',
  ALARM = 'ALARM',
  RULE_CHAIN = 'RULE_CHAIN',
  RULE_NODE = 'RULE_NODE',
  ENTITY_VIEW = 'ENTITY_VIEW',
  WIDGETS_BUNDLE = 'WIDGETS_BUNDLE',
  WIDGET_TYPE = 'WIDGET_TYPE'
}

export enum AliasEntityType {
  CURRENT_CUSTOMER = 'CURRENT_CUSTOMER'
}

export interface EntityTypeTranslation {
  type: string;
  typePlural: string;
  list: string;
  nameStartsWith: string;
  details: string;
  add: string;
  noEntities: string;
  selectedEntities: string;
  search: string;
}

export interface EntityTypeResource {
  helpLinkId: string;
}

export const entityTypeTranslations = new Map<EntityType, EntityTypeTranslation>(
  [
    [
      EntityType.TENANT,
      {
        type: 'entity.type-tenant',
        typePlural: 'entity.type-tenants',
        list: 'entity.list-of-tenants',
        nameStartsWith: 'entity.tenant-name-starts-with',
        details: 'tenant.tenant-details',
        add: 'tenant.add',
        noEntities: 'tenant.no-tenants-text',
        search: 'tenant.search',
        selectedEntities: 'tenant.selected-tenants'
      }
    ],
    [
      EntityType.CUSTOMER,
      {
        type: 'entity.type-customer',
        typePlural: 'entity.type-customers',
        list: 'entity.list-of-customers',
        nameStartsWith: 'entity.customer-name-starts-with',
        details: 'customer.customer-details',
        add: 'customer.add',
        noEntities: 'customer.no-customers-text',
        search: 'customer.search',
        selectedEntities: 'customer.selected-customers'
      }
    ],
    [
      EntityType.USER,
      {
        type: 'entity.type-user',
        typePlural: 'entity.type-users',
        list: 'entity.list-of-users',
        nameStartsWith: 'entity.user-name-starts-with',
        details: 'user.user-details',
        add: 'user.add',
        noEntities: 'user.no-users-text',
        search: 'user.search',
        selectedEntities: 'user.selected-users'
      }
    ],
    [
      EntityType.DEVICE,
      {
        type: 'entity.type-device',
        typePlural: 'entity.type-devices',
        list: 'entity.list-of-devices',
        nameStartsWith: 'entity.device-name-starts-with',
        details: 'device.device-details',
        add: 'device.add',
        noEntities: 'device.no-devices-text',
        search: 'device.search',
        selectedEntities: 'device.selected-devices'
      }
    ]
  ]
);

export const entityTypeResources = new Map<EntityType, EntityTypeResource>(
  [
    [
      EntityType.TENANT,
      {
        helpLinkId: 'tenants'
      }
    ],
    [
      EntityType.CUSTOMER,
      {
        helpLinkId: 'customers'
      }
    ],
    [
      EntityType.USER,
      {
        helpLinkId: 'users'
      }
    ],
    [
      EntityType.DEVICE,
      {
        helpLinkId: 'devices'
      }
    ]
  ]
);

export interface EntitySubtype {
  tenantId: TenantId;
  entityType: EntityType;
  type: string;
}
