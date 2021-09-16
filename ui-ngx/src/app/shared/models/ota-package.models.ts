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
import { OtaPackageId } from '@shared/models/id/ota-package-id';
import { DeviceProfileId } from '@shared/models/id/device-profile-id';

export enum ChecksumAlgorithm {
  MD5 = 'MD5',
  SHA256 = 'SHA256',
  SHA384 = 'SHA384',
  SHA512 = 'SHA512',
  CRC32 = 'CRC32',
  MURMUR3_32 = 'MURMUR3_32',
  MURMUR3_128 = 'MURMUR3_128'
}

export const ChecksumAlgorithmTranslationMap = new Map<ChecksumAlgorithm, string>(
  [
    [ChecksumAlgorithm.MD5, 'MD5'],
    [ChecksumAlgorithm.SHA256, 'SHA-256'],
    [ChecksumAlgorithm.SHA384, 'SHA-384'],
    [ChecksumAlgorithm.SHA512, 'SHA-512'],
    [ChecksumAlgorithm.CRC32, 'CRC-32'],
    [ChecksumAlgorithm.MURMUR3_32, 'MURMUR3-32'],
    [ChecksumAlgorithm.MURMUR3_128, 'MURMUR3-128']
  ]
);

export enum OtaUpdateType {
  FIRMWARE = 'FIRMWARE',
  SOFTWARE = 'SOFTWARE'
}

export const OtaUpdateTypeTranslationMap = new Map<OtaUpdateType, string>(
  [
    [OtaUpdateType.FIRMWARE, 'ota-update.types.firmware'],
    [OtaUpdateType.SOFTWARE, 'ota-update.types.software']
  ]
);

export interface OtaUpdateTranslation {
  label: string;
  required: string;
  noFound: string;
  noMatching: string;
  hint: string;
}

export const OtaUpdateTranslation = new Map<OtaUpdateType, OtaUpdateTranslation>(
  [
    [OtaUpdateType.FIRMWARE, {
      label: 'ota-update.assign-firmware',
      required: 'ota-update.assign-firmware-required',
      noFound: 'ota-update.no-firmware-text',
      noMatching: 'ota-update.no-firmware-matching',
      hint: 'ota-update.chose-firmware-distributed-device'
    }],
    [OtaUpdateType.SOFTWARE, {
      label: 'ota-update.assign-software',
      required: 'ota-update.assign-software-required',
      noFound: 'ota-update.no-software-text',
      noMatching: 'ota-update.no-software-matching',
      hint: 'ota-update.chose-software-distributed-device'
    }]
  ]
);

export interface OtaPagesIds {
  firmwareId?: OtaPackageId;
  softwareId?: OtaPackageId;
}

export interface OtaPackageInfo extends BaseData<OtaPackageId> {
  tenantId?: TenantId;
  type: OtaUpdateType;
  deviceProfileId?: DeviceProfileId;
  title?: string;
  version?: string;
  tag?: string;
  hasData?: boolean;
  url?: string;
  fileName: string;
  checksum?: string;
  checksumAlgorithm?: ChecksumAlgorithm;
  contentType: string;
  dataSize?: number;
  additionalInfo?: any;
  isURL?: boolean;
}

export interface OtaPackage extends OtaPackageInfo {
  file?: File;
  data: string;
}
