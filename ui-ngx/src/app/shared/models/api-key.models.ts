// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { BaseData } from '@shared/models/base-data';
import { HasTenantId } from '@shared/models/entity.models';
import { ApiKeyId } from '@shared/models/id/api-key-id';
import { UserId } from '@shared/models/id/user-id';

export const userInfoCommand  = (baseUrl: string, apiKey: string): string => `curl -X GET "${baseUrl}/api/auth/user" -H "Content-Type: application/json" -H "X-Authorization: ApiKey ${apiKey}"`

export interface ApiKeyInfo extends BaseData<ApiKeyId>, HasTenantId {
  enabled: boolean;
  expirationTime: number;
  description: string;
  expired: boolean;
  userId: UserId;
}

export interface ApiKey extends ApiKeyInfo {
  value: string;
}
