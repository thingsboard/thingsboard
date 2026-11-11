// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
  TrendzConfiguration,
  TrendzHealthcheckResult,
  TrendzSummary,
  TrendzSynchronization
} from '@app/shared/models/trendz-analytics.models';
import { Observable } from 'rxjs';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';

@Injectable({
  providedIn: 'root'
})
export class TrendzService {

  constructor(private http: HttpClient) { }

  public getTrendzSummary(config?: RequestConfig): Observable<TrendzSummary> {
    return this.http.get<TrendzSummary>(`/api/trendz/summary`, defaultHttpOptionsFromConfig(config));
  }

  public performTrendzHealthcheck(config?: RequestConfig): Observable<TrendzHealthcheckResult> {
    return this.http.get<TrendzHealthcheckResult>('/api/trendz/healthcheck', defaultHttpOptionsFromConfig(config))
  }

  public getTrendzConfig(config?: RequestConfig): Observable<TrendzConfiguration> {
    return this.http.get<TrendzConfiguration>('/api/trendz/config', defaultHttpOptionsFromConfig(config))
  }

  public saveTrendzConfig(trendzConfig: TrendzConfiguration, config?: RequestConfig): Observable<TrendzConfiguration> {
    return this.http.post<TrendzConfiguration>('/api/trendz/config', trendzConfig, defaultHttpOptionsFromConfig(config))
  }

  public getTrendzSyncResult(config?: RequestConfig): Observable<TrendzSynchronization> {
    return this.http.get<TrendzSynchronization>('/api/trendz/sync', defaultHttpOptionsFromConfig(config))
  }

  public connectToTrendz(config?: RequestConfig): Observable<TrendzSynchronization> {
    return this.http.post<TrendzSynchronization>('/api/trendz/connect', {}, defaultHttpOptionsFromConfig(config))
  }
}
