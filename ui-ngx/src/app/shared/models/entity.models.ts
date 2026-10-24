// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { EntityType } from '@shared/models/entity-type.models';
import { AttributeData, AttributeScope } from './telemetry/telemetry.models';
import { EntityId } from '@shared/models/id/entity-id';
import { DeviceCredentialMQTTBasic } from '@shared/models/device.models';
import { Lwm2mSecurityConfigModels } from '@shared/models/lwm2m-security-config.models';
import { TenantId } from '@shared/models/id/tenant-id';
import { RuleChainMetaData } from '@shared/models/rule-chain.models';
import { isLiteralObject } from '@core/utils';

export interface EntityInfo {
  name?: string;
  label?: string;
  entityType?: EntityType;
  id?: string;
  entityDescription?: string;
}

export interface EntityInfoData {
  id: EntityId;
  name: string;
}

export interface ImportEntityData {
  lineNumber: number;
  name: string;
  type: string;
  label: string;
  gateway: boolean;
  description: string;
  credential: {
    accessToken?: string;
    x509?: string;
    mqtt?: DeviceCredentialMQTTBasic;
    lwm2m?: Lwm2mSecurityConfigModels;
  };
  attributes: {
    server: AttributeData[],
    shared: AttributeData[]
  };
  timeseries: AttributeData[];
}

export interface EdgeImportEntityData extends ImportEntityData {
  secret: string;
  routingKey: string;
  cloudEndpoint: string;
  edgeLicenseKey: string;
}

export interface ImportEntitiesResultInfo {
  create?: {
    entity: number;
  };
  update?: {
    entity: number;
  };
  error?: {
    entity: number;
    errors?: string;
  };
}

export interface EntityField {
  keyName: string;
  value: string;
  name: string;
  time?: boolean;
}

export interface EntitiesKeysByQuery {
  attribute: Array<string>;
  timeseries: Array<string>;
  entityTypes: EntityType[];
}

export interface EntityKeySample {
  key: string;
  sample?: { ts: number; value: any };
}

export interface EntitiesKeysByQueryV2 {
  totalEntities: number;
  entityTypes: EntityType[];
  timeseries: EntityKeySample[];
  attributes: Partial<Record<AttributeScope, EntityKeySample[]>>;
}

export const entityFields: {[fieldName: string]: EntityField} = {
  createdTime: {
    keyName: 'createdTime',
    name: 'entity-field.created-time',
    value: 'createdTime',
    time: true
  },
  name: {
    keyName: 'name',
    name: 'entity-field.name',
    value: 'name'
  },
  type: {
    keyName: 'type',
    name: 'entity-field.type',
    value: 'type'
  },
  firstName: {
    keyName: 'firstName',
    name: 'entity-field.first-name',
    value: 'firstName'
  },
  lastName: {
    keyName: 'lastName',
    name: 'entity-field.last-name',
    value: 'lastName'
  },
  email: {
    keyName: 'email',
    name: 'entity-field.email',
    value: 'email'
  },
  title: {
    keyName: 'title',
    name: 'entity-field.title',
    value: 'title'
  },
  country: {
    keyName: 'country',
    name: 'entity-field.country',
    value: 'country'
  },
  state: {
    keyName: 'state',
    name: 'entity-field.state',
    value: 'state'
  },
  city: {
    keyName: 'city',
    name: 'entity-field.city',
    value: 'city'
  },
  address: {
    keyName: 'address',
    name: 'entity-field.address',
    value: 'address'
  },
  address2: {
    keyName: 'address2',
    name: 'entity-field.address2',
    value: 'address2'
  },
  zip: {
    keyName: 'zip',
    name: 'entity-field.zip',
    value: 'zip'
  },
  phone: {
    keyName: 'phone',
    name: 'entity-field.phone',
    value: 'phone'
  },
  label: {
    keyName: 'label',
    name: 'entity-field.label',
    value: 'label'
  },
  displayName: {
    keyName: 'displayName',
    name: 'entity-field.name',
    value: 'name'
  },
  configuration: {
    keyName: 'configuration',
    name: 'entity-field.configuration',
    value: 'configuration'
  },
  schedule: {
    keyName: 'schedule',
    name: 'entity-field.schedule',
    value: 'schedule'
  },
  originatorId: {
    keyName: 'originatorId',
    name: 'entity-field.originatorId',
    value: 'originatorId'
  },
  originatorType: {
    keyName: 'originatorType',
    name: 'entity-field.originatorType',
    value: 'originatorType'
  },
  queueName: {
    keyName: 'queueName',
    name: 'entity-field.queue-name',
    value: 'queueName'
  },
  serviceId: {
    keyName: 'serviceId',
    name: 'entity-field.service-id',
    value: 'serviceId'
  },
  ownerName: {
    keyName: 'ownerName',
    name: 'entity-field.owner-name',
    value: 'ownerName'
  },
  ownerType: {
    keyName: 'ownerType',
    name: 'entity-field.owner-type',
    value: 'ownerType'
  },
  additionalInfo: {
    keyName: 'additionalInfo',
    name: 'entity-field.additional-info',
    value: 'additionalInfo'
  },
  format: {
    keyName: 'format',
    name: 'entity-field.format',
    value: 'format'
  }
};

export interface HasTenantId {
  tenantId?: TenantId;
}

export interface HasVersion {
  version?: number;
}

export interface HasEntityDebugSettings {
  debugSettings?: EntityDebugSettings;
}

export interface EntityDebugSettings {
  failuresEnabled?: boolean;
  allEnabled?: boolean;
  allEnabledUntil?: number;
}

export interface EntityTestScriptResult {
  output: string;
  error: string;
}

export type VersionedEntity = EntityInfoData & HasVersion | RuleChainMetaData;

export enum NameConflictPolicy {
  FAIL = 'FAIL',
  UNIQUIFY = 'UNIQUIFY',
}

export enum UniquifyStrategy {
  RANDOM = 'RANDOM',
  INCREMENTAL = 'INCREMENTAL'
}

export interface SaveEntityParams {
  nameConflictPolicy?: NameConflictPolicy;
  uniquifyStrategy?: UniquifyStrategy;
  uniquifySeparator?: string;
}

export interface SaveEntityWithGroupParams extends SaveEntityParams {
  entityGroupId?: string;
  entityGroupIds?: string[];
}

export function toSaveParams<T extends SaveEntityWithGroupParams>(params: string | string[] | T ): T {
  if (!params) {
    return undefined;
  }
  if (isLiteralObject(params) && !Array.isArray(params)) {
    return params as T;
  }
  if (Array.isArray(params)) {
    return { entityGroupIds: params } as T;
  }
  return { entityGroupId: params } as T;
}
