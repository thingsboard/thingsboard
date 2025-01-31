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
import { EntityId } from '@shared/models/id/entity-id';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';

export interface CalculatedField extends Omit<BaseData<CalculatedFieldId>, 'label'>, HasVersion, HasTenantId {
  debugSettings?: EntityDebugSettings;
  externalId?: string;
  configuration: CalculatedFieldConfiguration;
  type: CalculatedFieldType;
  entityId: EntityId;
}

export enum CalculatedFieldType {
  SIMPLE = 'SIMPLE',
  SCRIPT = 'SCRIPT',
}

export const CalculatedFieldTypeTranslations = new Map<CalculatedFieldType, string>(
  [
    [CalculatedFieldType.SIMPLE, 'calculated-fields.type.simple'],
    [CalculatedFieldType.SCRIPT, 'calculated-fields.type.script'],
  ]
)

export interface CalculatedFieldConfiguration {
  type: CalculatedFieldType;
  expression: string;
  arguments: Record<string, CalculatedFieldArgument>;
}

export enum ArgumentEntityType {
  Current = 'CURRENT',
  Device = 'DEVICE',
  Asset = 'ASSET',
  Customer = 'CUSTOMER',
  Tenant = 'TENANT',
}

export const ArgumentEntityTypeTranslations = new Map<ArgumentEntityType, string>(
  [
    [ArgumentEntityType.Current, 'calculated-fields.argument-current'],
    [ArgumentEntityType.Device, 'calculated-fields.argument-device'],
    [ArgumentEntityType.Asset, 'calculated-fields.argument-asset'],
    [ArgumentEntityType.Customer, 'calculated-fields.argument-customer'],
    [ArgumentEntityType.Tenant, 'calculated-fields.argument-tenant'],
  ]
)

export enum ArgumentType {
  Attribute = 'ATTRIBUTE',
  LatestTelemetry = 'TS_LATEST',
  Rolling = 'TS_ROLLING',
}

export enum OutputType {
  Attribute = 'ATTRIBUTES',
  Timeseries = 'TIME_SERIES',
}

export const OutputTypeTranslations = new Map<OutputType, string>(
  [
    [OutputType.Attribute, 'calculated-fields.attribute'],
    [OutputType.Timeseries, 'calculated-fields.timeseries'],
  ]
)

export const ArgumentTypeTranslations = new Map<ArgumentType, string>(
  [
    [ArgumentType.Attribute, 'calculated-fields.attribute'],
    [ArgumentType.LatestTelemetry, 'calculated-fields.latest-telemetry'],
    [ArgumentType.Rolling, 'calculated-fields.rolling'],
  ]
)

export interface CalculatedFieldArgument {
  refEntityKey: RefEntityKey;
  defaultValue?: string;
  refEntityId?: RefEntityKey;
  limit?: number;
  timeWindow?: number;
}

export interface RefEntityKey {
  key: string;
  type: ArgumentType;
  scope?: AttributeScope;
}

export interface RefEntityKey {
  entityType: ArgumentEntityType;
  id: string;
}

export interface CalculatedFieldArgumentValue extends CalculatedFieldArgument {
  argumentName: string;
}

export interface CalculatedFieldDialogData {
  value: CalculatedField;
  buttonTitle: string;
  entityId: EntityId;
  debugLimitsConfiguration: string;
  tenantId: string;
}
