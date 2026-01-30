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

import {
  createNgModule,
  Inject,
  Injectable,
  Injector,
  Type,
  ɵComponentDef,
  ɵCssSelectorList,
  ɵNG_COMP_DEF,
  ɵNG_MOD_DEF,
  ɵNgModuleDef,
  DOCUMENT
} from '@angular/core';

import { forkJoin, from, Observable, ReplaySubject, throwError } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { IModulesMap } from '@modules/common/modules-map.models';
import { TbResourceId } from '@shared/models/id/tb-resource-id';
import { camelCase, isObject } from '@core/utils';
import { AuthService } from '@core/auth/auth.service';
import { select, Store } from '@ngrx/store';
import { selectIsAuthenticated } from '@core/auth/auth.selectors';
import { AppState } from '@core/core.state';
import { map, tap } from 'rxjs/operators';
import { RequestConfig } from '@core/http/http-utils';
import { isJSResource, removeTbResourcePrefix } from '@shared/models/resource.models';

export interface ModuleInfo {
  module: ɵNgModuleDef<any>;
  components: ɵComponentDef<any>[];
}

export interface ModulesWithComponents {
  modules: ModuleInfo[];
  standaloneComponents: ɵComponentDef<any>[];
}

export type ComponentsSelectorMap<T> = Record<string, Type<T>>;

export const flatModulesWithComponents = (modulesWithComponentsList: ModulesWithComponents[]): ModulesWithComponents => {
  const modulesWithComponents: ModulesWithComponents = {
    modules: [],
    standaloneComponents: []
  };
  for (const m of modulesWithComponentsList) {
    for (const module of m.modules) {
      if (!modulesWithComponents.modules.some(m1 => m1.module === module.module)) {
        modulesWithComponents.modules.push(module);
      }
    }
    for (const comp of m.standaloneComponents) {
      if (!modulesWithComponents.standaloneComponents.includes(comp)) {
        modulesWithComponents.standaloneComponents.push(comp);
      }
    }
  }
  return modulesWithComponents;
}

export const modulesWithComponentsToTypes = (modulesWithComponents: ModulesWithComponents): Type<any>[] =>
  [...modulesWithComponents.modules.map(m => m.module.type),
    ...modulesWithComponents.standaloneComponents.map(c => c.type)];

export const componentTypeBySelector = (modulesWithComponents: ModulesWithComponents, selector: string): Type<any> | undefined => {
  let found = modulesWithComponents.standaloneComponents.find(c => matchesSelector(c.selectors, selector));
  if (!found) {
    for (const m of modulesWithComponents.modules) {
      found = m.components.find(c => matchesSelector(c.selectors, selector));
      if (found) {
        break;
      }
    }
  }
  return found?.type;
}

const matchesSelector = (selectors: ɵCssSelectorList, selector: string) =>
  selectors.some(s => s.some(s1 => typeof s1 === 'string' && s1 === selector));

const extractSelectorFromComponent = (comp: ɵComponentDef<any>): string => {
  for (const selectors of comp.selectors) {
    for (const selector of selectors) {
      if (typeof selector === 'string') {
        return selector;
      }
    }
  }
  return null;
}

@Injectable({
  providedIn: 'root'
})
export class ResourcesService {

  private loadedJsonResources: { [url: string]: ReplaySubject<any> } = {};
  private loadedResources: { [url: string]: ReplaySubject<void> } = {};
  private loadedModulesWithComponents: { [url: string]: ReplaySubject<ModulesWithComponents> } = {};

  private anchor = this.document.getElementsByTagName('head')[0] || this.document.getElementsByTagName('body')[0];

  constructor(@Inject(DOCUMENT) private readonly document: Document,
              protected store: Store<AppState>,
              private http: HttpClient,
              private injector: Injector) {
    this.store.pipe(select(selectIsAuthenticated)).subscribe(() => this.clearModulesWithComponentsCache());
  }

  public loadJsonResource<T>(url: string, postProcess?: (data: T) => T): Observable<T> {
    if (this.loadedJsonResources[url]) {
      return this.loadedJsonResources[url].asObservable();
    }
    const subject = new ReplaySubject<any>();
    this.loadedJsonResources[url] = subject;
    const req$ = (url.endsWith('.raw') || url.endsWith('.svg') ?
      this.http.get(url, {responseType: 'text'}) : this.http.get<T>(url)) as Observable<T>;
    req$.subscribe(
      {
        next: (o) => {
          if (postProcess) {
            o = postProcess(o);
          }
          this.loadedJsonResources[url].next(o);
          this.loadedJsonResources[url].complete();
        },
        error: () => {
          this.loadedJsonResources[url].error(new Error(`Unable to load ${url}`));
          delete this.loadedJsonResources[url];
        }
      }
    );
    return subject.asObservable();
  }

  public loadResource(url: string): Observable<any> {
    if (this.loadedResources[url]) {
      return this.loadedResources[url].asObservable();
    }

    let fileType: string;
    const match = /[./](css|less|html|htm|js)?(([?#]).*)?$/.exec(url);
    if (match !== null) {
      fileType = match[1];
    }
    if (!fileType) {
      return throwError(() => new Error(`Unable to detect file type from url: ${url}`));
    } else if (fileType !== 'css' && fileType !== 'js') {
      return throwError(() => new Error(`Unsupported file type: ${fileType}`));
    }
    return this.loadResourceByType(fileType, url);
  }

  public downloadResource(downloadUrl: string, config?: RequestConfig): Observable<any> {
    return this.http.get(downloadUrl, {...config, ...{
      responseType: 'arraybuffer',
      observe: 'response'
    }}).pipe(
      map((response) => {
        const headers = response.headers;
        const filename = headers.get('x-filename');
        const contentType = headers.get('content-type');
        const linkElement = document.createElement('a');
        try {
          const blob = new Blob([response.body], {type: contentType});
          const url = URL.createObjectURL(blob);
          linkElement.setAttribute('href', url);
          linkElement.setAttribute('download', filename);
          const clickEvent = new MouseEvent('click',
            {
              view: window,
              bubbles: true,
              cancelable: false
            }
          );
          linkElement.dispatchEvent(clickEvent);
          return null;
        } catch (e) {
          throw e;
        }
      })
    );
  }

  public loadModulesWithComponents(resourceId: string | TbResourceId, modulesMap: IModulesMap): Observable<ModulesWithComponents> {
    const url = this.getDownloadUrl(resourceId);
    if (this.loadedModulesWithComponents[url]) {
      return this.loadedModulesWithComponents[url].asObservable();
    }
    const meta = this.getMetaInfo(resourceId);
    const subject = new ReplaySubject<ModulesWithComponents>();
    this.loadedModulesWithComponents[url] = subject;

    forkJoin([
      modulesMap.init(),
      from(import('@angular/compiler'))
    ]).subscribe(
      () => {
        // @ts-ignore
        System.import(url, undefined, meta).then(
          (module: any) => {
            try {
              const modulesWithComponents = this.extractModulesWithComponents(module);
              if (modulesWithComponents.modules.length || modulesWithComponents.standaloneComponents.length) {
                for (const module of modulesWithComponents.modules) {
                  createNgModule(module.module.type, this.injector);
                }
                this.loadedModulesWithComponents[url].next(modulesWithComponents);
                this.loadedModulesWithComponents[url].complete();
              } else {
                this.loadedModulesWithComponents[url].error(new Error(`Module '${url}' doesn't have exported modules or components!`));
              }
            } catch (e) {
              console.log(`Unable to parse module from url: ${url}`, e);
              this.loadedModulesWithComponents[url].error(new Error(`Unable to parse module from url: ${url}`));
            }
          },
          () => {
            this.loadedModulesWithComponents[url].error(new Error(`Unable to load module from url: ${url}`));
          }
        );
      }
    );
    return subject.asObservable().pipe(
      tap({
        next: () => System.delete(url),
        error: () => {
          delete this.loadedModulesWithComponents[url];
          System.delete(url);
        },
        complete: () => System.delete(url)
      })
    );
  }

  public extractComponentsFromModule<T>(module: any, instanceFilter?: any, isCamelCaseSelector = false): ComponentsSelectorMap<T> {
    const modulesWithComponents = this.extractModulesWithComponents(module);
    const componentMap: ComponentsSelectorMap<T> = {};

    const processComponents = (components: Array<ɵComponentDef<T>>) => {
      components.forEach(item => {
        if (instanceFilter && !(item.type.prototype instanceof instanceFilter)) {
          return;
        }
        let selector = extractSelectorFromComponent(item);
        if (isCamelCaseSelector) {
          selector = camelCase(selector);
        }
        componentMap[selector] = item.type;
      });
    };

    processComponents(modulesWithComponents.standaloneComponents);

    modulesWithComponents.modules.forEach(module => {
      processComponents(module.components);
    })
    return componentMap;
  }

  private extractModulesWithComponents(module: any,
                                       modulesWithComponents: ModulesWithComponents = {
                                         modules: [],
                                         standaloneComponents: []
                                       },
                                       visitedModules: Set<any> = new Set<any>()): ModulesWithComponents {
    if (module && ['object', 'function'].includes(typeof module) && !visitedModules.has(module)) {
      visitedModules.add(module);
      if (ɵNG_MOD_DEF in module) {
        const moduleDef: ɵNgModuleDef<any> = module[ɵNG_MOD_DEF];
        const moduleInfo: ModuleInfo = {
          module: moduleDef,
          components: []
        }
        modulesWithComponents.modules.push(moduleInfo);
        const exportsDecl = moduleDef.exports;
        let exports: Type<any>[];
        if (Array.isArray(exportsDecl)) {
          exports = exportsDecl;
        } else {
          exports = exportsDecl();
        }
        for (const element of exports) {
          if (ɵNG_COMP_DEF in element) {
            const component: ɵComponentDef<any> = element[ɵNG_COMP_DEF];
            if (!component.standalone) {
              moduleInfo.components.push(component);
            } else {
              modulesWithComponents.standaloneComponents.push(component);
            }
          } else {
            this.extractModulesWithComponents(element, modulesWithComponents, visitedModules);
          }
        }
      } else if (ɵNG_COMP_DEF in module) {
        const component: ɵComponentDef<any> = module[ɵNG_COMP_DEF];
        if (component.standalone) {
          if (!modulesWithComponents.standaloneComponents.includes(component)) {
            modulesWithComponents.standaloneComponents.push(component);
          }
        }
      } else {
        for (const k of Object.keys(module)) {
          const val = module[k];
          if (val && ['object', 'function'].includes(typeof val)) {
            this.extractModulesWithComponents(val, modulesWithComponents, visitedModules);
          }
        }
      }
    }
    return modulesWithComponents;
  }

  private loadResourceByType(type: 'css' | 'js', url: string): Observable<any> {
    const subject = new ReplaySubject<void>();
    this.loadedResources[url] = subject;
    let el: any;
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
    el.onload = el.onreadystatechange = () => {
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

  private getDownloadUrl(resourceId: string | TbResourceId): string {
    if (isObject(resourceId)) {
      return `/api/resource/js/${(resourceId as TbResourceId).id}/download`;
    }
    return removeTbResourcePrefix(resourceId as string);
  }

  private getMetaInfo(resourceId: string | TbResourceId): object {
    if (isObject(resourceId) || (typeof resourceId === 'string' && isJSResource(resourceId))) {
      return {
        additionalHeaders: {
          'X-Authorization': `Bearer ${AuthService.getJwtToken()}`
        }
      };
    }
  }

  private clearModulesWithComponentsCache() {
    this.loadedModulesWithComponents = {};
  }
}
