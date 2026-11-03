// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
