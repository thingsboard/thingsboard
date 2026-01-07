///
/// Copyright © 2016-2026 The Thingsboard Authors
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
