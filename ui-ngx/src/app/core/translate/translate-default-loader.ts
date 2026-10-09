// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { TranslateLoader, TranslationObject } from '@ngx-translate/core';
import { forkJoin, Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { catchError, map } from 'rxjs/operators';
import { environment as env } from '@env/environment';
import { mergeDeep } from '@core/utils';

@Injectable({ providedIn: 'root' })
export class TranslateDefaultLoader implements TranslateLoader {

  isSetupCompleted = false;
  isAuthenticated = false;

  constructor(private http: HttpClient) {

  }

  getTranslation(lang: string): Observable<TranslationObject> {
    let tasks: Array<Observable<TranslationObject>>
    if (this.isSetupCompleted) {
      if (this.isAuthenticated) {
        tasks = [this.http.get<TranslationObject>(`/api/translation/full/${lang}`)];
      } else {
        tasks = [this.http.get<TranslationObject>(`/api/noauth/translation/login/${lang}`)];
      }
      if (!env.production && env.supportedLangs && env.supportedLangs.indexOf(lang) !== -1) {
        tasks.push(this.http.get<TranslationObject>(`/assets/locale/locale.constant-${lang}.json`));
      }
    } else {
      return this.loadSystemLang(lang);
    }
    const observe = forkJoin(tasks).pipe(
      map((results) => {
        if (results.length > 1) {
          return mergeDeep({}, results[0], results[1]);
        }
        return results[0];
      })
    );
    return observe.pipe(
      catchError(() => this.loadSystemLang(lang))
    );
  }

  private loadSystemLang(lang: string): Observable<TranslationObject> {
    const tasks = [
      this.http.get<TranslationObject>(`/assets/locale/locale.constant-${env.defaultLang}.json`)
    ];
    if (env.supportedLangs && env.supportedLangs.indexOf(lang) !== -1) {
      tasks.push(this.http.get<TranslationObject>(`/assets/locale/locale.constant-${lang}.json`));
    }
    return forkJoin(tasks).pipe(
      map((results) => {
        if (results.length > 1) {
          return mergeDeep({}, results[0], results[1]);
        }
        return results[0];
      })
    );
  }
}
