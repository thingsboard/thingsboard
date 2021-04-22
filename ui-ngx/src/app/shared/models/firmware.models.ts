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
import { FirmwareId } from '@shared/models/id/firmware-id';

export enum ChecksumAlgorithm {
  MD5 = 'md5',
  SHA256 = 'sha256',
  CRC32 = 'crc32'
}

export const ChecksumAlgorithmTranslationMap = new Map<ChecksumAlgorithm, string>(
  [
    [ChecksumAlgorithm.MD5, 'MD5'],
    [ChecksumAlgorithm.SHA256, 'SHA-256'],
    [ChecksumAlgorithm.CRC32, 'CRC-32']
  ]
);

export interface FirmwareInfo extends BaseData<FirmwareId> {
  tenantId?: TenantId;
  title?: string;
  version?: string;
  hasData?: boolean;
  additionalInfo?: any;
}

export interface Firmware extends FirmwareInfo {
  file?: File;
  data: string;
  fileName: string;
  checksum?: ChecksumAlgorithm;
  checksumAlgorithm?: string;
  contentType: string;
}
