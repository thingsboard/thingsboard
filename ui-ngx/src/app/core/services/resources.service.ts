///
/// Copyright © 2016-2023 The Thingsboard Authors
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
  ComponentFactory,
  Inject,
  Injectable,
  Injector,
  ModuleWithComponentFactories,
  Type
} from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { forkJoin, Observable, ReplaySubject, throwError } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { IModulesMap } from '@modules/common/modules-map.models';

declare const System;

export interface ModulesWithFactories {
  modules: Type<any>[];
  factories: ComponentFactory<any>[];
}

@Injectable({
  providedIn: 'root'
})
export class ResourcesService {

  private loadedResources: { [url: string]: ReplaySubject<any> } = {};
  private loadedModules: { [url: string]: ReplaySubject<Type<any>[]> } = {};
  private loadedModulesAndFactories: { [url: string]: ReplaySubject<ModulesWithFactories> } = {};

  private anchor = this.document.getElementsByTagName('head')[0] || this.document.getElementsByTagName('body')[0];

  constructor(@Inject(DOCUMENT) private readonly document: any,
              private compiler: Compiler,
              private http: HttpClient,
              private injector: Injector) {}

  public loadResource(url: string): Observable<any> {
    if (this.loadedResources[url]) {
      return this.loadedResources[url].asObservable();
    }

    let fileType;
    const match = /[./](css|less|html|htm|js)?(([?#]).*)?$/.exec(url);
    if (match !== null) {
      fileType = match[1];
    }
    if (!fileType) {
      return throwError(new Error(`Unable to detect file type from url: ${url}`));
    } else if (fileType !== 'css' && fileType !== 'js') {
      return throwError(new Error(`Unsupported file type: ${fileType}`));
    }
    return this.loadResourceByType(fileType, url);
  }

  public loadFactories(url: string, modulesMap: IModulesMap): Observable<ModulesWithFactories> {
    if (this.loadedModulesAndFactories[url]) {
      return this.loadedModulesAndFactories[url].asObservable();
    }
    modulesMap.init();
    const subject = new ReplaySubject<ModulesWithFactories>();
    this.loadedModulesAndFactories[url] = subject;
    import('@angular/compiler').then(
      () => {
        System.import(url).then(
          (module) => {
            const modules = this.extractNgModules(module);
            if (modules.length) {
              const tasks: Promise<ModuleWithComponentFactories<any>>[] = [];
              for (const m of modules) {
                tasks.push(this.compiler.compileModuleAndAllComponentsAsync(m));
              }
              forkJoin(tasks).subscribe((compiled) => {
                  try {
                    const componentFactories: ComponentFactory<any>[] = [];
                    for (const c of compiled) {
                      c.ngModuleFactory.create(this.injector);
                      componentFactories.push(...c.componentFactories);
                    }
                    const modulesWithFactories: ModulesWithFactories = {
                      modules,
                      factories: componentFactories
                    };
                    this.loadedModulesAndFactories[url].next(modulesWithFactories);
                    this.loadedModulesAndFactories[url].complete();
                  } catch (e) {
                    this.loadedModulesAndFactories[url].error(new Error(`Unable to init module from url: ${url}`));
                    delete this.loadedModulesAndFactories[url];
                  }
                },
                (e) => {
                  this.loadedModulesAndFactories[url].error(new Error(`Unable to compile module from url: ${url}`));
                  delete this.loadedModulesAndFactories[url];
                });
            } else {
              this.loadedModulesAndFactories[url].error(new Error(`Module '${url}' doesn't have default export!`));
              delete this.loadedModulesAndFactories[url];
            }
          },
          (e) => {
            this.loadedModulesAndFactories[url].error(new Error(`Unable to load module from url: ${url}`));
            delete this.loadedModulesAndFactories[url];
          }
        );
      }
    );
    return subject.asObservable();
  }

  public loadModules(url: string, modulesMap: IModulesMap): Observable<Type<any>[]> {
    if (this.loadedModules[url]) {
      return this.loadedModules[url].asObservable();
    }
    modulesMap.init();
    const subject = new ReplaySubject<Type<any>[]>();
    this.loadedModules[url] = subject;
    import('@angular/compiler').then(
      () => {
        System.import(url).then(
          (module) => {
            try {
              let modules;
              try {
                modules = this.extractNgModules(module);
              } catch (e) {
                console.error(e);
              }
              if (modules && modules.length) {
                const tasks: Promise<ModuleWithComponentFactories<any>>[] = [];
                for (const m of modules) {
                  tasks.push(this.compiler.compileModuleAndAllComponentsAsync(m));
                }
                forkJoin(tasks).subscribe((compiled) => {
                    try {
                      for (const c of compiled) {
                        c.ngModuleFactory.create(this.injector);
                      }
                      this.loadedModules[url].next(modules);
                      this.loadedModules[url].complete();
                    } catch (e) {
                      this.loadedModules[url].error(new Error(`Unable to init module from url: ${url}`));
                      delete this.loadedModules[url];
                    }
                  },
                  (e) => {
                    this.loadedModules[url].error(new Error(`Unable to compile module from url: ${url}`));
                    delete this.loadedModules[url];
                  });
              } else {
                this.loadedModules[url].error(new Error(`Module '${url}' doesn't have default export or not NgModule!`));
                delete this.loadedModules[url];
              }
            } catch (e) {
              this.loadedModules[url].error(new Error(`Unable to load module from url: ${url}`));
              delete this.loadedModules[url];
            }
          },
          (e) => {
            this.loadedModules[url].error(new Error(`Unable to load module from url: ${url}`));
            delete this.loadedModules[url];
            console.error(`Unable to load module from url: ${url}`, e);
          }
        );
      }
    );
    return subject.asObservable();
  }

  private extractNgModules(module: any, modules: Type<any>[] = []): Type<any>[] {
    try {
      let potentialModules = [module];
      let currentScanDepth = 0;

      while (potentialModules.length && currentScanDepth < 10) {
        const newPotentialModules = [];
        for (const potentialModule of potentialModules) {
          if (potentialModule && ('ɵmod' in potentialModule)) {
            modules.push(potentialModule);
          } else {
            for (const k of Object.keys(potentialModule)) {
              if (!this.isPrimitive(potentialModule[k])) {
                newPotentialModules.push(potentialModule[k]);
              }
            }
          }
        }
        potentialModules = newPotentialModules;
        currentScanDepth++;
      }
    } catch (e) {
      console.log('Could not load NgModule', e);
    }
    return modules;
  }

  private isPrimitive(test) {
    return test !== Object(test);
  }

  private loadResourceByType(type: 'css' | 'js', url: string): Observable<any> {
    const subject = new ReplaySubject();
    this.loadedResources[url] = subject;
    let el;
    let loaded = false;
    switch (type) {
      case 'js':
        el = this.document.createElement('script');
        el.type = 'text/javascript';
        el.async = false;
        el.src = url;
        break;
      case 'css':
        el = this.document.createElement('link');
        el.type = 'text/css';
        el.rel = 'stylesheet';
        el.href = url;
        break;
    }
    el.onload = el.onreadystatechange = (e) => {
      if (el.readyState && !/^c|loade/.test(el.readyState) || loaded) { return; }
      el.onload = el.onreadystatechange = null;
      loaded = true;
      this.loadedResources[url].next();
      this.loadedResources[url].complete();
    };
    el.onerror = () => {
      this.loadedResources[url].error(new Error(`Unable to load ${url}`));
      delete this.loadedResources[url];
    };
    this.anchor.appendChild(el);
    return subject.asObservable();
  }
}
