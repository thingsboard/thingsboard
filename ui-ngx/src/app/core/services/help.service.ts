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

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';
import { Observable, of } from 'rxjs';
import { catchError, mergeMap, tap } from 'rxjs/operators';
import { helpBaseUrl } from '@shared/models/constants';

const NOT_FOUND_CONTENT = '## Not found';

@Injectable({
  providedIn: 'root'
})
export class HelpService {

  private helpBaseUrl = helpBaseUrl;

  private helpCache: {[lang: string]: {[key: string]: string}} = {};

  constructor(
    private translate: TranslateService,
    private http: HttpClient
  ) {}

  getHelpContent(key: string): Observable<string> {
    const lang = this.translate.currentLang;
    if (this.helpCache[lang] && this.helpCache[lang][key]) {
      return of(this.helpCache[lang][key]);
    } else {
      return this.loadHelpContent(lang, key).pipe(
        catchError(() => {
          const defaultLang = this.translate.getDefaultLang();
          if (lang !== defaultLang) {
            return this.loadHelpContent(defaultLang, key).pipe(
              catchError(() => {
                return of(NOT_FOUND_CONTENT);
              })
            );
          } else {
            return of(NOT_FOUND_CONTENT);
          }
        }),
        mergeMap((content) => {
          return this.processIncludes(this.processVariables(content));
        }),
        tap((content) => {
          let langContent = this.helpCache[lang];
          if (!langContent) {
            langContent = {};
            this.helpCache[lang] = langContent;
          }
          langContent[key] = content;
        })
      );
    }
  }

  private loadHelpContent(lang: string, key: string): Observable<string> {
    return this.http.get(`/assets/help/${lang}/${key}.md`, {responseType: 'text'} );
  }

  private processVariables(content: string): string {
    const baseUrlReg = /\${baseUrl}/g;
    return content.replace(baseUrlReg, this.helpBaseUrl);
  }

  private processIncludes(content: string): Observable<string> {
    const includesRule = /{% include (.*) %}/;
    const match = includesRule.exec(content);
    if (match) {
      const key = match[1];
      return this.getHelpContent(key).pipe(
        mergeMap((include) => {
          content = content.replace(match[0], include);
          return this.processIncludes(content);
        })
      );
    } else {
      return of(content);
    }
  }

}
