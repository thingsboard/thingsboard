// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptions, defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { publishReplay, refCount } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class UiSettingsService {

  private helpBaseUrlObservable: Observable<string>;

  constructor(
    private http: HttpClient
  ) { }

  public getHelpBaseUrl(): Observable<string> {
    if (!this.helpBaseUrlObservable) {
      this.helpBaseUrlObservable = this.http.get('/api/uiSettings/helpBaseUrl', {responseType: 'text', ...defaultHttpOptions(true)}).pipe(
        publishReplay(1),
        refCount()
      );
    }
    return this.helpBaseUrlObservable;
  }
}
