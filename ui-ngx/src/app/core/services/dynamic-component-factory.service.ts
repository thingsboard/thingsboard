// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
