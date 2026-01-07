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

import { Injectable } from '@angular/core';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable, of } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { catchError, map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class GitHubService {

  constructor(
    private http: HttpClient
  ) { }

  public getGitHubStar(config?: RequestConfig): Observable<number> {
    return this.http.get<any>('https://api.github.com/repos/thingsboard/thingsboard', defaultHttpOptionsFromConfig(config)).pipe(
      catchError(() => of({})),
      map((res: any) => res?.stargazers_count ?? 0)
    )
  }
}
