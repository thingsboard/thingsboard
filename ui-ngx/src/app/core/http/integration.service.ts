// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable, shareReplay, timer } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { map } from 'rxjs/operators';
import { sortEntitiesByIds } from '@shared/models/base-data';
import {
  Integration,
  IntegrationInfo,
  IntegrationsConvertersInfo,
  IntegrationType
} from '@shared/models/integration.models';

@Injectable({
  providedIn: 'root'
})
export class IntegrationService {

  private integrationsConvertersInfo$!: Observable<IntegrationsConvertersInfo>

  constructor(
    private http: HttpClient
  ) { }

  public getIntegrations(pageLink: PageLink, config?: RequestConfig): Observable<PageData<Integration>> {
    return this.getIntegrationsByEdgeTemplate(pageLink, false, config);
  }

  public getIntegrationsConvertersInfo(config?: RequestConfig): Observable<IntegrationsConvertersInfo> {
    return this.http.get<IntegrationsConvertersInfo>(`/api/integrations/converters/info`, defaultHttpOptionsFromConfig(config));
  }

  public getIntegrationsConvertersInfoCached(config?: RequestConfig): Observable<IntegrationsConvertersInfo> {
    if (!this.integrationsConvertersInfo$) {
      this.integrationsConvertersInfo$ = this.http.get<IntegrationsConvertersInfo>(`/api/integrations/converters/info`, defaultHttpOptionsFromConfig(config)).pipe(
        shareReplay({ bufferSize: 1, refCount: true })
      );
      timer(3600000).subscribe(() => this.integrationsConvertersInfo$ = null);
    }
    return this.integrationsConvertersInfo$;
  }

  public getIntegrationsInfo(pageLink: PageLink, isEdgeTemplate: boolean,
                             config?: RequestConfig): Observable<PageData<IntegrationInfo>> {
    return this.http.get<PageData<IntegrationInfo>>(`/api/integrationInfos${pageLink.toQuery()}&isEdgeTemplate=${isEdgeTemplate}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getIntegrationsByEdgeTemplate(pageLink: PageLink, isEdgeTemplate: boolean,
                                       config?: RequestConfig): Observable<PageData<Integration>> {
    return this.http.get<PageData<Integration>>(`/api/integrations${pageLink.toQuery()}&isEdgeTemplate=${isEdgeTemplate}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getIntegrationsByIds(integrationIds: Array<string>, config?: RequestConfig): Observable<Array<Integration>> {
    return this.http.get<Array<Integration>>(`/api/integrations?integrationIds=${integrationIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map((integrations) => sortEntitiesByIds(integrations, integrationIds))
    );
  }

  public getIntegration(integrationId: string, config?: RequestConfig): Observable<Integration> {
    return this.http.get<Integration>(`/api/integration/${integrationId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveIntegration(integration: Integration, config?: RequestConfig): Observable<Integration> {
    return this.http.post<Integration>('/api/integration', integration, defaultHttpOptionsFromConfig(config));
  }

  public deleteIntegration(integrationId: string, config?: RequestConfig) {
    return this.http.delete(`/api/integration/${integrationId}`, defaultHttpOptionsFromConfig(config));
  }

  public getIntegrationHttpEndpointLink(configuration: any, integrationType: IntegrationType, routingKey: string): string {
    let url: string = configuration.baseUrl;
    const type = integrationType ? integrationType.toLowerCase() : '';
    const key = routingKey ? routingKey : '';
    url += `/api/v1/integrations/${type}/${key}`;
    return url;
  }

  public checkIntegrationConnection(value: Integration, config?: RequestConfig): Observable<string>{
    return this.http.post<string>('/api/integration/check', value, defaultHttpOptionsFromConfig(config));
  }

  public assignIntegrationToEdge(edgeId: string, integrationId: string, config?: RequestConfig): Observable<Integration> {
    return this.http.post<Integration>(`/api/edge/${edgeId}/integration/${integrationId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public unassignIntegrationFromEdge(edgeId: string, integrationId: string, config?: RequestConfig) {
    return this.http.delete(`/api/edge/${edgeId}/integration/${integrationId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEdgeIntegrations(edgeId: string, pageLink: PageLink, config?: RequestConfig): Observable<PageData<IntegrationInfo>> {
    return this.http.get<PageData<IntegrationInfo>>(`/api/edge/${edgeId}/integrations${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public exportIntegrationPackage(integrationId: string, config?: RequestConfig): Observable<Blob> {
    return this.http.get(`/api/integration/${integrationId}/export-package`,
      { ...defaultHttpOptionsFromConfig(config), responseType: 'blob' });
  }
}
