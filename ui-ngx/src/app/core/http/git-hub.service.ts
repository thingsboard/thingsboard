// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
