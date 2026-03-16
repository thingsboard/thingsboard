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
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { UsageInfo } from '@shared/models/usage.models';

@Injectable({
  providedIn: 'root'
})
export class UsageInfoService {

  constructor(
    private http: HttpClient
  ) {}

  public getUsageInfo(config?: RequestConfig): Observable<UsageInfo> {
    return this.http.get<UsageInfo>('/api/usage',
      defaultHttpOptionsFromConfig(config));
  }

}
