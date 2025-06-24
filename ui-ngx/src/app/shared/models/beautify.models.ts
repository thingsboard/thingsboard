///
/// Copyright © 2016-2025 The Thingsboard Authors
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
import { unwrapModule } from '@core/utils';

let jsBeautifyModule: any;
let htmlBeautifyModule: any;
let cssBeautifyModule: any;

function loadJsBeautify(): Observable<any> {
  if (jsBeautifyModule) {
    return of(jsBeautifyModule);
  } else {
    return from(import('js-beautify/js/lib/beautify.js')).pipe(
      map((module) => unwrapModule(module)),
      tap((module) => {
        jsBeautifyModule = module;
      })
    );
  }
}

function loadHtmlBeautify(): Observable<any> {
  if (htmlBeautifyModule) {
    return of(htmlBeautifyModule);
  } else {
    return from(import('js-beautify/js/lib/beautify-html.js')).pipe(
      map((module) => unwrapModule(module)),
      tap((module) => {
        htmlBeautifyModule = module;
      })
    );
  }
}

function loadCssBeautify(): Observable<any> {
  if (cssBeautifyModule) {
    return of(cssBeautifyModule);
  } else {
    return from(import('js-beautify/js/lib/beautify-css.js')).pipe(
      map((module) => unwrapModule(module)),
      tap((module) => {
        cssBeautifyModule = module;
      })
    );
  }
}

export function beautifyJs(source: string, options?: any): Observable<string> {
  return loadJsBeautify().pipe(
    map((mod) => {
      return mod.js_beautify(source, options);
    })
  );
}

export function beautifyCss(source: string, options?: any): Observable<string> {
  return loadCssBeautify().pipe(
    map((mod) => mod.css_beautify(source, options))
  );
}

export function beautifyHtml(source: string, options?: any): Observable<string> {
  return loadHtmlBeautify().pipe(
    map((mod) => mod.html_beautify(source, options))
  );
}
