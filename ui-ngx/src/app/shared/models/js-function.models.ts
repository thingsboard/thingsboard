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

import { forkJoin, from, map, mergeMap, Observable, of, ReplaySubject, switchMap } from 'rxjs';
import { removeTbResourcePrefix, ResourceInfo } from '@shared/models/resource.models';
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptionsFromConfig } from '@core/http/http-utils';
import { TbEditorCompleter, TbEditorCompletion } from '@shared/models/ace/completion.models';
import { blobToText } from '@core/utils';
import { catchError, finalize } from 'rxjs/operators';
import { parseError } from '@shared/models/error.models';
import { TranslateService } from '@ngx-translate/core';

export interface TbFunctionWithModules {
  body: string;
  modules: {[alias: string]: string };
}

export type TbFunction = string | TbFunctionWithModules;

export const isNotEmptyTbFunction = (tbFunction: TbFunction): boolean => {
  if (tbFunction) {
    if (typeof tbFunction === 'string') {
      return tbFunction.trim().length > 0;
    } else {
      return tbFunction.body && tbFunction.body.trim().length > 0;
    }
  } else {
    return false;
  }
}

export const compileTbFunction = <T extends GenericFunction>(http: HttpClient, tbFunction: TbFunction, ...args: string[]): Observable<CompiledTbFunction<T>> => {
  let functionBody: string;
  let functionArgs: string[];
  let modules: {[alias: string]: string };
  if (typeof tbFunction === 'string') {
    functionBody = tbFunction;
    functionArgs = args;
  } else {
    functionBody = tbFunction.body;
    modules = tbFunction.modules;
    const modulesArgs = Object.keys(tbFunction.modules);
    functionArgs = args.concat(modulesArgs);
  }
  return loadFunctionModules(http, modules).pipe(
    map((compiledModules) => {
      const compiledFunction = new Function(...functionArgs, functionBody);
      return new CompiledTbFunction<T>(compiledFunction, compiledModules);
    })
  );
}

export const loadModulesCompleter = (http: HttpClient, modules: {[alias: string]: string }): Observable<TbEditorCompleter | null> => {
  if (!modules || !Object.keys(modules).length) {
    return of(null);
  } else {
    const modulesDescription: {[alias: string]: Observable<TbEditorCompletion>} = {};
    for (const alias of Object.keys(modules)) {
      modulesDescription[alias] = loadModuleCompletion(http, modules[alias]);
    }
    return forkJoin(modulesDescription).pipe(
      map((completions) => {
        return new TbEditorCompleter(completions);
      })
    );
  }
};

export const loadModuleMarkdownDescription = (http: HttpClient, translate: TranslateService, resource: ResourceInfo): Observable<string> => {
  let description = `<div class="flex flex-col !pl-4 !pr-4"><h6>${resource.title}</h6><small>${translate.instant('js-func.module-members')}</small></div>\n\n`;
  description += '<div class="divider !pt-2"></div>\n' +
    '<br/>\n\n';
  return loadFunctionModuleWithSource(http, resource.link).pipe(
    map((moduleWithSource) => {
      const module = moduleWithSource.module;
      const propertiesData: { type: 'function' | 'const', propName: string, description: string }[] = [];
      for (const propName of Object.keys(module)) {
        let propDescription = '';
        const prop = module[propName];
        const type = typeof prop;
        if (type === 'function') {
          const funcArgs = getFunctionArguments(prop);
          propDescription += `<p class="!pl-4 !pr-4"><em>function</em> <strong>${propName}</strong> <em>(${funcArgs.join(', ')})</em>: <code>any</code></p>`;
        } else {
          propDescription += `<p class="!pl-4 !pr-4"><em>const</em> <strong>${propName}</strong>: <code>${type}</code>`;
          if (type !== 'object') {
            propDescription += ' = ' + (type === 'string' ? `"${handleHtmlSpecialChars(prop)}"` : `${prop}`);
          }
          propDescription += '</p>';
        }
        propDescription += '\n\n';
        const propertyData: { type: 'function' | 'const', propName: string, description: string } = {
          type: type === 'function' ? 'function' : 'const',
          propName,
          description: propDescription
        }
        propertiesData.push(propertyData);
      }
      propertiesData.sort((a, b) => {
        if (a.type === b.type) {
          return a.propName.localeCompare(b.propName);
        } else if (a.type === 'const') return -1;
        else return 1;
      });
      if (!propertiesData.length) {
        description += `<div class="!pl-4 !pr-4">${translate.instant('js-func.module-no-members')}</div>\n\n`;
      } else {
        propertiesData.forEach((pData) => {
          description += pData.description;
        });
      }
      return description;
    }),
    catchError(err => {
      const errorText = parseError(err);
      description += `<div class="!pl-4 !pr-4">${translate.instant('js-func.module-load-error')}:<br/><span style="color: red;">${errorText}</span></div>\n\n`;
      return of(description);
    })
  );
}

const handleHtmlSpecialChars = (text: string): string => {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

export const loadModuleMarkdownSourceCode = (http: HttpClient, translate: TranslateService, resource: ResourceInfo): Observable<string> => {
  let sourceCode = `<div class="flex flex-col !pl-4"><h6>${resource.title}</h6><small>${translate.instant('js-func.source-code')}</small></div>\n\n`;
  return loadFunctionModuleSource(http, resource.link).pipe(
    map((source) => {
      sourceCode += '```javascript\n{:code-style="margin-left: -16px; margin-right: -16px; max-height: 65vh;"}\n' +  source + '\n```';
      return sourceCode;
    }),
    catchError(err => {
      const errorText = parseError(err);
      sourceCode += `<div class="!pl-4 !pr-4">${translate.instant('js-func.source-code-load-error')}:<br/><span style="color: red;">${errorText}</span></div>\n\n`;
      return of(sourceCode);
    })
  );
}

const loadModuleCompletion = (http: HttpClient, moduleLink: string): Observable<TbEditorCompletion> => {
  return loadFunctionModule(http, moduleLink).pipe(
    map((module) => {
      const completion: TbEditorCompletion = {
        meta: 'module',
        type: 'module',
        children: {}
      };
      for (const propName of Object.keys(module)) {
        const prop = module[propName];
        const type = typeof prop;
        const propertyCompletion: TbEditorCompletion = {
          meta: type === 'function' ? 'function' : 'constant',
          type
        };
        if (type === 'function') {
          propertyCompletion.args = getFunctionArguments(prop).map(functionArg => {
            return {name: functionArg}
          });
          propertyCompletion.return = { type: 'any'};
        } else if (type !== 'object') {
          propertyCompletion.description = `<div class="tb-api-title">Constant value:</div><code class="title">${prop}</code>`;
        }
        completion.children[propName] = propertyCompletion;
      }
      return completion;
    }),
    catchError(err => {
      const completion: TbEditorCompletion = {
        meta: 'module',
        type: 'module',
        children: {}
      };
      const errorText = parseError(err);
      completion.description = `<div>Module load error:<br/><span style="color: red;">${errorText}</span></div>`;
      return of(completion);
    })
  );
}

export type GenericFunction = (...args: any[]) => any;

export class CompiledTbFunction<T extends GenericFunction> {

  public execute: T = this.executeImpl.bind(this);

  constructor(private compiledFunction: Function,
              private compiledModules: System.Module[]) {
  }

  private executeImpl(...args: any[]): any {
    let functionArgs: any[];
    if (this.compiledModules?.length) {
      functionArgs = args ? args.concat(this.compiledModules) : this.compiledModules;
    } else {
      functionArgs = args;
    }
    return this.compiledFunction(...functionArgs);
  }

  apply(thisArg: any, argArray?: any): any {
    let functionArgs: any[];
    if (this.compiledModules?.length) {
      functionArgs = argArray ? argArray.concat(this.compiledModules) : this.compiledModules;
    } else {
      functionArgs = argArray;
    }
    return this.compiledFunction.apply(thisArg, functionArgs);
  }
}

const loadFunctionModules = (http: HttpClient, modules: {[alias: string]: string }): Observable<System.Module[]> => {
  if (modules && Object.keys(modules).length) {
    const moduleObservables: Observable<System.Module>[] = [];
    for (const alias of Object.keys(modules)) {
      moduleObservables.push(loadFunctionModule(http, modules[alias]));
    }
    return forkJoin(moduleObservables);
  } else {
    return of([]);
  }
}

const modulesLoading: {[url: string]: ReplaySubject<System.Module>} = {};

const loadFunctionModule = (http: HttpClient, moduleLink: string): Observable<System.Module> => {
  const url = removeTbResourcePrefix(moduleLink);
  let request: ReplaySubject<System.Module>;
  if (modulesLoading[url]) {
    request = modulesLoading[url];
  } else {
    request = new ReplaySubject<System.Module>(1);
    modulesLoading[url] = request;
    const options = defaultHttpOptionsFromConfig({ignoreLoading: true, ignoreErrors: true});
    http.get(url, {...options, ...{ observe: 'response', responseType: 'blob' } }).pipe(
      mergeMap((response) => {
        const objectURL = URL.createObjectURL(response.body);
        const asyncModule = from(import(/* @vite-ignore */objectURL));
        URL.revokeObjectURL(objectURL);
        return asyncModule;
      }),
      finalize(() => {
        delete modulesLoading[url];
      })
    ).subscribe(
      {
        next: (value) => {
          request.next(value);
          request.complete();
        },
        error: err => {
          request.error(err);
        }
      }
    );
  }
  return request;
}

interface TbModuleWithSource {
  module: System.Module;
  source: string;
}

const loadFunctionModuleWithSource = (http: HttpClient, moduleLink: string): Observable<TbModuleWithSource> => {
  const url = removeTbResourcePrefix(moduleLink);
  const options = defaultHttpOptionsFromConfig({ignoreLoading: true, ignoreErrors: true});
  return http.get(url, {...options, ...{ observe: 'response', responseType: 'blob' } }).pipe(
    switchMap((response) => {
      const objectURL = URL.createObjectURL(response.body);
      const asyncModule = from(import(/* @vite-ignore */objectURL));
      URL.revokeObjectURL(objectURL);
      const asyncSource = blobToText(response.body);
      return forkJoin({
        module: asyncModule,
        source: asyncSource
      });
    }));
}

const loadFunctionModuleSource = (http: HttpClient, moduleLink: string): Observable<string> => {
  const url = removeTbResourcePrefix(moduleLink);
  const options = defaultHttpOptionsFromConfig({ignoreLoading: true, ignoreErrors: true});
  return http.get(url, {...options, ...{ responseType: 'text' } });
}

const getFunctionArguments = (func: Function): string[] => {
  const fnStr = func.toString().replace(/((\/\/.*$)|(\/\*[\s\S]*?\*\/))/mg, '');
  const firstBracketIndex = fnStr.indexOf('(');
  const secondBracketIndex = fnStr.indexOf(')');
  if (firstBracketIndex === -1 || secondBracketIndex === -1 || (secondBracketIndex - firstBracketIndex) <= 1) {
    return [];
  }
  const match = fnStr.slice(firstBracketIndex+1, secondBracketIndex).match(/([^\s,]+)/g);
  if (match) {
    return new Array<string>(...match);
  } else {
    return [];
  }
}
