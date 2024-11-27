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

import { forkJoin, from, map, Observable, of, ReplaySubject, switchMap } from 'rxjs';
import { removeTbResourcePrefix } from '@shared/models/resource.models';
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptionsFromConfig } from '@core/http/http-utils';

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
    return true;
  }
}

export const compileTbFunction = (http: HttpClient, tbFunction: TbFunction, ...args: string[]): Observable<CompiledTbFunction> => {
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
      return new CompiledTbFunction(compiledFunction, compiledModules);
    })
  );
}

export class CompiledTbFunction {

  constructor(private compiledFunction: Function,
              private compiledModules: System.Module[]) {
  }

  execute(...args: any[]): any {
    let functionArgs: any[];
    if (this.compiledModules?.length) {
      functionArgs = args.concat(this.compiledModules);
    } else {
      functionArgs = args;
    }
    return this.compiledFunction(...functionArgs);
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
      switchMap((response) => {
        const objectURL = URL.createObjectURL(response.body);
        const asyncModule = from(import(/* @vite-ignore */objectURL));
        URL.revokeObjectURL(objectURL);
        return asyncModule;
      })
    ).subscribe(
      {
        next: (value) => {
          request.next(value);
          request.complete();
        },
        error: err => {
          request.error(err);
        },
        complete: () => {
          delete modulesLoading[url];
        }
      }
    );
  }
  return request;
}
