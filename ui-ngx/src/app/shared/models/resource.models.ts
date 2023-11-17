///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { TbResourceId } from '@shared/models/id/tb-resource-id';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { BaseWidgetType } from '@shared/models/widget.models';

export enum ResourceType {
  LWM2M_MODEL = 'LWM2M_MODEL',
  PKCS_12 = 'PKCS_12',
  JKS = 'JKS',
  JS_MODULE = 'JS_MODULE',
  IMAGE = 'IMAGE'
}

export const ResourceTypeMIMETypes = new Map<ResourceType, string>(
  [
    [ResourceType.LWM2M_MODEL, 'application/xml,text/xml'],
    [ResourceType.PKCS_12, 'application/x-pkcs12'],
    [ResourceType.JKS, 'application/x-java-keystore'],
    [ResourceType.JS_MODULE, 'text/javascript,application/javascript'],
    [ResourceType.IMAGE, 'image/*']
  ]
);

export const ResourceTypeExtension = new Map<ResourceType, string>(
  [
    [ResourceType.LWM2M_MODEL, 'xml'],
    [ResourceType.PKCS_12, 'p12,pfx'],
    [ResourceType.JKS, 'jks'],
    [ResourceType.JS_MODULE, 'js']
  ]
);

export const ResourceTypeTranslationMap = new Map<ResourceType, string>(
  [
    [ResourceType.LWM2M_MODEL, 'resource.type.lwm2m-model'],
    [ResourceType.PKCS_12, 'resource.type.pkcs-12'],
    [ResourceType.JKS, 'resource.type.jks'],
    [ResourceType.JS_MODULE, 'resource.type.js-module'],
    [ResourceType.IMAGE, 'resource.type.image']
  ]
);

export interface TbResourceInfo<D> extends Omit<BaseData<TbResourceId>, 'name' | 'label'>, ExportableEntity<TbResourceId> {
  tenantId?: TenantId;
  resourceKey?: string;
  title?: string;
  resourceType: ResourceType;
  fileName: string;
  descriptor?: D;
}

export type ResourceInfo = TbResourceInfo<any>;

export interface Resource extends ResourceInfo {
  data: string;
  name?: string;
}

export interface ImageDescriptor {
  mediaType: string;
  width: number;
  height: number;
  size: number;
  etag: string;
  previewDescriptor: ImageDescriptor;
}

export type ImageResourceInfo = TbResourceInfo<ImageDescriptor>;

export type ImageResourceType = 'tenant' | 'system';

export const imageResourceType = (imageInfo: ImageResourceInfo): ImageResourceType =>
  (!imageInfo.tenantId || imageInfo.tenantId?.id === NULL_UUID) ? 'system' : 'tenant';

export const IMAGES_URL_PREFIX = '/api/images';

export const isImageResourceUrl = (url: string): boolean => url && url.startsWith(IMAGES_URL_PREFIX);

export const NO_IMAGE_DATA_URI = 'data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==';
