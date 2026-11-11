// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { BaseData, HasId } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { SecretStorageId } from '@shared/models/id/secret-storage-id';
import { ResourceReferences, TbResourceInfo } from '@shared/models/resource.models';
import { isNotEmptyStr } from '@core/utils';
import { WhiteLabeling } from '@shared/models/white-labeling.models';

export interface SecretStorage extends BaseData<SecretStorageId> {
  tenantId?: TenantId;
  name: string;
  type: SecretStorageType;
  description?: any;
}

export interface SecretStorageInfo extends SecretStorage {
  value: string;
}

export enum SecretStorageType {
  TEXT = 'TEXT',
  TEXT_FILE = 'TEXT_FILE'
}

export const secretStorageTypeTranslationMap = new Map<SecretStorageType, string>(
  [
    [SecretStorageType.TEXT, 'secret-storage.types.text'],
    [SecretStorageType.TEXT_FILE, 'secret-storage.types.file'],
  ]
);

export const secretStorageCreateTitleTranslationMap = new Map<SecretStorageType, string>(
  [
    [SecretStorageType.TEXT, 'secret-storage.create.text'],
    [SecretStorageType.TEXT_FILE, 'secret-storage.create.file'],
  ]
);

export interface SecretWithReferences extends SecretStorage {
  references: any;
}

export interface SecretDeleteResult {
  resource: SecretStorage;
  success: boolean;
  resourceIsReferencedError?: boolean;
  error?: any;
  references?: ResourceReferences;
}

export type SecretResourceInfo = TbResourceInfo<SecretStorage>;

export const toSecretDeleteResult = (resource: SecretStorage, e?: any): SecretDeleteResult => {
  if (!e) {
    return {resource, success: true};
  } else {
    const result: SecretDeleteResult = {resource, success: false, error: e};
    if (e?.status === 400 && e?.error?.success === false && e?.error?.references) {
      const entityReferences: {[entityType: string]: Array<BaseData<HasId>>} = e?.error?.references;
      const references: ResourceReferences = [];
      if (entityReferences) {
        for (const entityTypeStr of Object.keys(entityReferences)) {
          const entities = entityReferences[entityTypeStr];
          references.push.apply(references, entities);
        }
      }
      result.resourceIsReferencedError = true;
      result.references = references;
    }
    return result;
  }
};

export const  parseSecret = (str: string) => {
  if (isNotEmptyStr(str)) {
    const regex = /^\${secret:([^;]+);type:[^}]+}$/;
    const match = str.match(regex);
    return match ? match[1] : null;
  }
  return null;
}

