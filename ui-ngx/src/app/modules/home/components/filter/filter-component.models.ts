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

import { ComplexFilterPredicateInfo, EntityKeyValueType } from '@shared/models/query/query.models';

export interface ComplexFilterPredicateDialogData {
  complexPredicate: ComplexFilterPredicateInfo;
  key: string;
  readonly: boolean;
  isAdd: boolean;
  valueType: EntityKeyValueType;
  displayUserParameters: boolean;
  allowUserDynamicSource: boolean;
  onlyUserDynamicSource: boolean;
}

