// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { BaseData } from '@shared/models/base-data';
import { DeviceCertificateRevocationId } from '@shared/models/id/device-certificate-revocation-id';
import { DeviceId } from '@shared/models/id/device-id';
import { HasTenantId } from '@shared/models/entity.models';
import { UserId } from '@shared/models/id/user-id';

export interface DeviceCertificateRevocation extends BaseData<DeviceCertificateRevocationId>, HasTenantId {
  deviceId?: DeviceId;
  certificateHash: string;
  serialNumber: string;
  issuer: string;
  subject: string;
  notBefore: number;
  notAfter: number;
  reason?: string;
  revokedBy?: UserId;
  expired: boolean;
}

export interface DeviceCertificateRevocationInfo extends DeviceCertificateRevocation {
  deviceName?: string;
}

export interface DeviceCertificateRevocationRequest {
  certificate?: string;
  reason?: string;
}

export interface DeviceCertificateInfo {
  subject: string;
  issuer: string;
  serialNumber: string;
  notBefore: number;
  notAfter: number;
  sha256Fingerprint: string;
  revocation?: DeviceCertificateRevocation;
}
