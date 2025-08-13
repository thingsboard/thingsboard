///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { InjectionToken, Type } from '@angular/core';
import { ComponentType } from '@angular/cdk/portal';

export const HELP_MARKDOWN_COMPONENT_TOKEN: InjectionToken<ComponentType<any>> =
  new InjectionToken<ComponentType<any>>('HELP_MARKDOWN_COMPONENT_TOKEN');

export const SHARED_MODULE_TOKEN: InjectionToken<Type<any>> =
  new InjectionToken<Type<any>>('SHARED_MODULE_TOKEN');
