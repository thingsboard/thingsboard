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

import {
  Compiler,
  Component,
  ComponentFactory,
  Injectable,
  Injector,
  NgModule,
  NgModuleRef,
  OnDestroy,
  Type
} from '@angular/core';
import { Observable, ReplaySubject } from 'rxjs';
import { CommonModule } from '@angular/common';

@NgModule()
export abstract class DynamicComponentModule implements OnDestroy {

  ngOnDestroy(): void {
  }

}

interface DynamicComponentModuleData {
  moduleRef: NgModuleRef<DynamicComponentModule>;
  moduleType: Type<DynamicComponentModule>;
}

@Injectable(
  {
    providedIn: 'root'
  }
)
export class DynamicComponentFactoryService {

  private dynamicComponentModulesMap = new Map<ComponentFactory<any>, DynamicComponentModuleData>();

  constructor(private compiler: Compiler,
              private injector: Injector) {
  }

  public createDynamicComponentFactory<T>(
                     componentType: Type<T>,
                     template: string,
                     modules?: Type<any>[]): Observable<ComponentFactory<T>> {
    const dymamicComponentFactorySubject = new ReplaySubject<ComponentFactory<T>>();
    import('@angular/compiler').then(
      () => {
        const comp = this.createDynamicComponent(componentType, template);
        let moduleImports: Type<any>[] = [CommonModule];
        if (modules) {
          moduleImports = [...moduleImports, ...modules];
        }
        // noinspection AngularInvalidImportedOrDeclaredSymbol
        const dynamicComponentInstanceModule = NgModule({
          declarations: [comp],
          imports: moduleImports
        })(class DynamicComponentInstanceModule extends DynamicComponentModule {});
        try {
          this.compiler.compileModuleAsync(dynamicComponentInstanceModule).then(
            (module) => {
              const moduleRef = module.create(this.injector);
              const factory = moduleRef.componentFactoryResolver.resolveComponentFactory(comp);
              this.dynamicComponentModulesMap.set(factory, {
                moduleRef,
                moduleType: module.moduleType
              });
              dymamicComponentFactorySubject.next(factory);
              dymamicComponentFactorySubject.complete();
            }
          ).catch(
            (e) => {
              dymamicComponentFactorySubject.error(e);
            }
          );
        } catch (e) {
          dymamicComponentFactorySubject.error(e);
        }
      }
    );
    return dymamicComponentFactorySubject.asObservable();
  }

  public destroyDynamicComponentFactory<T>(factory: ComponentFactory<T>) {
    const moduleData = this.dynamicComponentModulesMap.get(factory);
    if (moduleData) {
      moduleData.moduleRef.destroy();
      this.compiler.clearCacheFor(moduleData.moduleType);
      this.dynamicComponentModulesMap.delete(factory);
    }
  }

  private createDynamicComponent<T>(componentType: Type<T>, template: string): Type<T> {
    // noinspection AngularMissingOrInvalidDeclarationInModule
    return Component({
      template
    })(componentType);
  }

}
