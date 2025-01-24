///
/// Copyright © 2016-2024 The Thingsboard Authors
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
import { EntityDebugSettings, HasTenantId, HasVersion } from '@shared/models/entity.models';
import { BaseData } from '@shared/models/base-data';
import { CalculatedFieldId } from '@shared/models/id/calculated-field-id';

export interface CalculatedField extends BaseData<CalculatedFieldId>, HasVersion, HasTenantId {
  entityId: string;
  type: CalculatedFieldType;
  name: string;
  debugSettings?: EntityDebugSettings;
  externalId?: string;
  createdTime?: number;
  configuration: CalculatedFieldConfiguration;
}

export enum CalculatedFieldType {
  SIMPLE = 'SIMPLE',
  COMPLEX = 'COMPLEX',
}

export interface CalculatedFieldConfiguration {
  type: CalculatedFieldConfigType;
  expression: string;
  arguments: Record<string, unknown>;
}

export enum CalculatedFieldConfigType {
  SIMPLE = 'SIMPLE',
  SCRIPT = 'SCRIPT',
}
