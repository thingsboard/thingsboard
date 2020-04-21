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

import { ActivatedRouteSnapshot, Params } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

export interface BreadCrumb {
  label: string;
  labelFunction?: () => string;
  ignoreTranslate: boolean;
  icon: string;
  isMdiIcon: boolean;
  link: any[];
  queryParams: Params;
}

export type BreadCrumbLabelFunction<C> = (route: ActivatedRouteSnapshot, translate: TranslateService, component: C, data?: any) => string;

export interface BreadCrumbConfig<C> {
  labelFunction: BreadCrumbLabelFunction<C>;
  label: string;
  icon: string;
  skip: boolean;
}
