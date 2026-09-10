// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { ApiKeyInfo, ApiKey } from '@shared/models/api-key.models';

@Injectable({
  providedIn: 'root'
})
export class ApiKeyService {

  constructor(
    private http: HttpClient
  ) {
  }

  public saveApiKey(apiKey: ApiKeyInfo, config?: RequestConfig): Observable<ApiKey> {
    return this.http.post<ApiKey>('/api/apiKey', apiKey, defaultHttpOptionsFromConfig(config));
  }

  public deleteApiKey(id: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/apiKey/${id}`, defaultHttpOptionsFromConfig(config));
  }

  public updateApiKeyDescription(id: string, description: string, config?: RequestConfig): Observable<ApiKeyInfo> {
    return this.http.put<ApiKeyInfo>(`/api/apiKey/${id}/description`, description, defaultHttpOptionsFromConfig(config));
  }

  public enableApiKey(id: string, enabledValue: boolean, config?: RequestConfig): Observable<ApiKeyInfo> {
    return this.http.put<ApiKeyInfo>(`/api/apiKey/${id}/enabled/${enabledValue}`, defaultHttpOptionsFromConfig(config));
  }

  public getUserApiKeys(userId: string, pageLink: PageLink, config?: RequestConfig): Observable<PageData<ApiKeyInfo>> {
    return this.http.get<PageData<ApiKeyInfo>>(`/api/apiKeys/${userId}${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }
}
