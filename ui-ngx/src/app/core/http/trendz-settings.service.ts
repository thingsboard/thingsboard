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
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { TrendzSettings } from '@shared/models/trendz-settings.models';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';

@Injectable({
  providedIn: 'root'
})
export class TrendzSettingsService {

  constructor(
    private http: HttpClient
  ) {}

  public getTrendzSettings(config?: RequestConfig): Observable<TrendzSettings> {
    return this.http.get<TrendzSettings>(`/api/trendz/settings`, defaultHttpOptionsFromConfig(config))
  }

  public saveTrendzSettings(trendzSettings: TrendzSettings, config?: RequestConfig): Observable<TrendzSettings> {
    return this.http.post<TrendzSettings>(`/api/trendz/settings`, trendzSettings, defaultHttpOptionsFromConfig(config))
  }
}
