// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
