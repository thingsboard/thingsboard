// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
