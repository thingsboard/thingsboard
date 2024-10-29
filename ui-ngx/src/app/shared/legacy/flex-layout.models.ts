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

import { Observable } from 'rxjs/internal/Observable';
import { from, of } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { Type } from '@angular/core';

let flexLayoutModule: any;

export function getFlexLayout(): Observable<any> {
  if (flexLayoutModule) {
    return of(flexLayoutModule);
  } else {
    return from(import('@angular/flex-layout')).pipe(
      tap((module) => {
        module.DEFAULT_CONFIG.addFlexToParent = false;
        flexLayoutModule = module;
      })
    );
  }
}

export function getFlexLayoutModule(): Observable<Type<any>> {
  return getFlexLayout().pipe(
    map(module => module.FlexLayoutModule)
  );
}
