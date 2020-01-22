///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import { EntityId } from '@shared/models/id/entity-id';
import { DataKey } from '@shared/models/widget.models';

export interface EntityData {
  id: EntityId;
  entityName: string;
  entityType?: string;
  [key: string]: any;
}

export interface EntityColumn extends DataKey {
  title: string;
}

export interface DisplayColumn {
  title: string;
  label: string;
  display: boolean;
}

export interface CellContentInfo {
  useCellContentFunction: boolean;
  cellContentFunction?: Function;
  units?: string;
  decimals?: number;
}

export interface CellStyleInfo {
  useCellStyleFunction: boolean;
  cellStyleFunction?: Function;
}

export function getEntityValue(entity: any, key: DataKey): any {
  return getDescendantProp(entity, key.label);
}

export function getDescendantProp(obj: any, path: string): any {
  return path.split('.').reduce((acc, part) => acc && acc[part], obj)
}
