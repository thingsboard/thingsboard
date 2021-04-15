///
/// Copyright © 2016-2021 The Thingsboard Authors
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
import { TbResourceId } from '@shared/models/id/tb-resource-id';

export enum ResourceType {
  LWM2M_MODEL = 'LWM2M_MODEL',
  PKCS_12 = 'PKCS_12',
  JKS = 'JKS'
}

export const ResourceTypeMIMETypes = new Map<ResourceType, string>(
  [
    [ResourceType.LWM2M_MODEL, 'application/xml,text/xml'],
    [ResourceType.PKCS_12, 'application/x-pkcs12'],
    [ResourceType.JKS, 'application/x-java-keystore']
  ]
);

export const ResourceTypeExtension = new Map<ResourceType, string>(
  [
    [ResourceType.LWM2M_MODEL, 'xml'],
    [ResourceType.PKCS_12, 'p12,pfx'],
    [ResourceType.JKS, 'jks']
  ]
);

export const ResourceTypeTranslationMap = new Map<ResourceType, string>(
  [
    [ResourceType.LWM2M_MODEL, 'LWM2M model'],
    [ResourceType.PKCS_12, 'PKCS #12'],
    [ResourceType.JKS, 'JKS']
  ]
);

export interface ResourceInfo extends BaseData<TbResourceId> {
  tenantId?: TenantId;
  resourceKey?: string;
  title?: string;
  resourceType: ResourceType;
}

export interface Resource extends ResourceInfo {
  data: string;
  fileName: string;
}

export interface Resources extends ResourceInfo {
  data: string|string[];
  fileName: string|string[];
}
