///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { RuleNodeType } from '@shared/models/rule-node.models';

export enum ComponentType {
  ENRICHMENT = 'ENRICHMENT',
  FILTER = 'FILTER',
  TRANSFORMATION = 'TRANSFORMATION',
  ACTION = 'ACTION',
  EXTERNAL = 'EXTERNAL'
}

export enum ComponentScope {
  SYSTEM = 'SYSTEM',
  TENANT = 'TENANT'
}

export interface ComponentDescriptor {
  type: ComponentType | RuleNodeType;
  scope?: ComponentScope;
  name: string;
  clazz: string;
  configurationDescriptor?: any;
  actions?: string;
}
