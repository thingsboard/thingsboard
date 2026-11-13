// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { Extension, ExtensionQuota, ExtensionResourceSizeInfo, ExtensionStatus } from '@shared/models/extension.models';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';

@Injectable({
  providedIn: 'root'
})
export class ExtensionService {

  constructor(
    private http: HttpClient
  ) {}

  public getExtensions(pageLink: PageLink, config?: RequestConfig): Observable<PageData<Extension>> {
    return this.http.get<PageData<Extension>>(`/api/tenant/extensions${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getExtension(extensionId: string, config?: RequestConfig): Observable<Extension> {
    return this.http.get<Extension>(`/api/extension/${extensionId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveExtension(extension: Extension, config?: RequestConfig): Observable<Extension> {
    return this.http.post<Extension>('/api/extension', extension, defaultHttpOptionsFromConfig(config));
  }

  public deleteExtension(extensionId: string, config?: RequestConfig) {
    return this.http.delete(`/api/extension/${extensionId}`, defaultHttpOptionsFromConfig(config));
  }

  public getExtensionStatus(extensionId: string, config?: RequestConfig): Observable<ExtensionStatus> {
    return this.http.get<ExtensionStatus>(`/api/extension/${extensionId}/status`, defaultHttpOptionsFromConfig(config));
  }

  public restartExtension(extensionId: string, config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/extension/${extensionId}/restart`, null, defaultHttpOptionsFromConfig(config));
  }

  public stopExtension(extensionId: string, config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/extension/${extensionId}/stop`, null, defaultHttpOptionsFromConfig(config));
  }

  public startExtension(extensionId: string, config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/extension/${extensionId}/start`, null, defaultHttpOptionsFromConfig(config));
  }

  public getExtensionLogs(extensionId: string, lines = 200, config?: RequestConfig): Observable<string> {
    return this.http.get(`/api/extension/${extensionId}/logs?lines=${lines}`,
      {...defaultHttpOptionsFromConfig(config), responseType: 'text'});
  }

  public getExtensionApiDocs(extensionId: string, config?: RequestConfig): Observable<object> {
    return this.http.get<object>(`/api/extension/${extensionId}/api-docs`, defaultHttpOptionsFromConfig(config));
  }

  public getExtensionQuota(config?: RequestConfig): Observable<ExtensionQuota> {
    return this.http.get<ExtensionQuota>('/api/tenant/extensions/quota', defaultHttpOptionsFromConfig(config));
  }

  public getResourceSizes(config?: RequestConfig): Observable<Array<ExtensionResourceSizeInfo>> {
    return this.http.get<Array<ExtensionResourceSizeInfo>>('/api/extension/resourceSizes',
      defaultHttpOptionsFromConfig(config));
  }

}
