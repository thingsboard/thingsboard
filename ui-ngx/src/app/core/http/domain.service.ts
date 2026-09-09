// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { Domain, DomainInfo } from '@shared/models/oauth2.models';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';

@Injectable({
  providedIn: 'root'
})
export class DomainService {

  constructor(
    private http: HttpClient
  ) {
  }

  public saveDomain(domain: Domain, oauth2ClientIds?: Array<string>, config?: RequestConfig): Observable<Domain> {
    let url = '/api/domain';
    if (oauth2ClientIds?.length) {
      url += `?oauth2ClientIds=${oauth2ClientIds.join(',')}`;
    }
    return this.http.post<Domain>(url, domain, defaultHttpOptionsFromConfig(config));
  }

  public updateOauth2Clients(id: string, oauth2ClientIds: Array<string>, config?: RequestConfig): Observable<void> {
    return this.http.put<void>(`/api/domain/${id}/oauth2Clients`, oauth2ClientIds, defaultHttpOptionsFromConfig(config));
  }

  public getTenantDomainInfos(pageLink: PageLink, config?: RequestConfig): Observable<PageData<DomainInfo>> {
    return this.http.get<PageData<DomainInfo>>(`/api/domain/infos${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getDomainInfoById(id: string, config?: RequestConfig): Observable<DomainInfo> {
    return this.http.get<DomainInfo>(`/api/domain/info/${id}`, defaultHttpOptionsFromConfig(config));
  }

  public deleteDomain(id: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/domain/${id}`, defaultHttpOptionsFromConfig(config));
  }

}
