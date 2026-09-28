// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { SecretStorage, SecretStorageInfo } from '@shared/models/secret-storage.models';

@Injectable({
  providedIn: 'root'
})
export class SecretStorageService {

  constructor(
    private http: HttpClient
  ) { }

  public getSecrets(pageLink: PageLink, config?: RequestConfig): Observable<PageData<SecretStorage>> {
    return this.http.get<PageData<SecretStorage>>(`/api/secrets${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public saveSecret(secret: SecretStorageInfo, config?: RequestConfig): Observable<SecretStorage> {
    return this.http.post<SecretStorageInfo>('/api/secret', secret, defaultHttpOptionsFromConfig(config));
  }

  public deleteSecret(secretId: string, config?: RequestConfig) {
    return this.http.delete(`/api/secret/${secretId}`, defaultHttpOptionsFromConfig(config));
  }

  public updateSecretDescription(secretId: string, description: string, config?: RequestConfig): Observable<void> {
    return this.http.put<void>(`/api/secret/${secretId}/description`, description,
      defaultHttpOptionsFromConfig(config));
  }

  public updateSecretValue(secretId: string, value: string, config?: RequestConfig): Observable<void> {
    return this.http.put<void>(`/api/secret/${secretId}/value`, value,
      defaultHttpOptionsFromConfig(config));
  }

  public getSecretByName(name: string, config?: RequestConfig): Observable<SecretStorage> {
    const encodedName = encodeURIComponent(name);
    return this.http.get<SecretStorage>(`/api/secret?name=${encodedName}`, defaultHttpOptionsFromConfig(config));
  }
}
