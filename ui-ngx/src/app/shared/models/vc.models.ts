///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { ExportableEntity } from '@shared/models/base-data';
import { EntityRelation } from '@shared/models/relation.models';
import { Device, DeviceCredentials } from '@shared/models/device.models';
import { RuleChain, RuleChainMetaData } from '@shared/models/rule-chain.models';

export interface VersionCreateConfig {
  saveRelations: boolean;
}

export interface VersionLoadConfig {
  loadRelations: boolean;
}

export enum VersionCreateRequestType {
  SINGLE_ENTITY = 'SINGLE_ENTITY',
  COMPLEX = 'COMPLEX'
}

export enum VersionLoadRequestType {
  SINGLE_ENTITY = 'SINGLE_ENTITY',
  ENTITY_TYPE = 'ENTITY_TYPE'
}

export interface VersionCreateRequest {
  versionName: string;
  branch: string;
  type: VersionCreateRequestType;
}

export interface SingleEntityVersionCreateRequest extends VersionCreateRequest {
  entityId: EntityId;
  config: VersionCreateConfig;
  type: VersionCreateRequestType.SINGLE_ENTITY;
}

export interface VersionLoadRequest {
  branch: string;
  versionId: string;
  type: VersionLoadRequestType;
}

export interface SingleEntityVersionLoadRequest extends VersionLoadRequest {
  externalEntityId: EntityId;
  config: VersionLoadConfig;
  type: VersionLoadRequestType.SINGLE_ENTITY;
}

export interface BranchInfo {
  name: string;
  default: boolean;
}

export interface EntityVersion {
  timestamp: number;
  id: string;
  name: string;
}

export interface VersionCreationResult {
  version: EntityVersion;
  added: number;
  modified: number;
  removed: number;
}

export interface VersionLoadResult {
  entityType: EntityType;
  created: number;
  updated: number;
  deleted: number;
}

export interface EntityExportData<E extends ExportableEntity<EntityId>> {
  entity: E;
  entityType: EntityType;
  relations: Array<EntityRelation>;
}

export interface DeviceExportData extends EntityExportData<Device> {
  credentials: DeviceCredentials;
}

export interface RuleChainExportData extends EntityExportData<RuleChain> {
  metaData: RuleChainMetaData;
}

export interface EntityDataDiff {
  currentVersion: EntityExportData<any>;
  otherVersion: EntityExportData<any>;
  rawDiff: string;
}

export function entityExportDataToJsonString(data: EntityExportData<any>): string {
  if (!data.relations) {
    data.relations = [];
  }
  const allKeys = new Set<string>();
  JSON.stringify(data, (key, value) => (allKeys.add(key), value));
  return JSON.stringify(data, Array.from(allKeys).sort((key1, key2) => {
    if (key1 === 'relations') {
      return 1;
    } else if (key2 === 'relations') {
      return -1;
    } else {
      return 0;
    }
  }), 4);
}
