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

import {
  Compiler,
  Component,
  ComponentFactory,
  Injectable,
  Injector,
  NgModule,
  NgModuleRef,
  Type,
  ViewEncapsulation
} from '@angular/core';
import {
  DynamicWidgetComponent,
  DynamicWidgetComponentModule
} from '@home/components/widget/dynamic-widget.component';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { Observable, ReplaySubject } from 'rxjs';
import { HomeComponentsModule } from '../home-components.module';
import { WidgetComponentsModule } from './widget-components.module';

interface DynamicWidgetComponentModuleData {
  moduleRef: NgModuleRef<DynamicWidgetComponentModule>;
  moduleType: Type<DynamicWidgetComponentModule>;
}

@Injectable()
export class DynamicWidgetComponentFactoryService {

  private dynamicComponentModulesMap = new Map<ComponentFactory<DynamicWidgetComponent>, DynamicWidgetComponentModuleData>();

  constructor(private compiler: Compiler,
              private injector: Injector) {
  }

  public createDynamicWidgetComponentFactory(template: string): Observable<ComponentFactory<DynamicWidgetComponent>> {
    const dymamicWidgetComponentFactorySubject = new ReplaySubject<ComponentFactory<DynamicWidgetComponent>>();
    const comp = this.createDynamicWidgetComponent(template);
    // noinspection AngularInvalidImportedOrDeclaredSymbol,AngularInvalidEntryComponent
    @NgModule({
      declarations: [comp],
      entryComponents: [comp],
      imports: [CommonModule, SharedModule, WidgetComponentsModule],
    })
    class DynamicWidgetComponentInstanceModule extends DynamicWidgetComponentModule {}
    this.compiler.compileModuleAsync(DynamicWidgetComponentInstanceModule).then(
      (module) => {
        const moduleRef = module.create(this.injector);
        const factory = moduleRef.componentFactoryResolver.resolveComponentFactory(comp);
        this.dynamicComponentModulesMap.set(factory, {
          moduleRef,
          moduleType: module.moduleType
        });
        dymamicWidgetComponentFactorySubject.next(factory);
        dymamicWidgetComponentFactorySubject.complete();
      }
    ).catch(
      (e) => {
        dymamicWidgetComponentFactorySubject.error(`Failed to create dynamic widget component factory: ${e}`);
      }
    );
    return dymamicWidgetComponentFactorySubject.asObservable();
  }

  public destroyDynamicWidgetComponentFactory(factory: ComponentFactory<DynamicWidgetComponent>) {
    const moduleData = this.dynamicComponentModulesMap.get(factory);
    if (moduleData) {
      moduleData.moduleRef.destroy();
      this.compiler.clearCacheFor(moduleData.moduleType);
      this.dynamicComponentModulesMap.delete(factory);
    }
  }

  private createDynamicWidgetComponent(template: string): Type<DynamicWidgetComponent> {
    // noinspection AngularMissingOrInvalidDeclarationInModule
    @Component({
      template
    })
    class DynamicWidgetInstanceComponent extends DynamicWidgetComponent { }

    return DynamicWidgetInstanceComponent;
  }

}
