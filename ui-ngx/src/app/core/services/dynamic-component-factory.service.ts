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

import { Component, Injectable, Type, ɵComponentDef, ɵNG_COMP_DEF } from '@angular/core';
import { from, Observable, shareReplay } from 'rxjs';
import { CommonModule } from '@angular/common';
import { map } from 'rxjs/operators';
import { guid } from '@core/utils';

@Injectable({
    providedIn: 'root'
})
export class DynamicComponentFactoryService {

  private compiler$: Observable<any> = from(import('@angular/compiler')).pipe(
    shareReplay({refCount: true, bufferSize: 1})
  );

  constructor() {
  }

  public createDynamicComponent<T>(
                     componentType: Type<T>,
                     template: string,
                     imports?: Type<any>[],
                     preserveWhitespaces?: boolean,
                     styles?: string[]): Observable<Type<T>> {
    return this.compiler$.pipe(
      map(() => {
        let componentImports: Type<any>[] = [CommonModule];
        if (imports) {
          componentImports = [...componentImports, ...imports];
        }
        const comp = this.createAndCompileDynamicComponent(componentType, template, componentImports, preserveWhitespaces, styles);
        return comp.type;
      })
    );
  }

  public destroyDynamicComponent<T>(_componentType: Type<T>) {
  }

  public getComponentDef<T>(componentType: Type<T>): ɵComponentDef<T> {
    return componentType[ɵNG_COMP_DEF];
  }

  private createAndCompileDynamicComponent<T>(componentType: Type<T>, template: string, imports: Type<any>[],
                                              preserveWhitespaces?: boolean, styles?: string[]): ɵComponentDef<T> {
    // noinspection AngularMissingOrInvalidDeclarationInModule
    const comp = Component({
      template,
      imports,
      preserveWhitespaces,
      styles,
      standalone: true,
      selector: 'tb-dynamic-component#' + guid()
    })(componentType);
    // Trigger component compilation
    return comp[ɵNG_COMP_DEF];
  }

}
