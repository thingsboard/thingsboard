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

import { EntityAliasSelectCallbacks } from '../alias/entity-alias-select.component.models';
import { DataKeysCallbacks } from './data-keys.component.models';
import { WidgetActionCallbacks } from './action/manage-widget-actions.component.models';
import { FilterSelectCallbacks } from '@home/components/filter/filter-select.component.models';

export type WidgetConfigCallbacks = EntityAliasSelectCallbacks & FilterSelectCallbacks & DataKeysCallbacks & WidgetActionCallbacks;
